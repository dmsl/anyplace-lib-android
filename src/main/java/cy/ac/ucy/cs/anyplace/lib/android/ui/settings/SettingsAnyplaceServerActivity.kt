package cy.ac.ucy.cs.anyplace.lib.android.ui.settings

import android.os.Bundle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.ApUserDataStore
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.AnyplaceDataStore
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs.ConfirmActionDialog
import cy.ac.ucy.cs.anyplace.lib.android.ui.settings.base.SettingsActivity
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.di.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlImg.TG
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.AnyplaceViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Settings of the Anyplace Server:
 * - things like url, port, etc..
 */
@AndroidEntryPoint
class SettingsAnyplaceServerActivity: SettingsActivity() {
  private lateinit var settingsFragment: SettingsServerFragment

  companion object {
    private val TG = "act-settings-server-anyplace"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    settingsFragment = SettingsServerFragment(app, VMap, rfhAP, dsAnyplace, dsUserAP)
    setupFragment(settingsFragment, savedInstanceState)
  }

  class SettingsServerFragment(
          private val app: AnyplaceApp,
          private val VMap: AnyplaceViewModel,
          private val RFHap: RetrofitHolderAP,
          private val ds: AnyplaceDataStore,
          private val dsUserAp: ApUserDataStore,

          ) : PreferenceFragmentCompat() {
    private val TG = "${SettingsAnyplaceServerActivity.TG}-frgmt"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      preferenceManager.preferenceDataStore = ds
      setPreferencesFromResource(R.xml.preferences_server, rootKey)

      val versionPreferences : Preference? = findPreference("pref_server_version")
      versionPreferences?.setOnPreferenceClickListener {
        versionPreferences.icon = null
        versionPreferences.summary = "refreshing.."
        lifecycleScope.launch {  // artificial delay
          delay(250)
          VMap.nwVersion.displayVersion(versionPreferences)
        }
        true // click is handled
      }
      reactToSettingsUpdates(versionPreferences)

      setupLogoutAnyplace(app, VMap)
    }

    private fun setupLogoutAnyplace(app: AnyplaceApp, VMap: AnyplaceViewModel) {
      val MT = ::setupLogoutAnyplace.name
      val pref = findPreference<Preference>(getString(R.string.pref_ap_logout))
      pref?.setOnPreferenceClickListener {
        LOG.W(TG,  "$MT: clear cache")
        val mgr = requireActivity().supportFragmentManager
        ConfirmActionDialog.SHOW(mgr, "Logout?",
                "You will have to login again, in order to fetch Spaces",
                cancellable = true, isImportant = false) { // on confirmed

          lifecycleScope.launch(Dispatchers.IO) {
            // if (!app.hasInternet()) {
            //   app.showToast(VMap.viewModelScope, "Internet connection is required")
            //   return@launch
            // }

            dsUserAp.deleteUser()
          }
        }
        true
      }
    }

    private fun reactToSettingsUpdates(versionPreferences: Preference?) {
      val MT = ::reactToSettingsUpdates.name
      VMap.prefsServer.asLiveData().observe(this) { prefs ->
        RFHap.set(prefs)
        LOG.D3(TG, "$MT: ${RFHap.retrofit.baseUrl()}")
        VMap.nwVersion.displayVersion(versionPreferences)
      }
    }

  }
}