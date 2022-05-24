package cy.ac.ucy.cs.anyplace.lib.android.ui.cv

import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.store.CvPrefs
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.maps.Overlays
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.BottomSheetCvUI
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.FloorSelector
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.GmapWrapper
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.CvMapUi
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.DetectorActivityBase
import cy.ac.ucy.cs.anyplace.lib.android.utils.demo.AssetReader
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.CvMapViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.DetectorViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
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

  // PROVIDE TO BASE CLASS [CameraActivity]:
  override val layout_activity: Int get() = R.layout.example_cvmap
  override val id_bottomsheet: Int get() = R.id.bottom_sheet_layout
  override val id_gesture_layout: Int get() = R.id.gesture_layout

  @Suppress("UNCHECKED_CAST")
  override val view_model_class: Class<DetectorViewModel> =
          CvMapViewModel::class.java as Class<DetectorViewModel>

  // VIEW MODELS
  /** extends [DetectorViewModel] */
  private lateinit var VM: CvMapViewModel

  // UTILITY OBJECTS
  protected lateinit var wMap: GmapWrapper
  protected val overlays by lazy { Overlays(applicationContext) }
  protected val assetReader by lazy { AssetReader(applicationContext) }
  protected open lateinit var uiBottom : BottomSheetCvUI


  // UI
  //// COMPONENTS
  protected lateinit var floorSelector: FloorSelector
  protected lateinit var ui: CvMapUi

  override fun postCreate() {
    super.postCreate()
    VM = _vm as CvMapViewModel
    LOG.V2(TAG_METHOD, "ViewModel: VM currentTime: ${VM.currentTime}")
  }

  override fun onResume() {
    super.onResume()
    LOG.V2(TAG, "onResume")

    readPrefsAndContinue()
  }

  /**
   * Read preferences and continue setup:
   * -[VM.prefsCV] preferences: model, cvmaps. floorplans,
   * -[DataStoreNav]: map opacity, localization interval, etc
   */
  private fun readPrefsAndContinue() {
    lifecycleScope.launch(Dispatchers.IO) {
      LOG.V()
      dsCv.read.first { prefs ->
        VM.prefsCV= prefs
        onCvPrefsLoaded(prefs)
        true
      }

      LOG.D(TAG, "CvMapActivity: readPrefsAndContinue: calls read")
      dsCvNav.read.first { prefs ->
        VM.prefsNav = prefs
        onNavPrefsLoaded()
        true
      }
    }
  }

  private fun onCvPrefsLoaded(cvPrefs: CvPrefs) {
    if (cvPrefs.reloadCvMaps) {
      LOG.W(TAG_METHOD, "Reloading CvMaps and caches.")
      // refresh CvMap+Heatmap only when needed
      // TODO do something similar with floorplans when necessary as well
      // loadCvMapAndHeatmap() // TODO call this..
      dsCv.setReloadCvMaps(false)
    } else {
      LOG.D(TAG_METHOD, "not reloading (cvmap or caches)")
    }
  }

  private fun onNavPrefsLoaded() {
    setupButtonsAndUi()
    setMapOpacity()
  }

  /**
   * Initialize bottom sheet by reading the [VM.prefsNav]
   */
  open fun lazyInitBottomSheet() {
    uiBottom = BottomSheetCvUI(this@CvMapActivity, VM.prefsNav.devMode)
  }

  protected open fun setupButtonsAndUi() {
    setupFloorSelector()
    setupGmap()

    // keep reacting to  settings updates
    lifecycleScope.launch(Dispatchers.IO) {
      app.dsCvNav.read.collect {
        LOG.V4(TAG, "CvMapAct: reacting for BottomSheet")
        lazyInitBottomSheet()
        // MERGE:PM: this must change
        LOG.E(TAG, "MERGE:PM: BottomSheet setup skipped")
        uiBottom.setup()
      }
    }

    checkInternet()
    ui.setupOnFloorSelectionClick()
  }

  private fun setupGmap() {
    ui = CvMapUi(VM, lifecycleScope,
            this@CvMapActivity,
            supportFragmentManager,
            overlays, floorSelector)
    wMap = GmapWrapper(applicationContext, lifecycleScope, ui)
    wMap.attach(VM, this, R.id.mapView)
  }

  private fun setupFloorSelector() {
    floorSelector = FloorSelector(applicationContext,
            findViewById(R.id.group_floorSelector),
            findViewById(R.id.textView_titleFloor),
            findViewById(R.id.button_selectedFloor),
            findViewById(R.id.button_floorUp),
            findViewById(R.id.button_floorDown))

    /** Updates on the wMap after a floor has changed */
    val callbackFloorselector = object: FloorSelector.Callback() {
      override fun before() {
        LOG.D4(TAG_METHOD, "remove user locations")
        // clear any overlays
        ui.removeHeatmap()
        wMap.removeUserLocations()
        // [Overlays.drawFloorplan] removes any previous floorplan
        // before drawing a new one so it doesn't need anything.
      }

      override fun after() { }
    }

    floorSelector.callback = callbackFloorselector
  }

  private fun setMapOpacity() {
    val view = findViewById<View>(id_gmap)
    val value =VM.prefsNav.mapAlpha.toInt()
    view.alpha=value/100f
  }


  /**
   * GMap is created by [CvMapActivity].
   * This is a callback that can be used sub-classes.
   */
  protected abstract fun onMapReadyCallback()

  /** Setup the Google Map:
   * - TODO finalize floorplans
   */
  override fun onMapReady(googleMap: GoogleMap) {
    wMap.setup(googleMap)
    onMapReadyCallback()
  }

  override fun onProcessImageFinished() {
    LOG.V3()
    uiBottom.refreshUi(lifecycleScope)
  }

  protected fun checkInternet() {
    if (!app.hasInternet()) {
      // TODO method that updates ui based on internet connectivity: gray out settings button
      Toast.makeText(applicationContext, "No internet.", Toast.LENGTH_LONG).show()
    }
  }

  fun observerDetections() {
    lifecycleScope.launch {
      VM.detectionsNAV.collectLatest {
        it.forEach { rec ->
          LOG.E(TAG, "Detection: ${rec.id} ${rec.title}")
        }
      }
    }
    // detectionsLocalization.coll
  }

}