package cy.ac.ucy.cs.anyplace.lib.android

import android.app.Activity
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.preference.PreferenceFragmentCompat
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.smas.models.ChatMsg
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.store.ChatPrefsDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.store.ChatUserDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.source.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasChatViewModel
// import cy.ac.ucy.cs.anyplace.smas.viewmodel.SmasChatViewModel
// import androidx.compose.runtime.mutableStateListOf
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasMainViewModel
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject


// EXTENSIONS
val Activity.appSmas: SmasApp get() = this.application as SmasApp
val PreferenceFragmentCompat.appSmas: SmasApp get() = this.requireActivity().appSmas
val Activity.chatPrefsDS: ChatPrefsDataStore get() = this.appSmas.dsChat
val AndroidViewModel.appSmas: SmasApp get() = getApplication<SmasApp>()

@HiltAndroidApp
class SmasApp : AnyplaceApp() {

  /** Force skipping msg pull. Needed to control asynchronicity,
   * e.g., when deleting the messages
   */

  /** SMAS Chat Server preferences */
  @Inject lateinit var dsChat: ChatPrefsDataStore
  /** Logged-in SMAS user */
  @Inject lateinit var dsChatUser: ChatUserDataStore
  @Inject lateinit var rfhSmas: RetrofitHolderSmas

  /** list of messages shown on screen by [LazyColumn] */
  var msgList = mutableStateListOf<ChatMsg>()

  /** The VM set by [SmasMainActivity] */
  private var VM: SmasMainViewModel?= null
  /** The VMchat set by [SmasMainActivity] */
  private var VMchat : SmasChatViewModel? = null


  override fun onCreate() {
    super.onCreate()
    LOG.D2()

    observeChatPrefs()
  }

  /**
   * Set from the [SmasMainActivity],
   * to allow triggering the [SmasMainViewModel] from the [SmasChatActivity]
   */
  fun setMainActivityVMs(VM: SmasMainViewModel, VMchat: SmasChatViewModel) {
    this.VM=VM
    this.VMchat=VMchat
  }

  /**
   * Stops the receival of messages and waits if there is an ongoing receival
   */
  fun stopMsgGetBLOCKING() {
    VMchat?.nwMsgGet?.skipCall=true
    while (VMchat?.nwMsgGet?.resp?.value is NetworkResult.Loading) {
      LOG.D2(TAG, "$METHOD: waiting for last MsgGet to end..")
    }
  }

  fun resumeMsgGet() {
    VMchat?.nwMsgGet?.skipCall = false
  }

  fun pullMessagesONCE() {
    LOG.V2(TAG, "$METHOD: using VMchat of Main Activity?")
    VMchat?.netPullMessagesONCE(false)
  }

  /** Manually create a new instance of the RetrofitHolder on pref changes */
  private fun observeChatPrefs() {
    val prefsChat = dsChat.read
    prefsChat.asLiveData().observeForever { prefs ->
      rfhSmas.set(prefs)
      LOG.V3(TAG, "Updated Chat backend URL: ${rfhSmas.baseURL}")
    }
  }
}