package cy.ac.ucy.cs.anyplace.lib.android.ui.settings

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.DetectionModel
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.CvDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.CvLoggerDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.FloorWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.FloorsWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.SpaceWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs.ModelPickerDialog
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Space
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 *
 *
 * DEPRECATED. Use: [SettingsCvActivity]
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
@Deprecated("Use the SettingsCvActivity")
@AndroidEntryPoint
class SettingsCvLoggerActivityDEPR: AnyplaceSettingsActivity() {

  companion object {
    const val ARG_SPACE = "pref_act_space"
    const val ARG_FLOORS = "pref_act_floors"
    const val ARG_FLOOR = "pref_act_floor"
  }

  private lateinit var settingsFragment: SettingsCvLoggerFragment

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    settingsFragment = SettingsCvLoggerFragment(dsCv, dsCvLog,  repoAP, repoSmas)
    setupFragment(settingsFragment, savedInstanceState)

    // TODO FIXME:PM not shown!
    // preferenceScreen.icon = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_settings)
  }

  @Deprecated("Use the SettingsCvActivity")
  class SettingsCvLoggerFragment(
          private val dsCv: CvDataStore,
          private val dsCvLog: CvLoggerDataStore,
          private val repoAP: RepoAP,
          private val repoSmas: RepoSmas
  ) : PreferenceFragmentCompat() {

    var spaceH : SpaceWrapper? = null
    var floorsH: FloorsWrapper? = null
    var floorH: FloorWrapper? = null

    @SuppressLint("ResourceAsColor")
    override fun onCreatePreferences(args: Bundle?, rootKey: String?) {
      setPreferencesFromResource(R.xml.preferences_cv_logger, rootKey)

      preferenceManager.preferenceDataStore = dsCvLog // TODO:PM implement ths

      val extras = requireActivity().intent.extras
      spaceH = IntentExtras.getSpace(requireActivity(), repoAP, extras, ARG_SPACE)
      floorsH = IntentExtras.getFloors(spaceH, extras, ARG_FLOORS)
      floorH = IntentExtras.getFloor(spaceH, extras, ARG_FLOOR)

      // bind DataStore values to the preference XML
      lifecycleScope.launch {
        dsCvLog.read.first { prefsCv ->
          setNumericInput(R.string.pref_cvlog_window_logging_seconds,
                  R.string.summary_logging_window, prefsCv.windowLoggingSeconds)

          // DEPRECATED. dont use
          // setNumericInput(R.string.pref_cv_localization_ms,
          //         R.string.summary_localization_window, prefsCv.windowLocalizationMs)

          setBooleanInput(R.string.pref_cv_dev_mode, prefsCv.devMode)
          true
        }


      }

      // setupClearCvFingerprintsCache(spaceH, floorsH, floorH)
      setupChangeCvModel()
    }

    @Deprecated("Use the SettingsCvActivity")
    private fun setupChangeCvModel() {
      val pref = findPreference<Preference>(getString(R.string.pref_cv_model))
      lifecycleScope.launch {
        pref?.setOnPreferenceClickListener {
          LOG.W(TAG, "Changing CV model")
          ModelPickerDialog.SHOW(requireActivity().supportFragmentManager, dsCv)
          true
        }
        dsCv.read.collectLatest { prefs ->
          pref?.summary = DetectionModel.getModelAndDescription(prefs.modelName)
        }
      }
    }
  }  // PreferenceFragmentCompat
}
