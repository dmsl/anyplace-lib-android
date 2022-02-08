package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite.example

import android.annotation.SuppressLint
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.BottomSheetCvMap
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite.DetectorActivityBase
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.CvMapViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.DetectorViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * TO BE USED ONLY AS AN EXAMPLE.
 *
 * This is the default DetectorActivity as it comes from:
 * - https://github.com/hunglc007/tensorflow-yolov4-tflite
 */
@AndroidEntryPoint
class DetectorActivity : DetectorActivityBase() {

  // PROVIDE TO BASE CLASS [CameraActivity]:
  override val layout_activity: Int get() = R.layout.tfe_od_activity_camera
  override val id_bottomsheet: Int get() = R.id.bottom_sheet_layout
  override val id_gesture_layout: Int get() = R.id.gesture_layout
  override val view_model_class: Class<DetectorViewModel> = DetectorViewModel::class.java

  private val bottomSheet by lazy { BottomSheetCvMap(this@DetectorActivity) }

  // BottomSheet specific details (default ones)
  lateinit var frameValueTextView: TextView

  override fun postCreate() {
    super.postCreate()
    // VM = _vm as DetectorViewModel // not used in here
    bottomSheet.setup()
  }

  override fun onProcessImageFinished() {
    LOG.V4()
    lifecycleScope.launch(Dispatchers.Main) {
      bottomSheet.refreshUi()
    }
  }

}