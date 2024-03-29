package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.DetectionModel
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.CvDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.CvMapDataStore
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.Classifier
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.DetectorActivityBase
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.customview.OverlayView
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.tracking.MultiBoxTracker
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.demo.AssetReader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class DetectorStatus {
  enabled,
  disabled,
}

/**
 * [DetectorViewModel] contains only whatever is related to
 * - object detection (YOLOV4-TFLITE):
 *    - YOLO-related setup
 * - and Cvmap Activity
 *
 *  Initialized by [DetectorActivityBase.onPreviewSizeChosen]
 */
@HiltViewModel
open class DetectorViewModel @Inject constructor(
        application: Application,
        val dsCv: CvDataStore,
        val dsCvMap: CvMapDataStore,
) : AndroidViewModel(application) {
  private val TG = "vm-detector"
  val assetReader by lazy { AssetReader(app) }

  lateinit var detector: Classifier
  lateinit var tracker: MultiBoxTracker
  lateinit var trackingOverlay: OverlayView

  private val status = MutableStateFlow(DetectorStatus.disabled)
  var initedDetector = false
  lateinit var model: DetectionModel

  fun setModelName(modelName: String) {
    model = getModel(modelName)
  }

  fun setDetectorLoaded() { initedDetector = true }
  fun unsetDetectorLoaded() { initedDetector = false }

  /**
   * Apart from the first initialization,
   * it helps when reloading the detector (e.g. changing CV Model)
   */
  @Deprecated("Is there a better way for this?")
  suspend fun waitForDetector() {
    val MT = ::waitForDetector.name
    while (!initedDetector) {
      LOG.V(TG, "$MT..")
      delay(200)
    }
  }

  private fun getModel(modelName: String) : DetectionModel {
    return when (modelName.lowercase()) {
      "coco" -> DetectionModel.COCO
      "lashco" -> DetectionModel.LASHCO
      "ucyco" -> DetectionModel.UCYCO
      else -> { DetectionModel.COCO }
    }
  }

  fun disableCvDetection() {
   status.update { DetectorStatus.disabled }
  }

  fun isDetecting() = status.value == DetectorStatus.enabled

  fun enableCvDetection() {
    status.update { DetectorStatus.enabled }
  }

}