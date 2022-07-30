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

class BottomSheetCvLoggerUI(
  private val act: CvLoggerActivity,
  val VMlog: CvLoggerViewModel,
  val id_bottomsheet: Int,
  val id_btn_logging: Int,
  val visible: Boolean)
  : BottomSheetCvUI(act as DetectorActivityBase, visible) {

  // val llBottomSheet: ConstraintLayout by lazy {act.findViewById(id_bottomsheet) }

  // TODO replace  bu_TvTimeInfo by: tvTimeInfo
  // TODO replace bu_TvCropInfo: with tvCropInfo
  // TODO replace ivBottomSheetArrow with  ivArrowImg

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


  // TODO: in parent BottomSheetCvUI?
  // NAV COMMON shared between activities? (pre-merge)
  fun bindCvStats() {
    tvElapsedTime.text=VMlog.getElapsedSecondsStr()
    tvObjUnique.text=VMlog.statObjWindowUNQ.toString()
    tvCurWindow.text=VMlog.objOnMAP.size.toString()
    tvObjTotal.text=VMlog.statObjTotal.toString()
  }


  // TODO: in parent BottomSheetCvUI? override?!
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
      LOG.V4(TAG, "peek height: ${act.sheetBehavior.peekHeight}")
    }

    // TODO:PM get detectionModel and setup sizes
    // val model = VMb.detector.getDetectionModel()
    @SuppressLint("SetTextI18n")
    tvCropInfo.text = "<NAN>x<NAN>"
    // binding.bottomUi.cropInfo.text = "${model.inputSize}x${model.inputSize}"
  }
}