package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.navigator

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.BuildConfig
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.ACT_NAME_NAV
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.CvMapActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.BottomSheetCvUI
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.Classifier
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilNotify
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.DetectorViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationResult
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.UserCoordinates
import cy.ac.ucy.cs.anyplace.lib.android.ui.settings.MainSettingsDialog
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.LocalizationMode
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.TrackingMode
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasChatViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasMainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

/**
 * CV Navigator Activity
 * NOTE: THIS ACTIVITY WAS CREATED/EXTRACTED FROM: [SmasMainActivity]
 * - Probably more things need to be done to clean it up.
 *
 * It's like SMAS, without chat and alerts
 * - markers, users, etc still appear
 *
 * TODO: a better approach would be to:
 * - create a CvNavigatorBaseActivityt
 * - put in there all common functionality
 * - and extend it here, and also in [SmasMainActivity] to specialize
 * - this parent class should be abstract
 */
@AndroidEntryPoint
class CvNavigatorActivity : CvMapActivity(), OnMapReadyCallback {
  private var TG = "act-cv-nav"

  //// PROVIDE TO BASE CLASS ([CvMapActivity]), which will provide to its base class
  override val layout_activity: Int get() = R.layout.activity_cv_navigator
  override val id_bottomsheet: Int get() = R.id.bottom_sheet_cvmap
  override val id_gesture_layout: Int get() = R.id.gesture_layout
  //// PROVIDE TO IMMEDIATE PARENT CLASS [CvMapActivity]:
  override val id_gmap: Int get() = R.id.mapView
  override val actName = ACT_NAME_NAV
  override val id_btn_settings: Int get() = R.id.button_settings
  ////// FLOOR SELECTOR
  override val id_group_levelSelector : Int get() = R.id.group_levelSelector
  override val id_tvTitleFloor: Int get() = R.id.textView_titleLevel
  override val id_btnSelectedFloor: Int get() = R.id.button_selectedLevel
  override val id_btnFloorUp: Int get() = R.id.button_levelUp
  override val id_btnFloorDown: Int get() = R.id.button_levelDown
  ////// UI-LOCALIZATION
  override val id_btn_localization: Int get() = R.id.btn_localization
  override val id_btn_whereami: Int get() = R.id.btn_whereami

  @Suppress("UNCHECKED_CAST")
  override val view_model_class: Class<DetectorViewModel> =
          SmasMainViewModel::class.java as Class<DetectorViewModel>

  // VIEW MODELS
  /** extends [CvViewModel] */
  private lateinit var VM: SmasMainViewModel

  /** Async handling of SMAS Messages and Alerts */
  private lateinit var VMsmas: SmasChatViewModel

  // UI COMPONENTS
  private val btnImu by lazy { findViewById<MaterialButton>(R.id.btn_imu) }
  private val utlNotify by lazy { UtilNotify(applicationContext) }

