package cy.ac.ucy.cs.anyplace.lib.android.ui.settings

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.store.CvDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.store.CvLoggerDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.models.helpers.FloorHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.models.helpers.FloorsHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.models.helpers.SpaceHelper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs.ClearCachesDialog
import cy.ac.ucy.cs.anyplace.lib.models.Space
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Accepts a serialized [Space] as an argument (using [Bundle]).
 *
 * TODO Settings:
 * - clear section
 * - clear all cache
 * - clear floorplans
 * - clear cvmaps
 *
 * TODO: Datastore Settings:
 * -
 */
@AndroidEntryPoint
class SettingsCvLoggerActivity: AnyplaceSettingsActivity() {

  companion object {
    const val ARG_SPACE = "pref_act_space"
    const val ARG_FLOORS = "pref_act_floors"
    const val ARG_FLOOR = "pref_act_floor"
  }

  private lateinit var settingsFragment: SettingsCvLoggerFragment

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    settingsFragment = SettingsCvLoggerFragment(cvLogDS, cvDataStoreDS, repo)
    setupFragment(settingsFragment, savedInstanceState)

    // TODO FIXME:PM not shown!
    // preferenceScreen.icon = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_settings)
  }

  class SettingsCvLoggerFragment(
          private val cvLoggerDataStore: CvLoggerDataStore,
          private val cvDataStore: CvDataStore,
          private val repo: RepoAP,
  ) : PreferenceFragmentCompat() {

    var spaceH : SpaceHelper? = null
    var floorsH: FloorsHelper? = null
    var floorH: FloorHelper? = null

    @SuppressLint("ResourceAsColor")
    override fun onCreatePreferences(args: Bundle?, rootKey: String?) {
      setPreferencesFromResource(R.xml.preferences_cv_logger, rootKey)

      preferenceManager.preferenceDataStore = cvLoggerDataStore // TODO:PM implement ths

      val extras = requireActivity().intent.extras
      spaceH = IntentExtras.getSpace(requireActivity(), repo, extras, ARG_SPACE)
      floorsH = IntentExtras.getFloors(spaceH, extras, ARG_FLOORS)
      floorH = IntentExtras.getFloor(spaceH, extras, ARG_FLOOR)

      // bind DataStore values to the preference XML
      lifecycleScope.launch {
        cvLoggerDataStore.read.first { prefs ->
          setNumericInput(R.string.pref_cvlog_window_logging_seconds,
                  R.string.summary_logging_window, prefs.windowLoggingSeconds)
          setNumericInput(R.string.pref_cv_window_localization_seconds,
                  R.string.summary_localization_window, prefs.windowLocalizationSeconds)

          setBooleanInput(R.string.pref_cv_dev_mode, prefs.devMode)

          true
        }
      }

      setupButtonClearCache(spaceH, floorsH, floorH)
      setupButtonChangeModel()
    }

    private fun setupButtonClearCache(
            spaceH: SpaceHelper?,
            floorsH: FloorsHelper?,
            floorH: FloorHelper?) {
      val pref = findPreference<Preference>(getString(R.string.pref_log_clear_cache))
      pref?.setOnPreferenceClickListener {
        LOG.W(TAG_METHOD, "TODO clear cache")
        ClearCachesDialog.SHOW(requireActivity().supportFragmentManager,
                repo, cvDataStore, spaceH, floorsH, floorH)
        true
      }
    }

    private fun setupButtonChangeModel() {
      val pref = findPreference<Preference>(getString(R.string.pref_cv_model))
      pref?.setOnPreferenceClickListener {
        LOG.W(TAG_METHOD, "clear cache")
        // ModelPickerDialog.SHOW(requireActivity().supportFragmentManager, dataStoreCv)
        true
      }
    }
  }  // PreferenceFragmentCompat
}
