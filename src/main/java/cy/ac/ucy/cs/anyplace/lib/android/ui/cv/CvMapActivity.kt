package cy.ac.ucy.cs.anyplace.lib.android.ui.cv

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.FloorWrapper
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.CvEnginePrefs
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.utils.imu.IMU
import cy.ac.ucy.cs.anyplace.lib.android.utils.imu.SensorsViewModel
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.BottomSheetCvUI
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.FloorSelector
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.CvUI
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.DetectorActivityBase
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilColor
import cy.ac.ucy.cs.anyplace.lib.android.utils.demo.AssetReader
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
  protected abstract val id_gmap: Int
  protected abstract val actName: String

  // PROVIDE TO BASE CLASS [CameraActivity]:
  override val layout_activity: Int get() = R.layout.example_cvmap
  override val id_bottomsheet: Int get() = R.id.bottom_sheet_layout
  override val id_gesture_layout: Int get() = R.id.gesture_layout

  @Suppress("UNCHECKED_CAST")
  override val view_model_class: Class<DetectorViewModel> =
          CvViewModel::class.java as Class<DetectorViewModel>

  // VIEW MODELS
  /** extends [DetectorViewModel] */
  private lateinit var VM: CvViewModel

  private val tvTitle by lazy { findViewById<TextView>(R.id.tvTitle) }

  // UTILITY OBJECTS
  protected val utlColor by lazy { UtilColor(applicationContext) }
  protected val assetReader by lazy { AssetReader(applicationContext) }
  protected open lateinit var uiBottom : BottomSheetCvUI  // TODO: put in [CvMapUi]
  val utlUi by lazy { UtilUI(applicationContext, lifecycleScope) }

  private val tag = "act-cvcomm"

  lateinit var VMs: SensorsViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    VMs = ViewModelProvider(this)[SensorsViewModel::class.java]
  }

  override fun postResume() {
    super.postResume()

    VM = _vm as CvViewModel
    LOG.V2(TAG_METHOD, "ViewModel: VM currentTime: ${VM.currentTime}")

    LOG.D(TAG, "$tag: $METHOD")

    updateModelName()

    lifecycleScope.launch(Dispatchers.IO) {
      LOG.D(TAG, "CvMap: $METHOD: getting models.. CvModels")
      LOG.E(TAG, "FUTURE: get fingerprints too here?")
      // TODO: DZ: endpoint: accept timestamp (like chat) of LAST UPDATE
      // (we may delete a fingerprint. update.. etc..)
      // must be a different table: per spaceId
      // return either: empty
      VM.nwCvModelsGet.safeCall()
      VM.nwCvModelsGet.collect()
    }
  }

  override fun onResume() {
    super.onResume()
    LOG.E(TAG, "onResume")

    readPrefsAndContinue()
  }


  /**
   * Read preferences and continue setup:
   * -[VM.prefsCV] preferences: model, cvmaps. floorplans,
   * -[DataStoreNav]: map opacity, localization interval, etc
   */
  private fun readPrefsAndContinue() {
    lifecycleScope.launch(Dispatchers.IO) {
      LOG.W(TAG, "$tag: $METHOD")
      dsCv.read.first { prefs ->
        VM.prefsCv= prefs
        onLoadedPrefsCvEngine(prefs)
        true
      }

      dsCvMap.read.first { prefs ->
        VM.prefsCvMap=prefs
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
      LOG.E(TAG, "CHECK: might need to reload headmap")
      // loadHeatmap() on the floor
      // dsCv.setReloadCvMaps(false)
    } else {
      LOG.D(TAG_METHOD, "not reloading (fingerprint or caches)")
    }
  }

  private fun onLoadedPrefsCvMap() {
    LOG.W()
    setupUi()
  }

  /**
   * Initialize bottom sheet by reading the [VM.prefsNav]
   */
  open fun setupUiAfterGmap() {
    uiBottom = BottomSheetCvUI(this@CvMapActivity, VM.prefsCvMap.devMode)
  }

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
        LOG.V4(TAG, "$tag: reacting for BottomSheet")
        setupUiAfterGmap()
        uiBottom.setup()  // CHECK: this may have to change
      }
    }

    checkInternet()
    VM.ui.setupOnFloorSelectionClick()
  }

  var initedGmap = false
  private fun setupUiGmap() {
    LOG.D2(TAG, "Setup CommonUI & GMap")
    if (initedGmap) return

    LOG.W(TAG, "SETUP CommonUI & GMAP: ACTUAL INIT")
    initedGmap=true
    VM.ui = CvUI(app, this@CvMapActivity, VM, lifecycleScope,
            supportFragmentManager, VM.floorSelector)
    LOG.W(TAG, "$METHOD: ui (component) is now loaded.")
    VM.uiComponentLoaded=true
    VM.ui.map.attach(VM, this, R.id.mapView)
    VM.mu = IMU(this,VM, VM.ui.map)
  }

  var floorSelectorInited = false
  private fun setupUiFloorSelector() {
    if (floorSelectorInited) return
    floorSelectorInited=true

    VM.floorSelector = FloorSelector(applicationContext,
            lifecycleScope,
            findViewById(R.id.group_floorSelector),
            findViewById(R.id.textView_titleFloor),
            findViewById(R.id.button_selectedFloor),
            findViewById(R.id.button_floorUp),
            findViewById(R.id.button_floorDown))

    /** Updates on the wMap after a floor has changed */
    val fsCallback = object: FloorSelector.Callback() {
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

    VM.floorSelector.callback = fsCallback
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
    LOG.E(TAG, "onMapReadyCallback: [CvMap]")
    VM.ui.map.setup(googleMap)
    VM.ui.localization.setupClick()

    collectLocationREMOTE()
  }

  override fun onProcessImageFinished() {
    LOG.V3()
    uiBottom.refreshUi(lifecycleScope)
  }

  protected fun checkInternet() {
    if (!app.hasInternet()) {
      app.showToast(lifecycleScope, "No internet!")
    }
  }

  fun observerDetections() {
    lifecycleScope.launch {
      VM.detectionsLOC.collectLatest {
        it.forEach { rec ->
          LOG.E(TAG, "Detection: ${rec.id} ${rec.title}")
        }
      }
    }
    // detectionsLocalization.coll
  }

  var firstFloorLoaded = false

  open fun onFirstFloorLoaded() {
    LOG.D2(TAG, "First floor loaded: ${app.wFloor?.floorNumber()}")
  }

  open fun onFloorLoaded() {
    LOG.D2(TAG, "Floor loaded: ${app.wFloor?.floorNumber()}")
    lifecycleScope.launch(Dispatchers.IO) {
      if (app.wFloor != null) {
      while (!VM.uiLoaded()) delay(100) // workaround (not the best one..)

      VM.ui.map.markers.updateLocationMarkerBasedOnFloor(app.wFloor!!.floorNumber())
        test()
      }
    }
  }

  suspend fun test() {
      if (!DBG.uim) return

      if (app.space!=null && !VM.cache.hasSpaceConnectionsAndPois(app.space!!)) {
        LOG.E(TAG, "will get pois conns")
        VM.nwPOIs.safeCall(app.space!!.id)
        VM.nwConnections.safeCall(app.space!!.id)
      }
      VM.ui.map.lines.loadPolylines(app.wFloor!!.floorNumber())
  }

  var observingFloors = false
  /**
   * Observes when the initial floor will be loaded, and runs a method
   */
  fun observeFloors() {
    if (observingFloors) return
    observingFloors=true

    val _method = METHOD
    lifecycleScope.launch(Dispatchers.IO) {
      app.floor.collect { floor ->
        LOG.D1(TAG, "$_method: floor is: ${floor?.floorNumber}")
        if (floor == null) return@collect

        // LOG.D4(TAG, "$_method: is spaceH filled? ${app.spaceH.obj.name}")
        // // Update FH
        app.wFloor = FloorWrapper(floor, app.wSpace)
        // LOG.E(TAG, "$_method: floor now is: ${VM.floorH!!.floorNumber()}")
        // wMap.markers.updateLocationMarkerBasedOnFloor(VM.floorH!!.floorNumber())

        if (!firstFloorLoaded) { // runs only when the first floor is loaded
          onFirstFloorLoaded()
          firstFloorLoaded = true
        }

        onFloorLoaded()
      }
    }
  }

  var collectingLocationRemote = false
  fun collectLocationREMOTE() {
    if (collectingLocationRemote) return
    collectingLocationRemote=true

    lifecycleScope.launch (Dispatchers.IO){
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

            val isManual = LocalizationResult.isManual(result)
            LOG.W(TAG, "Collected: isManual: $isManual")
            result.coord?.let { VM.ui.map.setUserLocation(it, isManual) }
            val coord = result.coord!!
            val msg = "${CvLocalizeNW.tag}: Smas location: ${coord.lat}, ${coord.lon} floor: ${coord.level}"
            LOG.D2(TAG, msg)
            val curFloor = app.wFloor?.floorNumber()
            if (coord.level != curFloor) {
              app.showToast(lifecycleScope, "Changing floor: ${coord.level} (from: ${curFloor})")
            }

            app.wFloors.moveToFloor(VM, coord.level)
          }
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

      val modelInfo = "$actName (model: ${VM.model.modelName})"
      LOG.W(TAG, "$method: $modelInfo")
      utlUi.text(tvTitle, modelInfo)
    }
  }

}