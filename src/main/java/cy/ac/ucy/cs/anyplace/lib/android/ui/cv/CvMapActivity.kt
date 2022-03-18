package cy.ac.ucy.cs.anyplace.lib.android.ui.cv

import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.store.CvPrefs
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.maps.Overlays
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.BottomSheetCvMap
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.FloorSelector
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.GmapHandler
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.CvMapUi
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.DetectorActivityBase
import cy.ac.ucy.cs.anyplace.lib.android.utils.demo.AssetReader
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.CvMapViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.DetectorViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * It uses:
 * - uses Yolov4 TFLite
 * - Settings
 * - Google Maps TODO
 *   - cache mechanism + floor changing TODO
 *   - floor selector? TODO
 *   - LEFTHERE:
 *     - 1. put google maps
 *     - 2. anything else?
 *     - 3. make this open?! DONE
 *        - to pass google maps? DONE
 *   - reuse functionality using Helper UI classes/objects?
 *  - ViewModel for this?? DONE
 */
@AndroidEntryPoint
abstract class CvMapActivity : DetectorActivityBase(), OnMapReadyCallback {
  companion object {
    // const val CAMERA_REQUEST_CODE: Int = 1
    // const val CAMERA_ASPECT_RATIO: Int = AspectRatio.RATIO_4_3 // AspectRatio.RATIO_16_9
    const val OPACITY_MAP_LOGGING = 0f
    const val ANIMATION_DELAY : Long = 100
  }

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
  // protected val gmap by lazy { GmapHandler(applicationContext) }
  protected lateinit var mapH: GmapHandler
  protected val overlays by lazy { Overlays(applicationContext) }
  protected val assetReader by lazy { AssetReader(applicationContext) }
  protected lateinit var bottomSheet : BottomSheetCvMap
  // protected val bottomSheet by lazy { BottomSheetCvMap(this@CvMapActivity) }

  // UI
  // protected lateinit var gmap: GoogleMap
  //// COMPONENTS
  protected lateinit var floorSelector: FloorSelector
  protected lateinit var UI: CvMapUi

  override fun postCreate() {
    super.postCreate()
    VM = _vm as CvMapViewModel
    LOG.D2(TAG_METHOD, "ViewModel: VM currentTime: ${VM.currentTime}")
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
    lifecycleScope.launch {
      LOG.D()
      cvDataStoreDS.read.first { prefs ->
        VM.prefsCV= prefs
        onCvPrefsLoaded(prefs)
        true
      }

      cvNavDS.read.first { prefs ->
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
      cvDataStoreDS.setReloadCvMaps(false)
    } else {
      LOG.D(TAG_METHOD, "not reloading (cvmap or caches)")
    }
  }

  private fun onNavPrefsLoaded() {
    setupButtonsAndUi()
    setMapOpacity()
  }

  protected open fun setupButtonsAndUi() {
    floorSelector = FloorSelector(applicationContext,
            findViewById(R.id.group_floorSelector),
            findViewById(R.id.textView_titleFloor),
            findViewById(R.id.button_selectedFloor),
            findViewById(R.id.button_floorUp),
            findViewById(R.id.button_floorDown))

    UI = CvMapUi(VM, lifecycleScope,
            this@CvMapActivity,
            supportFragmentManager,
            overlays, floorSelector)
    mapH = GmapHandler(applicationContext, lifecycleScope, UI)
    mapH.attach(VM, this, R.id.mapView)

    /** Updates on the mapH after a floor has changed */
    val floorSelectorCallback = object: FloorSelector.Callback() {
      override fun before() {
        LOG.D4(TAG_METHOD, "remove user locations")
        // clear any overlays
        UI.removeHeatmap()
        mapH.removeUserLocations()

        // [Overlays.drawFloorplan] removes any previous floorplan
        // before drawing a new one so it doesn't need anything.
      }

      override fun after() {
      }
    }

    floorSelector.callback = floorSelectorCallback

    bottomSheet = BottomSheetCvMap(this@CvMapActivity, VM.prefsNav.devMode)

    // keep reacting to  settings updates
    lifecycleScope.launch {
      app.cvNavDS.read.collect {
        bottomSheet.setup()
      }
    }

    checkInternet()
    UI.setupOnFloorSelectionClick()
  }

  private fun setMapOpacity() {
    val view = findViewById<View>(id_gmap)
    val value =VM.prefsNav.mapAlpha.toInt()
    view.alpha=value/100f
  }


  /** Setup the Google Map:
   * - TODO finalize floorplans
   */
  override fun onMapReady(googleMap: GoogleMap) {
    mapH.setup(googleMap)
  }

  override fun onProcessImageFinished() {
    LOG.V3()
    lifecycleScope.launch(Dispatchers.Main) {
      bottomSheet.refreshUi()
    }
  }

  protected fun checkInternet() {
    if (!app.hasInternet()) {
      // TODO method that updates ui based on internet connectivity: gray out settings button
      Toast.makeText(applicationContext, "No internet connection.", Toast.LENGTH_LONG).show()
    }
  }

}