package cy.ac.ucy.cs.anyplace.lib.android.ui.settings.smas

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs.ConfirmActionDialog
import cy.ac.ucy.cs.anyplace.lib.android.ui.settings.base.BaseSettingsActivity
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.android.cache.smas.ChatCache
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.store.SmasDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.di.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlImg.TG
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasChatViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasMainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsChatActivity: BaseSettingsActivity() {
  companion object {
   private val TG = "act-settings-chat"
  }

  private lateinit var settingsFragment: SettingsChatFragment
  private lateinit var VM: SmasMainViewModel
  private lateinit var VMchat: SmasChatViewModel
  @Inject lateinit var repo: RepoSmas
  @Inject lateinit var RFH: RetrofitHolderSmas


  private val cacheChat by lazy { ChatCache(applicationContext) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    VM = ViewModelProvider(this)[SmasMainViewModel::class.java]
    VMchat = ViewModelProvider(this)[SmasChatViewModel::class.java]

    settingsFragment = SettingsChatFragment(VM, VMchat, RFH, this.app.dsSmas, cacheChat, repo)
    setupFragment(settingsFragment, savedInstanceState)

    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.setTextColor(Color.WHITE)
    supportActionBar?.setBackButton(applicationContext, Color.WHITE)

  }

  class SettingsChatFragment(
    private val VM: SmasMainViewModel,
    private val VMchat: SmasChatViewModel,
    private val RFH: RetrofitHolderSmas,
    private val dsChat: SmasDataStore,
    private val cacheChat: ChatCache,
    private val repo: RepoSmas) : PreferenceFragmentCompat() {

    private val TG = "${SettingsChatActivity.TG}-frgmnt"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      preferenceManager.preferenceDataStore = dsChat
      setPreferencesFromResource(R.xml.preferences_chat, rootKey)

      setupVersionButton()
      setupClearMessagesButton()
    }


    private fun setupClearMessagesButton() {
      val title= "Clearing all messages?"
      val subtitle = "To fetch those again, you must close and reopen the app (intentional).\n\n"+
      "Given internet connectivity, messages will be fetched again."

      val mgr = requireActivity().supportFragmentManager
      val prefBtn : Preference? = findPreference(getString(R.string.pref_smas_delete_local_msgs))

      prefBtn?.setOnPreferenceClickListener {
        lifecycleScope.launch(Dispatchers.IO) {
          if (!cacheChat.hasImgCache() && !repo.local.hasMsgs()) {
            app.showToast(lifecycleScope, "No messages found")
          } else {
            ConfirmActionDialog.SHOW(mgr, title, subtitle, cancellable = true, isImportant = true) { // on confirmed
              clearMessages()
            }
          }
        }
        true
      }
    }

    /**
     * Deletes the message cache.
     *
     * TODO: there can be contention if we have background ongoing tasks for fetching messages.
     */
    private fun clearMessages() {
      val MT = ::clearMessages.name
      LOG.W(TG, "$MT: from DB and SmasCache (images)")
      lifecycleScope.launch(Dispatchers.IO) {  // artificial delay

        // from now on don't pull any new messages (wait for any ongoing pulls)
        appSmas.stopMsgGetBLOCKING()

        LOG.D2(TG, "$MT: dropping db, clearing img cache, & LazyColumn msgList")
        repo.local.dropMsgs()
        cacheChat.clearImgCache()
        appSmas.msgList.clear()

        appSmas.showToast(lifecycleScope, "Chat cache cleared.\nReopen app to fetch again.", Toast.LENGTH_LONG)
        // appSmas.resumeMsgGet() // TODO need this?
      }
    }

    private fun setupVersionButton() {
      val prefBtn: Preference? = findPreference(getString(R.string.pref_smas_server_version))
      prefBtn?.setOnPreferenceClickListener {
        prefBtn.icon = null
        prefBtn.summary = "refreshing.."
        lifecycleScope.launch {  // artificial delay
          delay(250)
          VM.displayVersion(prefBtn)
        }
        true // click is handled
      }
      observeChatPrefs(prefBtn)
    }

    /**
     * When Chat Preferences change:
     * - update Retrofit Holder (wrapper to work well w/ DI)
     * - re-initiate contact with the Chat Server
     */
    private fun observeChatPrefs(versionPreferences: Preference?) {
      VM.prefsChat.asLiveData().observe(this) { prefs ->
        RFH.set(prefs)
        LOG.D3(TG, "Chat Base URL: ${RFH.retrofit.baseUrl()}")
        VM.displayVersion(versionPreferences)
      }
    }
  }
}