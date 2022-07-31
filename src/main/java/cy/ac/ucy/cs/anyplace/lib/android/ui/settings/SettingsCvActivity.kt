package cy.ac.ucy.cs.anyplace.lib.android.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.cache.anyplace.Cache
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.DetectionModel
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.CvDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.FloorWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.FloorsWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.SpaceWrapper
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
 *
 *
 * CvMapDS
 */
@AndroidEntryPoint
class SettingsCvActivity: SettingsActivity() {
  companion object {
    const val ARG_SPACE = "pref_act_space"
    const val ARG_FLOORS = "pref_act_floors"
    const val ARG_FLOOR = "pref_act_floor"
  }

  private lateinit var settingsFragment: SettingsCvFragment
  private lateinit var VM: CvViewModel

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

    override fun onResume() {
      super.onResume()
      LOG.D4(TAG, "SettingsNav: FRAGMENT on resume")
    }

    var spaceH : SpaceWrapper? = null
    var floorsH: FloorsWrapper? = null
    var floorH: FloorWrapper? = null

    val cache by lazy { Cache(requireActivity()) }



    @SuppressLint("ResourceAsColor")
    override fun onCreatePreferences(args: Bundle?, rootKey: String?) {
      setPreferencesFromResource(R.xml.preferences_cv, rootKey)

      preferenceManager.preferenceDataStore = ds

      val extras = requireActivity().intent.extras
      spaceH = IntentExtras.getSpace(requireActivity(), repoAP, extras, ARG_SPACE)
      floorsH = IntentExtras.getFloors(spaceH, extras, ARG_FLOORS)
      floorH = IntentExtras.getFloor(spaceH, extras, ARG_FLOOR)

      // bind DataStore values to the preference XML
      lifecycleScope.launch {
        LOG.D(TAG, "SNAct: calls read")
        val prefs = ds.read.first()
        LOG.E(TAG, "SCAN: SNAct: scanDelay: ${prefs.scanDelay}")

        setPercentageInput(R.string.prev_cvmap_alpha,
                R.string.summary_map_alpha, prefs.mapAlpha,
                "Map is fully opaque", "Map is fully transparent")

        setNumericInput(R.string.pref_cv_scan_delay,
                R.string.summary_cv_scan_delay, prefs.scanDelay, 0)

        setNumericInput(R.string.pref_cv_localization_ms,
                R.string.summary_localization_window, prefs.windowLocalizationMs, 0)

        if (DBG.LMT) {
          setNumericInput(R.string.pref_cv_logging_ms,
                  R.string.summary_logging_window, prefs.windowLoggingMs, 1000)
        } else {
          setNumericInput(R.string.pref_cv_logging_ms,
                  R.string.summary_logging_window, prefs.windowLoggingMs, 0)
        }

        if (DBG.LMT) {
          setNumericInput(R.string.pref_smas_location_refresh,
                  R.string.summary_refresh_locations, prefs.locationRefreshMs, 500)
        } else {
          setNumericInput(R.string.pref_smas_location_refresh,
                  R.string.summary_refresh_locations, prefs.locationRefreshMs, 0)
        }


        setBooleanInput(R.string.pref_cv_dev_mode, prefs.devMode)
        setBooleanInput(R.string.pref_cv_autoset_initial_location, prefs.autoSetInitialLocation)
        setBooleanInput(R.string.pref_cv_follow_selected_user, prefs.followSelectedUser)

        setupChangeCvModel()
        setupButtonServerSettings()
        setupClearCvFingerprints()
        setupUiClearCvModelsDB(app, VM)

        setupDownloadCvMap(app, VM)
        clearAvailableSpaces(app, VM, VMap)
        clearSelectedSpace()
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

    private fun setupUiClearCvModelsDB(app: AnyplaceApp, VM: CvViewModel) {
      val pref = findPreference<Preference>(getString(R.string.pref_log_clear_cache_cv_models))
      pref?.setOnPreferenceClickListener {
        LOG.W(TAG, "$METHOD: clear cache")
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
      val pref = findPreference<Preference>(getString(R.string.pref_loc_cv_map))

      if (!DBG.CVM) {
        pref?.isVisible = false
        return
      }

      pref?.setOnPreferenceClickListener {
        LOG.W(TAG, "$METHOD: setting up")
        val mgr = requireActivity().supportFragmentManager
        ConfirmActionDialog.SHOW(mgr, "Download CvMap",
                "The previous CvMap will be overridden.\nAn application restart is required.",
                cancellable = true, isImportant = false) { // on confirmed

          lifecycleScope.launch(Dispatchers.IO) {
            if (!app.hasInternet()) {
              app.showToast(VM.viewModelScope, "Internet connection is required")
              return@launch
            }

            VM.nwCvMapGet.safeCall()
            VM.nwCvMapGet.collect()
          }
        }
        true
      }
    }


    fun getSelectedSpaceSummary(value: String) : String {
      return value.ifEmpty {
        "No space selected"
      }
    }

    /**
     * Clears the selected space.
     * The user will have to select a different one from the SpaceSelector
     */
    private fun clearSelectedSpace() {
      val pref = findPreference<Preference>(getString(R.string.pref_selected_space))
      if (!DBG.SLR) { pref?.isVisible = false; return }

      lifecycleScope.launch(Dispatchers.IO) {
        lifecycleScope.launch(Dispatchers.Main) {
          pref?.summary = getSelectedSpaceSummary(ds.read.first().selectedSpace)
        }
      }

      // set initial value and update on new values
      pref?.setOnPreferenceChangeListener { it, value ->
        it.summary=getSelectedSpaceSummary(value.toString())
        true
      }

      pref?.setOnPreferenceClickListener {
        LOG.W(TAG, "$METHOD: setting up")
        val mgr = requireActivity().supportFragmentManager
        ConfirmActionDialog.SHOW(mgr, "Clear selected space",
                "Space Selector will open again,"+
                        "so you can select a different space.\n",
                cancellable = true, isImportant = false) { // on confirmed

          lifecycleScope.launch(Dispatchers.IO) {
            ds.clearSelectedSpace()
            app.showToast(lifecycleScope, "Please select another space.")
            requireActivity().finishAndRemoveTask()
          }
        }
        true
      }
    }


    private fun clearAvailableSpaces(app: AnyplaceApp, VM: CvViewModel, VMap: AnyplaceViewModel) {
      val pref = findPreference<Preference>(getString(R.string.pref_clear_available_spaces))

      if (!DBG.SLR) { pref?.isVisible = false; return }

      pref?.setOnPreferenceClickListener {
        LOG.W(TAG, "$METHOD: setting up")
        val mgr = requireActivity().supportFragmentManager
        ConfirmActionDialog.SHOW(mgr, "Clear available spaces",
                "Space Selector will fetch them again from remote.\n"+
                        "Use this if the remote spaces had changes.\n",
                cancellable = true, isImportant = true) { // on confirmed

          lifecycleScope.launch(Dispatchers.IO) {
            if (!app.hasInternet()) {
              app.showToast(VM.viewModelScope, "Internet connection is required")
              return@launch
            }

            repoAP.local.dropSpaces()
            VMap.setBackFromSettings()
            app.showToast(VM.viewModelScope, "Please select a space.")
            requireActivity().finishAndRemoveTask()
          }
        }
        true
      }
    }

    // CLR: V22 implemented in the UI btnUploadDiscard
    private fun setupClearCvFingerprints() {
      val pref = findPreference<Preference>(getString(R.string.pref_log_clear_cache_cv_fingerprints))
      pref?.isEnabled = cache.hasFingerprints()
      pref?.isVisible=false  // TODO:PMX: V22

      pref?.setOnPreferenceClickListener {
        val mgr=requireActivity().supportFragmentManager
        // TODO:PMX: V22
        // ConfirmActionDialog.SHOW(mgr, "Discard CV Fingerprint cache",
        //         "These are scanned objects that have not been uploaded yet to the database.\n" +
        //                 "Proceed only if you want to discard them.") { // on confirmed
        //   lifecycleScope.launch(Dispatchers.IO) {
        //     cache.deleteFingerprintsCache()
        //   }
        // }
        true
      }
    }

  }


}
