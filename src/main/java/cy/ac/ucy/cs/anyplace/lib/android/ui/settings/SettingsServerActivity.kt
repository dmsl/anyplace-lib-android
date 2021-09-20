package cy.ac.ucy.cs.anyplace.lib.android.ui.settings

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.datastore.DataStoreServer
import cy.ac.ucy.cs.anyplace.lib.android.dataStoreServer
import cy.ac.ucy.cs.anyplace.lib.android.ui.selector.space.BaseActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.network.RetrofitHolder
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsBaseActivity: BaseActivity() {
  private val TAG = SettingsBaseActivity::class.java.simpleName
  private lateinit var mainViewModel: MainViewModel
  private lateinit var settingsFragment: SettingsFragment

  @Inject
  lateinit var retrofitHolder: RetrofitHolder

  override fun onResume() {
    super.onResume()
    mainViewModel.setBackFromSettings()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.settings_activity)

    mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)

    settingsFragment = SettingsFragment(mainViewModel, retrofitHolder, dataStoreServer)

    // applicationContext.dataStoreServer
    if (savedInstanceState == null) {
      supportFragmentManager
          .beginTransaction()
          .replace(R.id.settings, settingsFragment)
          .commit()
    }

    supportActionBar?.setDisplayHomeAsUpEnabled(true)

  }

  class SettingsFragment(
    private val mainViewModel: MainViewModel,
    private val retrofitHolder: RetrofitHolder,
    private val dataStoreServer: DataStoreServer) : PreferenceFragmentCompat() {
    private val TAG  = SettingsBaseActivity::class.java.simpleName+"."+SettingsFragment::class.java.simpleName

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      preferenceManager.preferenceDataStore = dataStoreServer
      setPreferencesFromResource(R.xml.base_preferences, rootKey)

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
      mainViewModel.serverPreferences.asLiveData().observe(this, { prefs ->
        retrofitHolder.set(prefs)
        LOG.D3(TAG, "reactToSettingsUpdates: ${retrofitHolder.retrofit.baseUrl()}")
        mainViewModel.displayBackendVersion(versionPreferences)
      })
    }
  }
}