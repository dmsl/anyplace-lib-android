package cy.ac.ucy.cs.anyplace.lib.android.ui.settings

import android.os.Bundle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.store.ServerDataStore
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.dsServer
import cy.ac.ucy.cs.anyplace.lib.android.utils.network.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsServerActivity: AnyplaceSettingsActivity() {
  private lateinit var settingsFragment: SettingsServerFragment

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    settingsFragment = SettingsServerFragment(mainViewModel, retrofitHolderAP, dsServer)
    setupFragment(settingsFragment, savedInstanceState)
  }

  class SettingsServerFragment(
          private val mainViewModel: MainViewModel,
          private val retrofitHolderAP: RetrofitHolderAP,
          private val serverDataStore: ServerDataStore) : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      preferenceManager.preferenceDataStore = serverDataStore
      setPreferencesFromResource(R.xml.preferences_server, rootKey)

      val versionPreferences : Preference? = findPreference("pref_server_version")
      versionPreferences?.setOnPreferenceClickListener {
        versionPreferences.icon = null
        versionPreferences.summary = "refreshing.."
        lifecycleScope.launch {  // artificial delay
          delay(250)
          mainViewModel.displayBackendVersion(versionPreferences)
        }
        true // click is handled
      }
      reactToSettingsUpdates(versionPreferences)
    }

    private fun reactToSettingsUpdates(versionPreferences: Preference?) {
      mainViewModel.prefsServer.asLiveData().observe(this) { prefs ->
        retrofitHolderAP.set(prefs)
        LOG.D3(TAG, "reactToSettingsUpdates: ${retrofitHolderAP.retrofit.baseUrl()}")
        mainViewModel.displayBackendVersion(versionPreferences)
      }
    }
  }
}