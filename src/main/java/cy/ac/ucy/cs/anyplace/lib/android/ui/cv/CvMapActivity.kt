package cy.ac.ucy.cs.anyplace.lib.android.ui.cv

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.MapBounds
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.LevelWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.SpaceWrapper.Companion.BUID_HARDCODED
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.CvEnginePrefs
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.LevelSelector
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.BottomSheetCvUI
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.CvUI
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.DetectorActivityBase
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilColor
import cy.ac.ucy.cs.anyplace.lib.android.utils.demo.AssetReader
import cy.ac.ucy.cs.anyplace.lib.android.utils.imu.IMU
import cy.ac.ucy.cs.anyplace.lib.android.utils.imu.SensorsViewModel
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.UtilUI
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.DetectorViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw.CvLocalizeNW
import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


/**
 * It uses:
 * - uses Yolov4 TFLite
 * - Settings
 * - Google Maps
 *   - cache mechanism + floor changing
 *
 *   - reuse functionality using Helper UI classes/objects?
 */
@AndroidEntryPoint
abstract class CvMapActivity : DetectorActivityBase(), OnMapReadyCallback {
  // ALL THESE COMPONENTS MUST BE PROVIDED TO ANY CLASS THAT INHERITS [CvMapActivity]
  //// PROVIDE TO BASE CLASS [CameraActivity]:
  override val layout_activity: Int get() = R.layout.example_cvmap
  override val id_bottomsheet: Int get() = R.id.bottom_sheet_layout
  override val id_gesture_layout: Int get() = R.id.gesture_layout

  //// PROVIDE TO THIS CLASS [CvMapActivity]:
  abstract val actName: String
  protected abstract val id_gmap: Int
  protected abstract val id_btn_settings: Int
  ////// FLOOR SELECTOR
  protected abstract val id_group_floorSelector: Int
  protected abstract val id_tvTitleFloor: Int
  protected abstract val id_btnSelectedFloor: Int
  protected abstract val id_btnFloorUp: Int
  protected abstract val id_btnFloorDown: Int
  ////// UI-LOCALIZATION
  protected abstract val id_btn_localization: Int
  protected abstract val id_btn_whereami: Int

  private val C by lazy { CONST(applicationContext) }


  @Suppress("UNCHECKED_CAST")
  override val view_model_class: Class<DetectorViewModel> =
          CvViewModel::class.java as Class<DetectorViewModel>

  // VIEW MODELS
  /** extends [DetectorViewModel] */
  private lateinit var VM: CvViewModel

  private val tvTitle by lazy { findViewById<TextView>(R.id.tvTitle) }
  val btnSettings: MaterialButton by lazy { findViewById(id_btn_settings) }

  // UTILITY OBJECTS
  protected val utlColor by lazy { UtilColor(applicationContext) }
  protected val assetReader by lazy { AssetReader(applicationContext) }
  open lateinit var uiBottom : BottomSheetCvUI  // TODO: put in [CvMapUi]
  val utlUi by lazy { UtilUI(applicationContext, lifecycleScope) }

  private val TAG = "ACT-CVM"

  lateinit var VMsensor: SensorsViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    VMsensor = ViewModelProvider(this)[SensorsViewModel::class.java]

    // CLR:PM
    // XXX: GET THE EXTRAS !!!!
    // val extras = requireActivity().intent.extras
    // spaceH = IntentExtras.getSpace(requireActivity(), repoAP, extras, SettingsCvActivity.ARG_SPACE)
    // floorsH = IntentExtras.getFloors(spaceH, extras, SettingsCvActivity.ARG_FLOORS)
    // floorH = IntentExtras.getFloor(spaceH, extras, SettingsCvActivity.ARG_FLOOR)

