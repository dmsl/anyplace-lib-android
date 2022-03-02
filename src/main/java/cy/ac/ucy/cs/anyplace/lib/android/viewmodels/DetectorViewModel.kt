package cy.ac.ucy.cs.anyplace.lib.android.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import cy.ac.ucy.cs.anyplace.lib.android.cv.enums.YoloConstants
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
open class DetectorViewModel @Inject constructor(application: Application) :
        AndroidViewModel(application) {

  protected val assetReader by lazy { AssetReader(app) }

  internal lateinit var detector: Classifier

  @Deprecated("TODO SETTINGS")
  val model = YoloConstants.DETECTION_MODEL
}