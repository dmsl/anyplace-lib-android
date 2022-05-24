package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import cy.ac.ucy.cs.anyplace.lib.android.cv.enums.DetectionModel
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.CvDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.CvNavDataStore
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.Classifier
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.DetectorActivityBase
import cy.ac.ucy.cs.anyplace.lib.android.utils.demo.AssetReader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


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
        val dsCvNav: CvNavDataStore,
) : AndroidViewModel(application) {

  protected val assetReader by lazy { AssetReader(app) }

  lateinit var detector: Classifier

  var modelLoaded = false
  lateinit var model: DetectionModel

  fun setModel(modelName: String) {
    model = getModel(modelName)
    modelLoaded = true
  }

  private fun getModel(modelName: String) : DetectionModel {
    return when (modelName.lowercase()) {
      "coco" -> DetectionModel.COCO
      "lashco" -> DetectionModel.LASHCO
      "ucyco" -> DetectionModel.UCYCO
      else -> { DetectionModel.COCO }
    }
  }

}