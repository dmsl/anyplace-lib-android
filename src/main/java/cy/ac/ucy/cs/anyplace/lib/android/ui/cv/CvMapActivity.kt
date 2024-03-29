package cy.ac.ucy.cs.anyplace.lib.android.ui.cv

import android.content.Intent
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
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.wrappers.LevelWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.wrappers.SpaceWrapper.Companion.BUID_HARDCODED
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
import cy.ac.ucy.cs.anyplace.lib.android.sensors.imu.IMU
import cy.ac.ucy.cs.anyplace.lib.android.sensors.imu.SensorsViewModel
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.UtilUI
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.DetectorViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


/**
 * Base activity for [CvMap]: Computer Vision stuff + a [GoogleMap]
 * It uses:
 * - uses Yolov4 TFLite
 * - Settings
 * - Google Maps
 *   - cache mechanism + floor changing
 *
 * - it is extended by [CvLoggerActivity], [CvNavigatorActivity], and [SmasMainActivity]
 *   - t allows reusing functionality using Helper UI classes/objects?
 */
@AndroidEntryPoint
abstract class CvMapActivity : DetectorActivityBase(), OnMapReadyCallback {
  private var TG = "act-cv"

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
  protected abstract val id_group_levelSelector: Int
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

  lateinit var VMsensor: SensorsViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    VMsensor = ViewModelProvider(this)[SensorsViewModel::class.java]
  }

  override fun postResume() {
    super.postResume()
    val MT = ::postResume.name

    VM = _vm as CvViewModel
    VM.setAttachedActivityId(actName)

    LOG.D(TG, MT)

    updateModelName()
    collectLoggedInUser()

    lifecycleScope.launch(Dispatchers.IO) {
      LOG.D(TG, "CvMap: $MT: getting models.. CvModels")
      LOG.I(TG, "FUTURE: get fingerprints too here?")
      // FUTURE: with a new backend endpoint, we could try to auto-update the local fingerprints given connectivity
      // - the backend must provide timestamp TS on Fingerprint download
      // - that TS should also be materialized (SQLite or DataStore)
      // - then compare that one w/ the latest modification (another endpoint maybe)
      // - if local CvMap/Fingerprint outdated: fetch it again..

      checkForFingerprintUpdates()

      VM.nwCvModelsGet.safeCall()
      VM.nwCvModelsGet.collect()
    }
  }

  override fun onResume() {
    super.onResume()
    LOG.V2(TG, "onResume")

    continueWithPrefs()
  }

  /**
   * Reacts to updates on [ChatUser]'s login status:
   * Only authenticated users are allowed to use this activity
   */
  var collectingUser = false
  private fun collectLoggedInUser() {
    if (collectingUser) return
    collectingUser = true

    // only logged in users are allowed on this activity:
    lifecycleScope.launch(Dispatchers.IO) {
      app.dsUserSmas.read.collectLatest { user ->
        if (user.sessionkey.isBlank()) {
          finish()

          startActivity(Intent(this@CvMapActivity, app.getSmasBackendLoginActivity()))
        }
      }
    }
  }

  /**
   * Read preferences and continue setup:
   * -[VM.prefsCV] preferences: model, cvmaps. floorplans,
   * -[DataStoreNav]: map opacity, localization interval, etc
   */
  private fun continueWithPrefs() {
    lifecycleScope.launch(Dispatchers.IO) {
      val MT = ::continueWithPrefs.name
      LOG.D(TG, MT)

      dsCv.read.first { prefs ->
        VM.prefsCv= prefs
        onLoadedPrefsCvEngine(prefs)
        true
      }

      if (!DBG.USE_SPACE_SELECTOR) {
        LOG.E(TG, "$MT: Forcing space: $BUID_HARDCODED")
        dsCvMap.setSelectedSpace(BUID_HARDCODED)
      }

      dsCvMap.read.first { prefs ->
        VM.prefsCvMap=prefs
        LOG.D2(TG, "$MT: SELECTED SPACE ID: '${prefs.selectedSpace}'")
        onLoadedPrefsCvMap()
        true
      }

      VM.reactToPrefChanges()
    }
  }

  private fun onLoadedPrefsCvEngine(cvEnginePrefs: CvEnginePrefs) {
    val MT = ::onLoadedPrefsCvEngine.name
    if (cvEnginePrefs.reloadCvMaps) {
      LOG.W(TG, "$MT: Reloading CvMaps and caches.")
      // might need to reload headmap
      // loadHeatmap() on the floor
      // dsCv.setReloadCvMaps(false)
    } else {
      LOG.D(TG, "$MT: not reloading (fingerprint or caches)")
    }
  }

  var cvMapPrefsLoaded = false
  private fun onLoadedPrefsCvMap() {
    LOG.V2(TG, "onLoadedPrefsCvMap")
    cvMapPrefsLoaded=true
    setupUi()
  }

  /**
   * Initialize bottom sheet by reading the [VM.prefsNav]
   */
  abstract fun setupUiAfterGmap()

  protected open fun setupUi() {
    LOG.E(TG, "setupUi")
    setupUiFloorSelector()
    setupUiGmap()

    // there is demo localization in Logger too,
    // to validate findings according to the latest CvMap
    VM.ui.localization.collectStatus()

    // keep reacting to  settings updates
    lifecycleScope.launch(Dispatchers.IO) {
      app.dsCvMap.read.collect {
        LOG.V4(TG, "reacting for BottomSheet")
        setupUiAfterGmap()
        uiBottom.setup()  // CHECK: this may have to change
        VM.initedBsheet=true
      }
    }

    checkInternet()
    VM.ui.setupLevelSelectorClicks()
  }

  var floorSelectorInited = false
  private fun setupUiFloorSelector() {
    if (floorSelectorInited) return
    floorSelectorInited=true

    VM.levelSelector = LevelSelector(applicationContext,
            lifecycleScope,
            findViewById(id_group_levelSelector),
            findViewById(id_tvTitleFloor),
            findViewById(id_btnSelectedFloor),
            findViewById(id_btnFloorUp),
            findViewById(id_btnFloorDown))

    /** Updates on the wMap after a floor has changed */
    val fsCallback = object: LevelSelector.Callback() {
      override fun before() {
        LOG.D4(TG, "remove user locations")
        // clear any overlays
        VM.ui.map.overlays.removeHeatmap()
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
    val MT = ::setupUiGmap.name
    if (initedGmap) return
    initedGmap=true

    LOG.W(TG, MT)

    VM.ui = CvUI(app, this@CvMapActivity, VM, lifecycleScope,
            supportFragmentManager,
            VM.levelSelector,
            id_btn_localization,
            id_btn_whereami)

    LOG.W(TG, "$MT: ui (component) is now loaded.")
    VM.ui.map.attach(VM, this, R.id.mapView)
    VM.mu = IMU(this,VM, VM.ui.map)
    VM.initedUi=true
  }

  private fun setMapOpacity() {
    val view = findViewById<View>(id_gmap)
    val value =VM.prefsCvMap.mapAlpha.toInt()
    LOG.E(TG, "setting opacity: $value")
    view.alpha=value/100f
  }

  /** Setup the Google Map:
   * - TODO finalize floorplans
   */
  override fun onMapReady(googleMap: GoogleMap) {
    val MT = ::onMapReady.name
    LOG.W(TG, "$MT: map ready")

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
      notify.long(lifecycleScope, C.ERR_MSG_NO_INTERNET)
    }
  }

  var collectingDetections=false
  fun collectDetections() {
    if (collectingDetections) return
    collectingDetections=true
    lifecycleScope.launch(Dispatchers.IO) {
      VM.detectionsLOC.collectLatest {
        it.forEach { rec ->
          LOG.E(TG, "Detection: ${rec.id} ${rec.title}")
        }
      }
    }
  }

  var firstLevelLoaded = false
  open fun onFirstLevelLoaded() {
    val MT = ::onFirstLevelLoaded.name
    LOG.D2(TG, "$MT: ${app.wLevel?.levelNumber()}")
  }

  open fun onLevelLoaded() {
    val MT = ::onLevelLoaded.name
    LOG.D2(TG, "$MT: : ${app.wLevel?.levelNumber()}")
    lifecycleScope.launch(Dispatchers.IO) {
      if (app.wLevel != null) {
        VM.waitForUi()
        VM.ui.map.markers.updateLocationMarkerBasedOnFloor(app.wLevel!!.levelNumber())
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
    val MT = ::loadPOIsAndConnections.name
      if (app.space!=null && !VM.cache.hasSpaceConnectionsAndPois(app.space!!)) {
        VM.downloadingPoisAndConnections=true
        LOG.D2(TG, "$MT: Fetching POIs and Connections..")
        VM.nwPOIs.callBlocking(app.space!!.buid)
        VM.nwConnections.callBlocking(app.space!!.buid)
        VM.downloadingPoisAndConnections=false
      }
      VM.ui.map.lines.loadPolylines(app.wLevel!!.levelNumber())
  }

  var observingLevels = false
  /**
   * Observes when the initial floor will be loaded, and runs a method
   */
  fun observeLevels() {
    val MT = ::observeLevels.name

    if (observingLevels) return
    observingLevels=true

    lifecycleScope.launch(Dispatchers.IO) {
      VM.waitForUi()

      app.level.collect { level ->
        if (level == null) return@collect

        LOG.V2(TG, "$MT: collected level: ${level.buid}/${level.number} ")

        // update FloorWrapper & FloorSelector
        app.wLevel = LevelWrapper(level, app.wSpace)
        VM.ui.levelSelector.updateFloorSelector(level, app.wLevels)

        if (!firstLevelLoaded) { // runs only when the first level is loaded
          firstLevelLoaded = true
          VM.ui.map.onFirstLevelLoaded()
          onFirstLevelLoaded()
        }

        LOG.V2(TG, "$MT: -> level: ${level.number}")
        LOG.V2(TG, "$MT: -> updating cache: level: ${app.level.value?.number}")
        VM.cacheLastLevel(app.level.value)
        LOG.V2(TG, "$MT: -> loadFloor: ${level.number}")
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
          LOG.D(TG, "Collected: method: $usedMethod")
          result.coord?.let { VM.ui.map.setUserLocation(it, usedMethod) }
          val coord = result.coord!!
          val msg = "$TG: Smas location: ${coord.lat}, ${coord.lon} level: ${coord.level}"
          LOG.V2(TG, msg)
          val curFloor = app.wLevel?.levelNumber()
          if (coord.level != curFloor) {
            LOG.D(TG, "Changing to ${app.wLevel?.prettyFloor}: ${coord.level}")
            // app.showToast(lifecycleScope, )
          }
          app.wLevels.moveToFloor(VM, coord.level)
        }
      }
    }
  }

  /**
   * - create a variable and observe it (a Flow or something observable)
   */
  fun updateModelName() {
    val MT = ::updateModelName.name
    LOG.D3(TG, MT)
    lifecycleScope.launch(Dispatchers.IO) {
      VM.waitForDetector()

      val modelInfo = "$actName | ${VM.model.modelName}"
      LOG.V3(TG, "$MT: $modelInfo")
      utlUi.text(tvTitle, modelInfo)
    }
  }

  private suspend fun checkForFingerprintUpdates() {
    val MT = ::checkForFingerprintUpdates.name
    LOG.W(TG, MT)
    if (app.dsCvMap.read.first().autoUpdateCvFingerprints) {
      VM.waitForDetector()
      app.waitForSpace()
      if (app.hasInternet()) {
        LOG.D2(TG, "$MT: checking..")
        VM.nwCvFingerprintsGet.safeCall(false)
      }
    }
  }
}