  /** whether this activity is active or not */
  private var isActive = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    LOG.D2(TG, "onCreate")
  }

  /**
   * Called by [CvMapActivity]
   */
  override fun setupUi() {
    super.setupUi()
    val MT = ::setupUi.name
    LOG.D2(TG, MT)

    setupButtonSettings()
    // setupButtonChat()
    // setupButtonFlir()
    // setupButtonAlert()

    VM.ui.localization.setupButtonImu(btnImu)
  }


  override fun onMapReady(googleMap: GoogleMap) {
    super.onMapReady(googleMap)
    setupMapLongClick(googleMap)
  }

  var handlingGmapLongClick= false
  private fun setupMapLongClick(googleMap: GoogleMap) {
    if (handlingGmapLongClick) return; handlingGmapLongClick=true

    val MT = ::setupMapLongClick.name

    // BUG:F34LC: for some reason some normal clicks are registered as long-clicks
    googleMap.setOnMapLongClickListener {

      LOG.W(TG, "$MT: long-click received")
      lifecycleScope.launch(Dispatchers.IO) {
        if (VM.trackingMode.first() != TrackingMode.off
                && VM.localizationMode.first() != LocalizationMode.stopped) {
          LOG.W(TG, "$MT: ignoring long-click (localization / tracking)")
          return@launch
        }

        forceUserLocation(it)
      }
    }
  }

  private suspend fun forceUserLocation(forcedLocation: LatLng) {
    val MT = ::forceUserLocation.name
    LOG.W(TG, "$MT: $forcedLocation")
    val msg = "Location set manually (long-clicked)"

    if (app.dsMisc.showTutorialNavMapLongPress()) {
      notify.TUTORIAL(lifecycleScope, "$actName LONG-PRESS:\nsets manually the user's location.\nResult: $msg")
    } else {
      notify.short(lifecycleScope, msg)
    }

    if (app.wLevel==null) {
      notify.warn(lifecycleScope, "Cannot load space.")
      return
    }

    val floorNum = app.wLevel!!.levelNumber()
    val loc = forcedLocation.toCoord(floorNum)
    app.locationSmas.update { LocalizationResult.Success(loc, LocalizationResult.MANUAL) }
  }

  /**
   * Called by [CvMapActivity] ? (some parent method)
   */
  override fun postResume() {
    super.postResume()
    val MT = ::postResume.name
    LOG.D2(TG, MT)

    VM = _vm as SmasMainViewModel
    app.setMainView(findViewById(R.id.layout_root), false)

    VMsmas = ViewModelProvider(this)[SmasChatViewModel::class.java]
    appSmas.setMainActivityVMs(VM, VMsmas)

    VM.readBackendVersion()
    setupCollectors()
  }

  /**
   * Runs only once, when any of the floors is loaded for the first time.
   */
  override fun onFirstLevelLoaded() {
    val MT = ::onFirstLevelLoaded.name
    LOG.D2(TG, "$MT: Floor: ${app.level.value?.number}")

    super.onFirstLevelLoaded()

    updateLocationsLOOP()  // send own location & receive other users locations
    lifecycleScope.launch(Dispatchers.IO) {
      VM.waitForUi()
      VM.collectLocations(VMsmas, VM.ui.map)
    }
    // collectAlertingUser()
    VM.collectUserOutOfBounds()
  }


  /* Runs when any of the floors is loaded */
  override fun onLevelLoaded() {
    super.onLevelLoaded()

    lifecycleScope.launch(Dispatchers.IO) {
      // workaround: wait for UI to be ready (not the best one)
      VM.waitForUi()
      VM.ui.map.markers.clearChatLocationMarker()
    }
  }

  /*
   * In a loop:
   * - conditionally send own location
   * - get other users locations
   */
  var updatingLocationsLoop = false
  private fun updateLocationsLOOP()  {
    if (updatingLocationsLoop) return
    updatingLocationsLoop=true

    lifecycleScope.launch(Dispatchers.IO) {
      while (true) {
        var msg = "pull"
        if (isActive && app.hasLastLocation()) {
          val coords = app.locationSmas.value.coord!!
          val userCoords = UserCoordinates(app.wSpace.obj.buid,
                  coords.level, coords.lat, coords.lon)
          VM.nwLocationSend.safeCall(userCoords)
          msg+="&send"
        }

        msg="($msg) "
        if (!isActive) msg+=" [inactive]"
        if (!app.hasLastLocation()) msg+=" [no-location-yet]"

        if (!app.hasInternet()) {
          msg+="[SKIP: NO-INTERNET]"
        } else {
          VM.nwLocationGet.safeCall()
        }

        LOG.V2(TG, "loop-location: main: $msg")

        delay(VM.prefsCvMap.locationRefreshMs.toLong())
      }
    }
  }

  ////////////////////////////////////////////////

  override fun onResume() {
    super.onResume()
    val MT = ::onResume.name
    LOG.W(TG, MT)

    dsCvMap.setMainActivity(CONST.START_ACT_NAV)

    isActive = true
    VMsensor.registerListeners()
  }

  override fun setupUiAfterGmap() {
    // bsheet will be hidden in SMAS
    uiBottom = BottomSheetCvUI(this@CvNavigatorActivity, false)
  }

  override fun onPause() {
    super.onPause()
    val MT = ::onPause.name
    LOG.W(TG, "$TG: $MT")
    isActive = false

    VMsensor.unregisterListener()
  }

  /**
   * Async Collection of remotely fetched data
   */
  private fun setupCollectors() {
    val MT = ::setupCollectors.name
    LOG.D(TG, MT)

    observeLevels()
  }

  private fun setupButtonSettings() {
    btnSettings.setOnClickListener {
      val versionStr = BuildConfig.VERSION_CODE
      MainSettingsDialog.SHOW(supportFragmentManager,
              MainSettingsDialog.FROM_MAIN, this@CvNavigatorActivity, versionStr)
    }
  }

  override fun onInferenceRan(detections: MutableList<Classifier.Recognition>) {
    val MT = ::onInferenceRan.name
    LOG.D3(TG, "$MT")
    VM.ui.onInferenceRan()
    VM.processDetections(detections, this@CvNavigatorActivity)
  }

}