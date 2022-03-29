package cy.ac.ucy.cs.anyplace.lib.android.ui.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.store.CvDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.models.helpers.FloorHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.models.helpers.FloorsHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.models.helpers.SpaceHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.store.CvNavDataStore
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs.ClearCachesDialog
import cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs.ModelPickerDialog
import cy.ac.ucy.cs.anyplace.lib.models.Space
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Accepts a serialized [Space] as an argument (using [Bundle]).
 * Has similarities with [SettingsCvLoggerFragment]
 *
 * TODO Settings:
 * - clear section
 * - clear all cache
 * - clear floorplans
 * - clear cvmaps
 *
 * TODO: Datastore Settings:
 *
 * NOTE: this should have been in the SMAS source code
 */
@AndroidEntryPoint
class SettingsNavigationActivity: AnyplaceSettingsActivity() {
  companion object {
    const val ARG_SPACE = "pref_act_space"
    const val ARG_FLOORS = "pref_act_floors"
    const val ARG_FLOOR = "pref_act_floor"
  }

  private lateinit var settingsFragment: SettingsCvNavigationFragment

  override fun onBackPressed() {
    super.onBackPressed()
    LOG.D4(TAG, "SettingsNav on backPressed")
  }

  override fun onResume() {
    super.onResume()
    LOG.D4(TAG, "SettingsNav on resume")
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    settingsFragment = SettingsCvNavigationFragment(cvNavDS, cvDataStoreDS, repo)
    setupFragment(settingsFragment, savedInstanceState)

    // TODO: icon not shown
    // preferenceScreen.icon = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_settings)
  }

  class SettingsCvNavigationFragment(
          private val dsCvNav: CvNavDataStore,
          private val dsCv: CvDataStore,
          private val repo: RepoAP,
  ) : PreferenceFragmentCompat() {

    override fun onResume() {
      super.onResume()
      LOG.D4(TAG, "SettingsNav: FRAGMENT on resume")
    }

    var spaceH : SpaceHelper? = null
    var floorsH: FloorsHelper? = null
    var floorH: FloorHelper? = null

    @SuppressLint("ResourceAsColor")
    override fun onCreatePreferences(args: Bundle?, rootKey: String?) {
      setPreferencesFromResource(R.xml.preferences_cv_navigation, rootKey)

      preferenceManager.preferenceDataStore = dsCvNav

      val extras = requireActivity().intent.extras
      spaceH = IntentExtras.getSpace(requireActivity(), repo, extras, ARG_SPACE)
      floorsH = IntentExtras.getFloors(spaceH, extras, ARG_FLOORS)
      floorH = IntentExtras.getFloor(spaceH, extras, ARG_FLOOR)

      // bind DataStore values to the preference XML
      lifecycleScope.launch {
        dsCvNav.read.first { prefs ->
          setPercentageInput(R.string.pref_cvnav_map_alpha,
                  R.string.summary_map_alpha, prefs.mapAlpha,
                  "Map is fully opaque", "Map is fully transparent")

          setNumericInput(R.string.pref_smas_location_refresh,
                  R.string.summary_refresh_locations, prefs.locationRefresh)

          setNumericInput(R.string.pref_cv_window_localization_seconds,
                  R.string.summary_localization_window, prefs.windowLocalizationSeconds)

          setBooleanInput(R.string.pref_cv_dev_mode, prefs.devMode)

          true
        }

        setupButtonClearCache(spaceH, floorsH, floorH)

        val modelName = dsCv.read.first().modelName
        setupButtonChangeModel(modelName)
        setupButtonServerSettings()
      }
    }

    private fun setupButtonServerSettings() {
      val pref = findPreference<Preference>(getString(R.string.pref_anyplace_server))
      pref?.setOnPreferenceClickListener {
        LOG.D(TAG_METHOD)
        startActivity(Intent(requireActivity(), SettingsServerActivity::class.java))
        true
      }
    }


    private fun setupButtonClearCache(
            spaceH: SpaceHelper?,
            floorsH: FloorsHelper?,
            floorH: FloorHelper?) {
      val pref = findPreference<Preference>(getString(R.string.pref_log_clear_cache))
      pref?.setOnPreferenceClickListener {
        LOG.D(TAG_METHOD)
        ClearCachesDialog.SHOW(requireActivity().supportFragmentManager,
                repo, dsCv, spaceH, floorsH, floorH)
        true
      }
    }

    private fun setupButtonChangeModel(modelName: String) {
      val pref = findPreference<Preference>(getString(R.string.pref_cv_model))
      pref?.setOnPreferenceClickListener {
        // TODO:PM Change CV Model
        true
      }
    }
  }  // PreferenceFragmentCompat
}
