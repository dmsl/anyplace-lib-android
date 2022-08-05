package cy.ac.ucy.cs.anyplace.lib.android.ui.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.cache.anyplace.Cache
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.DetectionModel
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.CvDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.wrappers.LevelWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.wrappers.LevelsWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.wrappers.SpaceWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.CvMapDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs.ConfirmActionDialog
import cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs.ModelPickerDialog
import cy.ac.ucy.cs.anyplace.lib.android.ui.settings.base.SettingsActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.settings.base.IntentExtras
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.AnyplaceViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasMainViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Space
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Basic CV Settings of the application
 * - shown in both SMAS/Navigator, and Logger
 *   - logger makes visible an extra preference-category
 *
 * Accepts a serialized [Space] as an argument (using [Bundle]).
 * Has similarities with [SettingsCvLoggerFragment]
 */
@AndroidEntryPoint
class SettingsCvActivity: SettingsActivity() {
  companion object {
    private const val TG = "act-settings-cv"
    const val ARG_SPACE = "pref_act_space"
    const val ARG_FLOORS = "pref_act_floors"
    const val ARG_FLOOR = "pref_act_floor"
  }

  private lateinit var settingsFragment: SettingsCvFragment
  private lateinit var VM: CvViewModel

  override fun onBackPressed() {
    val MT = ::onBackPressed.name
    super.onBackPressed()
    LOG.D4(TG, MT)
  }

  override fun onResume() {
    val MT = ::onResume.name
    super.onResume()
    LOG.D4(TG, MT)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    VM = ViewModelProvider(this)[SmasMainViewModel::class.java]

    settingsFragment = SettingsCvFragment(app, VM, VMap, dsCvMap, dsCv, repoAP, repoSmas)
    setupFragment(settingsFragment, savedInstanceState)
  }

