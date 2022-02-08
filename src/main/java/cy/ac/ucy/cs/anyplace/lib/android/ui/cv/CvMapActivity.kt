package cy.ac.ucy.cs.anyplace.lib.android.ui.cv

import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.maps.Overlays
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.BottomSheetCvMap
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.FloorSelector
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.GmapHandler
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.gnk.UiActivityCvBase
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite.DetectorActivityBase
import cy.ac.ucy.cs.anyplace.lib.android.utils.demo.AssetReader
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.CvMapViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.DetectorViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * It uses:
 * - uses Yolov4 TFLite
 * - Settings
 * - Google Maps TODO
 *   - floor selector?
 *   - LEFTHERE:
 *     - 1. put google maps
 *     - 2. anything else?
 *     - 3. make this open?!
 *        - to pass google maps?
 *     - or no?
 *       - reuse functionality using Helper UI classes/objects?
 *  - TODO ViewModel for this??
 */
@AndroidEntryPoint
open class CvMapActivity : DetectorActivityBase(), OnMapReadyCallback {
  companion object {
    // const val CAMERA_REQUEST_CODE: Int = 1
    // const val CAMERA_ASPECT_RATIO: Int = AspectRatio.RATIO_4_3 // AspectRatio.RATIO_16_9
    const val OPACITY_MAP_LOGGING = 0f
    const val ANIMATION_DELAY : Long = 100
  }

  // PROVIDE TO BASE CLASS [CameraActivity]:
  override val layout_activity: Int get() = R.layout.example_cvmap
  override val id_bottomsheet: Int get() = R.id.bottom_sheet_cvmap
  override val id_gesture_layout: Int get() = R.id.gesture_layout

  override val view_model_class: Class<DetectorViewModel> =
          CvMapViewModel::class.java as Class<DetectorViewModel>

  // VIEW MODELS
  /** extends [DetectorViewModel] */
  private lateinit var VM: CvMapViewModel

  // UTILITY OBJECTS
  protected val gmap by lazy { GmapHandler(applicationContext) }
  protected val overlays by lazy { Overlays(applicationContext) }
  protected val assetReader by lazy { AssetReader(applicationContext) }
  protected val bottomSheet by lazy { BottomSheetCvMap(this@CvMapActivity) }

  // UI
  // protected lateinit var gmap: GoogleMap
  //// COMPONENTS
  protected lateinit var floorSelector: FloorSelector
  protected lateinit var UIB: UiActivityCvBase

  override fun postCreate() {
    super.postCreate()
    VM = _vm as CvMapViewModel
    LOG.D2(TAG_METHOD, "ViewModel: VM currentTime: ${VM.currentTime}")

    bottomSheet.setup()
    gmap.attach(this, R.id.mapView)
  }

  override fun onMapReady(googleMap: GoogleMap) {
    gmap.setup(googleMap)
  }

  override fun onProcessImageFinished() {
    LOG.V3()
    lifecycleScope.launch(Dispatchers.Main) {
      bottomSheet.refreshUi()
    }
  }

}