    // app.showSnackbarInf(lifecycleScope, "Testing msg here")
    // app.showSnackbarInf(lifecycleScope, "Testing msg here\nThis is the second line\nAnd there is even a third one")
    // app.showSnackbarInfDEV(lifecycleScope, "Testing msg here")
    // app.showSnackbarInfDEV(lifecycleScope, "Testing msg here\nThis is the second line\nAnd there is even a third one")
  }

  override fun postResume() {
    super.postResume()

    VM = _vm as CvViewModel
    VM.setAttachedActivityId(actName)

    LOG.D(TAG, METHOD)

    updateModelName()

    lifecycleScope.launch(Dispatchers.IO) {
      LOG.D(TAG, "CvMap: $METHOD: getting models.. CvModels")
      LOG.I(TAG, "FUTURE: get fingerprints too here?")
      // FUTURE: with a new backend endpoint, we could try to auto-update the local fingerprints given connectivity
      // - the backend must provide timestamp TS on Fingerprint download
      // - that TS should also be materialized (SQLite or DataStore)
      // - then compare that one w/ the latest modification (another endpoint maybe)
      // - if local CvMap/Fingerprint outdated: fetch it again..

      VM.nwCvModelsGet.safeCall()
      VM.nwCvModelsGet.collect()
    }
  }

  override fun onResume() {
    super.onResume()
    LOG.D2()

    continueWithPrefs()
  }


  /**
   * Read preferences and continue setup:
   * -[VM.prefsCV] preferences: model, cvmaps. floorplans,
   * -[DataStoreNav]: map opacity, localization interval, etc
   */
  private fun continueWithPrefs() {
    lifecycleScope.launch(Dispatchers.IO) {
      val method = METHOD
      LOG.W(TAG, method)

      dsCv.read.first { prefs ->
        VM.prefsCv= prefs
        onLoadedPrefsCvEngine(prefs)
        true
      }

      if (!DBG.SLR) {
        LOG.E(TAG, "$method: Forcing space: $BUID_HARDCODED")
        dsCvMap.setSelectedSpace(BUID_HARDCODED)
      }

      dsCvMap.read.first { prefs ->
        VM.prefsCvMap=prefs
        if (DBG.SLR) {
          if (prefs.selectedSpace.isEmpty() || app.mustSelectSpaceForCvMap) {
            app.mustSelectSpaceForCvMap=false // handled below
            app.showToast(lifecycleScope, "Please re-run app.")
            // val intent = Intent(app.applicationContext, SelectSpaceActivity::class.java)
            // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // startActivity(intent)
            finishAndRemoveTask()
          }
        }

        LOG.E(TAG, "$method: SELECTED SPACE ID: '${prefs.selectedSpace}'")
        onLoadedPrefsCvMap()
        true
      }

      VM.reactToPrefChanges()
    }
  }

  // CHECK: is this needed
  private fun onLoadedPrefsCvEngine(cvEnginePrefs: CvEnginePrefs) {
    if (cvEnginePrefs.reloadCvMaps) {
      LOG.W(TAG_METHOD, "Reloading CvMaps and caches.")
      // might need to reload headmap
      // loadHeatmap() on the floor
      // dsCv.setReloadCvMaps(false)
    } else {
      LOG.D(TAG_METHOD, "not reloading (fingerprint or caches)")
    }
  }

  var cvMapPrefsLoaded = false
  private fun onLoadedPrefsCvMap() {
    LOG.W()
    cvMapPrefsLoaded=true
    setupUi()
  }

  /**
   * Initialize bottom sheet by reading the [VM.prefsNav]
   */
  abstract fun setupUiAfterGmap()

  protected open fun setupUi() {
    LOG.E(TAG, "setupUi")
    setupUiFloorSelector()
    setupUiGmap()

    // there is demo localization in Logger too,
    // to validate findings according to the latest CvMap
    VM.ui.localization.collectStatus()

    // keep reacting to  settings updates
    lifecycleScope.launch(Dispatchers.IO) {
      app.dsCvMap.read.collect {
        LOG.V4(TAG, "reacting for BottomSheet")
        setupUiAfterGmap()
        uiBottom.setup()  // CHECK: this may have to change
        VM.uiBottomInited=true
      }
    }

    checkInternet()
    VM.ui.setupOnFloorSelectionClick()
  }

  var floorSelectorInited = false
  private fun setupUiFloorSelector() {
    if (floorSelectorInited) return
    floorSelectorInited=true

    VM.levelSelector = LevelSelector(applicationContext,
            lifecycleScope,
            findViewById(id_group_floorSelector),
            findViewById(id_tvTitleFloor),
            findViewById(id_btnSelectedFloor),
            findViewById(id_btnFloorUp),
            findViewById(id_btnFloorDown))

    /** Updates on the wMap after a floor has changed */
    val fsCallback = object: LevelSelector.Callback() {
      override fun before() {
        LOG.D4(TAG_METHOD, "remove user locations")
        // clear any overlays
        VM.ui.removeHeatmap()
        VM.ui.map.removeUserLocations()
        // [Overlays.drawFloorplan] removes any previous floorplan
        // before drawing a new one so it doesn't need anything.
      }

      override fun after() { }
    }

    VM.levelSelector.callback = fsCallback
  }

  var initedGmap = false
  private fun setupUiGmap() {
    LOG.E(TAG, "Setup CommonUI & GMap")
    if (initedGmap) return

    LOG.W(TAG, "SETUP CommonUI & GMAP: ACTUAL INIT")
    initedGmap=true

    VM.ui = CvUI(
            app, this@CvMapActivity, VM, lifecycleScope,
            supportFragmentManager,
            VM.levelSelector,
            id_btn_localization,
            id_btn_whereami,
    )

    LOG.W(TAG, "$METHOD: ui (component) is now loaded.")
    VM.ui.map.attach(VM, this, R.id.mapView)
    VM.mu = IMU(this,VM, VM.ui.map)
    VM.uiComponentInited=true
  }



  private fun setMapOpacity() {
    val view = findViewById<View>(id_gmap)
    val value =VM.prefsCvMap.mapAlpha.toInt()
    LOG.E(TAG, "$METHOD: setting opacity: $value")
    view.alpha=value/100f
  }

  /** Setup the Google Map:
   * - TODO finalize floorplans
   */
  override fun onMapReady(googleMap: GoogleMap) {
    LOG.I(TAG, "onMapReadyCallback: [CvMap]")

    VM.ui.map.setup(googleMap, this)
    lifecycleScope.launch(Dispatchers.Main) {
      VM.ui.localization.setup()
      collectOwnUserLocation()
    }

  }

  override fun onProcessImageFinished() {
    LOG.V3()
    uiBottom.refreshUi(lifecycleScope)
  }

  protected fun checkInternet() {
    if (!app.hasInternet()) {
      app.snackbarLong(lifecycleScope, C.ERR_MSG_NO_INTERNET)
    }
  }

  var collectingDetections=false
  fun collectDetections() {
    if (collectingDetections) return
    collectingDetections=true

    lifecycleScope.launch {
      VM.detectionsLOC.collectLatest {
        it.forEach { rec ->
          LOG.E(TAG, "Detection: ${rec.id} ${rec.title}")
        }
      }
    }
    // detectionsLocalization.call
  }

  var firstLevelLoaded = false

  open fun onFirstLevelLoaded() {
    LOG.D2(TAG, "First floor loaded: ${app.wLevel?.floorNumber()}")
  }

  open fun onLevelLoaded() {
    LOG.D2(TAG, "Floor loaded: ${app.wLevel?.floorNumber()}")
    lifecycleScope.launch(Dispatchers.IO) {
      if (app.wLevel != null) {
        VM.waitForUi()

        // val usedMethod = LocalizationResult.getUsedMethod(app.locationSmas.value)
        VM.ui.map.markers.updateLocationMarkerBasedOnFloor(app.wLevel!!.floorNumber())
        loadPOIsAndConnections()
      }

      app.userOutOfBounds.update {
        val boundState = when {
          !app.userHasLocation() -> MapBounds.notLocalizedYet
          app.userOnOtherFloor() -> MapBounds.outOfBounds
          else -> MapBounds.inBounds
        }

        boundState
      }
    }
  }

  suspend fun loadPOIsAndConnections() {
      if (!DBG.uim) return

      if (app.space!=null && !VM.cache.hasSpaceConnectionsAndPois(app.space!!)) {
        LOG.D2(TAG, "Fetching POIs and Connections..")
        VM.nwPOIs.callBlocking(app.space!!.buid)
        VM.nwConnections.callBlocking(app.space!!.buid)
      }
      VM.ui.map.lines.loadPolylines(app.wLevel!!.floorNumber())
  }

  var observingLevels = false
  /**
   * Observes when the initial floor will be loaded, and runs a method
   */
  fun observeLevels() {
    if (observingLevels) return
    observingLevels=true

    val method = METHOD
    lifecycleScope.launch(Dispatchers.IO) {
      VM.waitForUi()

      app.level.collect { level ->
        if (level == null) return@collect

        LOG.W(TAG, "$method: collected level: ${level.buid}")
        LOG.W(TAG, "$method: collected level: ${level.number}")

        // update FloorWrapper & FloorSelector
        app.wLevel = LevelWrapper(level, app.wSpace)
        VM.ui.levelSelector.updateFloorSelector(level, app.wLevels)

        if (!firstLevelLoaded) { // runs only when the first level is loaded
          firstLevelLoaded = true
          VM.ui.map.onFirstLevelLoaded()
          onFirstLevelLoaded()
        }

        LOG.V3(TAG, "$METHOD: -> level: ${level.number}")
        LOG.V2(TAG, "$METHOD: -> updating cache: level: ${app.level.value?.number}")
        VM.ui.map.fHandler.cacheLastLevel(app.level.value)
        LOG.V2(TAG, "$METHOD: -> loadFloor: ${level.number}")
        VM.ui.levelSelector.lazilyChangeLevel(VM, lifecycleScope)

        onLevelLoaded()
      }
    }
  }

  var collectingLocationRemote = false
  suspend fun collectOwnUserLocation() {
    if (collectingLocationRemote) return
    collectingLocationRemote=true

    app.locationSmas.collect { result ->
      when (result) {
        is LocalizationResult.Unset -> { }
        is LocalizationResult.Error -> {
          var msg = result.message.toString()
          val details = result.details
          if (details != null) {
            msg+="\n$details"
          }
          app.showToast(lifecycleScope, msg, Toast.LENGTH_LONG)
        }
        is LocalizationResult.Success -> {
          val usedMethod = LocalizationResult.getUsedMethod(result)
          LOG.W(TAG, "Collected: method: $usedMethod")
          result.coord?.let { VM.ui.map.setUserLocation(it, usedMethod) }
          val coord = result.coord!!
          val msg = "${CvLocalizeNW.tag}: Smas location: ${coord.lat}, ${coord.lon} level: ${coord.level}"
          LOG.E(TAG, msg)
          val curFloor = app.wLevel?.floorNumber()
          if (coord.level != curFloor) {
            LOG.W(TAG, "Changing to ${app.wLevel?.prettyFloor}: ${coord.level}")
            // app.showToast(lifecycleScope, )
          }

          app.wLevels.moveToFloor(VM, coord.level)
        }
      }
    }
  }


  /**
   * TODO: for this (and any similar code that loops+delay):
   * - create a variable and observe it (a Flow or something observable/collactable)
   */
  fun updateModelName() {
    val method = METHOD
    LOG.W(TAG, method)
    lifecycleScope.launch(Dispatchers.IO) {
      while (!VM.detectorLoaded) delay(100)

      val modelInfo = "$actName | ${VM.model.modelName}"
      LOG.W(TAG, "$method: $modelInfo")
      utlUi.text(tvTitle, modelInfo)
    }
  }

}