  class SettingsCvFragment(
          private val app: AnyplaceApp,
          private val VM: CvViewModel,
          private val VMap: AnyplaceViewModel,
          /** Only this is binded to the activity */
          private val ds: CvMapDataStore,
          /** NOT binded to this activity. Only used to independently change (Dialog UI) the Model*/
          private val dsCv: CvDataStore,
          private val repoAP: RepoAP,
          private val repoSmas: RepoSmas,
  ) : PreferenceFragmentCompat() {  // PreferenceFragmentCompat
    private val TG = "${SettingsCvActivity.TG}-frgmt"

    override fun onResume() {
      val MT = ::onResume.name
      super.onResume()
      LOG.D4(TG, MT)
    }

    @Deprecated("")
    var spaceH : SpaceWrapper? = null
    @Deprecated("")
    var levelsH: LevelsWrapper? = null
    @Deprecated("")
    var levelH: LevelWrapper? = null

    val cache by lazy { Cache(requireActivity()) }

    /**
     * Preferences must be bound to the [ds] ([CvMapDataStore].
     * Otherwise the [R.xml.preferences_cv] will not be initialized properly.
     * Some values need to be updated as they change (e.g., setPercentageInput, numericInput, etc..)
     * while some others dont (setListPreferenceInput).
     *
     * Some others are handled as buttons that do other actions,
     * e.g., setupChangeCvModel, or deleting some caches..
     */
    @SuppressLint("ResourceAsColor")
    override fun onCreatePreferences(args: Bundle?, rootKey: String?) {
      val MT = ::onCreatePreferences.name
      setPreferencesFromResource(R.xml.preferences_cv, rootKey)

      // a datastore must be binded to preferenceDataStore
      preferenceManager.preferenceDataStore = ds

      // these are not used.. (see [IntentExtras])
      // used to accept the selected space. but this is not used anymore.. [spaceH], [levelsH], and [levelH]
      val extras = requireActivity().intent.extras
      spaceH = IntentExtras.getSpace(requireActivity(), repoAP, extras, ARG_SPACE)
      levelsH = IntentExtras.getFloors(spaceH, extras, ARG_FLOORS)
      levelH = IntentExtras.getFloor(spaceH, extras, ARG_FLOOR)

      // BINDING DataStore VALUES TO THE PREFERENCE XML
      lifecycleScope.launch {
        val prefs = ds.read.first()

        showLoggerPreferences()

        setPercentageInput(R.string.prev_cvmap_alpha,
                R.string.summary_map_alpha, prefs.mapAlpha,
                "Map is fully opaque", "Map is fully transparent")

        setNumericInput(R.string.pref_cv_scan_delay,
                R.string.summary_cv_scan_delay, prefs.scanDelay, 0)

        setNumericInput(R.string.pref_cv_localization_ms,
                R.string.summary_localization_window, prefs.windowLocalizationMs, 0)

        setNumericInput(R.string.pref_cv_logging_ms,
                R.string.summary_logging_window, prefs.windowLoggingMs, 1000)

        setNumericInput(R.string.pref_smas_location_refresh,
                R.string.summary_refresh_locations, prefs.locationRefreshMs, 500)


        if (!DBG.TRK) {
          val preference = findPreference(getString(R.string.pref_cv_tracking_delay)) as EditTextPreference?
          preference?.isVisible=false
        }
        setNumericInput(R.string.pref_cv_tracking_delay,
                R.string.summary_tracking_delay, prefs.cvTrackingDelay, 1000)

        if (!DBG.TRK) {
          val preference = findPreference(getString(R.string.pref_cv_tracking_auto_disable)) as EditTextPreference?
          preference?.isVisible=false
        }
        setNumericInput(R.string.pref_cv_tracking_auto_disable,
                R.string.summary_tracking_auto_disable, prefs.cvTrackingAutoDisable, 5)

        setBooleanInput(R.string.pref_cv_dev_mode, prefs.devMode)
        setBooleanInput(R.string.pref_cv_autoset_initial_location, prefs.autoSetInitialLocation)
        setBooleanInput(R.string.pref_cv_follow_selected_user, prefs.followSelectedUser)
        setBooleanInput(R.string.pref_cv_fingerprints_auto_update, prefs.autoUpdateCvFingerprints)

        setListPreferenceInput(R.string.pref_cv_loc_algo_choice, prefs.cvAlgoChoice)
        setListPreferenceInput(R.string.pref_cv_loc_algo_execution, prefs.cvAlgoExec)

        setupChangeCvModel()
        setupButtonServerSettings()
        setupUiClearCvModelsDB(app, VM)

        setupDownloadCvMap(app, VM)
        clearAvailableSpaces(app, VM, VMap)
      }
    }

    private suspend fun showLoggerPreferences() {
      val MT = ::showLoggerPreferences.name
      LOG.W(TG, MT)
      if (app.isLogger()) {
        LOG.E(TG, "$MT: showing")
        val pref = findPreference<PreferenceCategory>(getString(R.string.prefcat_cv_logging))
        pref?.isVisible=true
      }
    }

    private fun setupButtonServerSettings() {
      val MT = ::setupButtonServerSettings.name
      val pref = findPreference<Preference>(getString(R.string.pref_anyplace_server))
      pref?.setOnPreferenceClickListener {
        LOG.D(TG, MT)
        startActivity(Intent(requireActivity(), SettingsAnyplaceServerActivity::class.java))
        true
      }
    }

    private fun setupChangeCvModel() {
      val MT = ::setupChangeCvModel.name
      val pref = findPreference<Preference>(getString(R.string.pref_cv_model))
      lifecycleScope.launch {
        pref?.setOnPreferenceClickListener {
          LOG.W(TG, "$MT: changing CV model")
          ModelPickerDialog.SHOW(requireActivity().supportFragmentManager, dsCv)
          true
        }
        dsCv.read.collectLatest { prefs ->
          pref?.summary = DetectionModel.getModelAndDescription(prefs.modelName)
        }
      }
    }

    private fun setupUiClearCvModelsDB(app: AnyplaceApp, VM: CvViewModel) {
      val MT = ::setupUiClearCvModelsDB.name
      val pref = findPreference<Preference>(getString(R.string.pref_log_clear_cache_cv_models))
      pref?.setOnPreferenceClickListener {
        LOG.W(TG, "$MT: clear cache")
        val mgr = requireActivity().supportFragmentManager
        ConfirmActionDialog.SHOW(mgr, "Refresh CV Models",
                "Will delete the CV Model classes.\n" +
                        "and then download them again.", cancellable = true, isImportant = false) { // on confirmed

          lifecycleScope.launch(Dispatchers.IO) {
            if (!app.hasInternet()) {
              app.showToast(VM.viewModelScope, "Internet connection is required")
              return@launch
            }

            repoSmas.local.dropCvModelClasses()
            app.cvUtils.showNotification=true
            app.cvUtils.clearConvertionTables()
            VM.nwCvModelsGet.safeCall()
          }
        }
        true
      }
    }

    private fun setupDownloadCvMap(app: AnyplaceApp, VM: CvViewModel) {
      val MT = ::setupDownloadCvMap.name
      val pref = findPreference<Preference>(getString(R.string.pref_loc_cv_map))

      pref?.setOnPreferenceClickListener {
        LOG.W(TG, "$MT: setting up")
        val mgr = requireActivity().supportFragmentManager
        ConfirmActionDialog.SHOW(mgr, "Download CvMap",
                "The previous CvMap will be overridden.\nAn application restart is required.",
                cancellable = true, isImportant = false) { // on confirmed

          lifecycleScope.launch(Dispatchers.IO) {
            if (!app.hasInternet()) {
              app.notify.short(VM.viewModelScope, "Internet connection is required")
              return@launch
            }

            VM.nwCvFingerprintsGet.dropFingerprints()
            VM.nwCvFingerprintsGet.blockingCall(app.wSpace.obj.buid, false)
            VM.nwCvFingerprintsGet.collect()
          }
        }
        true
      }
    }

    fun getSelectedSpaceSummary(value: String) : String {
      return value.ifEmpty { "No space selected" }
    }

    private fun clearAvailableSpaces(app: AnyplaceApp, VM: CvViewModel, VMap: AnyplaceViewModel) {
      val MT = ::clearAvailableSpaces.name
      val pref = findPreference<Preference>(getString(R.string.pref_clear_available_spaces))

      if (!DBG.USE_SPACE_SELECTOR) { pref?.isVisible = false; return }

      pref?.setOnPreferenceClickListener {
        LOG.W(TG, "$MT: setting up")
        val mgr = requireActivity().supportFragmentManager
        ConfirmActionDialog.SHOW(mgr, "Clear available spaces",
                "Space Selector will fetch them again from remote,\n"+
                        "the next time you clear the selected space\n"+
                        "Use this if the remote spaces had changes.\n"+
                        "NOTE: a manual app restart might be necessary.",
                cancellable = true, isImportant = true) { // on confirmed

          lifecycleScope.launch(Dispatchers.IO) {
            if (!app.hasInternet()) {
              app.notify.short(VM.viewModelScope, "Internet connection is required")
              return@launch
            }

            repoAP.local.dropSpaces()
            VMap.setBackFromSettings()
          }
        }
        true
      }
    }
  }
}
