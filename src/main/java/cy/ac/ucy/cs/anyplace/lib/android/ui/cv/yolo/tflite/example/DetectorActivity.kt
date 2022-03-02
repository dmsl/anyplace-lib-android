package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.example

import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.BottomSheetCvMap
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.DetectorActivityBase
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.DetectorViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * EXAMPLE CLASS:
 * - a modernized version of matcheshunglc007
 * - it is as close as possible to the sources:
 *   - [DetectorActivity.java](https://github.com/hunglc007/tensorflow-yolov4-tflite/blob/master/android/app/src/main/java/org/tensorflow/lite/examples/detection/DetectorActivity.java)
 *   - Github Repo: [hunglc007/tensorflow-yolov4-tflite](https://github.com/hunglc007/tensorflow-yolov4-tflite)
 *
 * TO BE USED ONLY AS AN EXAMPLE.
 */
@AndroidEntryPoint
class DetectorActivity : DetectorActivityBase() {

  // PROVIDE TO BASE CLASS [CameraActivity]:
  override val layout_activity: Int get() = R.layout.tfe_od_activity_camera
  override val id_bottomsheet: Int get() = R.id.tfe_bottom_sheet_layout
  override val id_gesture_layout: Int get() = R.id.gesture_layout
  override val view_model_class: Class<DetectorViewModel> = DetectorViewModel::class.java

  private val showBottomSheet = true
  private val bottomSheet by lazy { BottomSheetCvMap(this@DetectorActivity, showBottomSheet) }

  // BottomSheet specific details (default ones)
  lateinit var frameValueTextView: TextView

  override fun postCreate() {
    super.postCreate()
    // VM = _vm as DetectorViewModel  // no need to use a VM here
    bottomSheet.setup()
  }

  override fun onProcessImageFinished() {
    LOG.V4()
    lifecycleScope.launch(Dispatchers.Main) {
      bottomSheet.refreshUi()
    }
  }

}