package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger

import android.annotation.SuppressLint
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.lifecycle.lifecycleScope
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.UiLoggerTimer
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.UiLoggingBtn
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.BottomSheetCvUI
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.DetectorActivityBase
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvLoggerViewModel

/**
 * BottomSheet for the logger
 *
 * NOTE: this used to display more information, like:
 * - how many objects in a window, total detections, inference time
 * - it was modifying these components: tvTimeInfo, tvCropInfo, etc..
 * - Now this functionality is not incorporated..
 *   - could be done w/ view binding (once the relevant data are ready from the CV Engine
 *   - I think [onInferenceRan] was called at a relevant place to bind these stats..
 *
 *   Extends the base [BottomSheetCvUI]
 */
class BottomSheetCvLoggerUI(
  private val act: CvLoggerActivity,
  val VMlog: CvLoggerViewModel,
  val id_bottomsheet: Int,
  val id_btn_logging: Int,
  val visible: Boolean)
  : BottomSheetCvUI(act as DetectorActivityBase, visible) {
  private val TG = "ui-cv-logger-bottomsheet"

  private val id_btn_timer = R.id.button_cameraTimer
  private val id_progressBar_timer = R.id.progressBar_timer
  private val id_btn_clearObjs = R.id.button_clearObjects
  val logging by lazy { UiLoggingBtn(act, VMlog, act.lifecycleScope,
          VMlog.ui,
          VMlog.uiLog,
          id_btn_logging) }
  val timer by lazy { UiLoggerTimer(act, VMlog, act.lifecycleScope, VMlog.ui, VMlog.uiLog,
          id_btn_timer, id_progressBar_timer, id_btn_clearObjs) }

  val tvWindowObjectsAll : TextView by lazy { act.findViewById(R.id.tv_windowObjectsAll) }
  val groupTutorial : Group by lazy { act.findViewById(R.id.group_tutorial) }

  val llBottomSheetInternal: LinearLayout  by lazy { act.findViewById(R.id.bottom_sheet_internal) }
  val ivBottomSheetArrow: ImageView by lazy { act.findViewById(R.id.bottom_sheet_arrow) }
  val llGestureLayout: ConstraintLayout by lazy {act.findViewById(R.id.gesture_layout) }
  val groupDevSettings: Group by lazy { act.findViewById(R.id.group_devSettings) }

  val tvElapsedTime: TextView by lazy { act.findViewById(R.id.tv_elapsedTime) }
  val tvObjUnique: TextView by lazy { act.findViewById(R.id.tv_windowObjectsUnique) }
  val tvCurWindow: TextView by lazy { act.findViewById(R.id.tv_currentWindow) }
  val tvObjTotal: TextView by lazy { act.findViewById(R.id.tv_totalObjects) }

  fun bindCvStats() {
    tvElapsedTime.text=VMlog.getElapsedSecondsStr()
    tvObjUnique.text=VMlog.statObjWindowUNQ.toString()
    tvCurWindow.text=VMlog.objOnMAP.size.toString()
    tvObjTotal.text=VMlog.statObjTotal.toString()
  }

  override fun hideBottomSheet() {
    // super.hideBottomSheet() CLR ?
    utlUi.gone(ivArrowImg)
    utlUi.invisible(layoutInternal)
    act.sheetBehavior.isDraggable=false
  }

  override fun showBottomSheet() {
    super.showBottomSheet()

    if (bottomSheetEnabled) {
      utlUi.visible(ivArrowImg)
      utlUi.visible(layoutInternal)
      act.sheetBehavior.isDraggable=true
    }
  }

  override fun setupSpecialize() {
    // Peak height setup
    llGestureLayout.viewTreeObserver.addOnGlobalLayoutListener {
      act.sheetBehavior.peekHeight = ivBottomSheetArrow.bottom + 60
      LOG.V4(TG, "peek height: ${act.sheetBehavior.peekHeight}")
    }

    @SuppressLint("SetTextI18n")
    tvCropInfo.text = "<NAN>x<NAN>"
    // binding.bottomUi.cropInfo.text = "${model.inputSize}x${model.inputSize}"
  }
}