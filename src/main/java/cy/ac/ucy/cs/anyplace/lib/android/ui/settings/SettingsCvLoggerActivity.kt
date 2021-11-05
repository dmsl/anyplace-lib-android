package cy.ac.ucy.cs.anyplace.lib.android.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.data.datastore.DataStoreCvLogger
import cy.ac.ucy.cs.anyplace.lib.android.extensions.dataStoreCvLogger
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsCvLoggerActivity: BaseSettingsActivity() {
  private lateinit var settingsFragment: SettingsCvLoggerFragment

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    settingsFragment = SettingsCvLoggerFragment(dataStoreCvLogger)
    setupFragment(settingsFragment, savedInstanceState)
  }

  class SettingsCvLoggerFragment(
    private val dataStoreCvLogger: DataStoreCvLogger
  ) : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      preferenceManager.preferenceDataStore = dataStoreCvLogger
      setPreferencesFromResource(R.xml.preferences_logger_cv, rootKey)
    }
  }
}