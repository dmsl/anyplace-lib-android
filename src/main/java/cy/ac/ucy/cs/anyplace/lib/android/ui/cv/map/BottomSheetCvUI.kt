package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.ACT_NAME_NAV
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.ACT_NAME_SMAS
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.CvMapActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.DetectorActivityBase
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.UtilUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class BottomSheetCvUI(private val act: DetectorActivityBase,
                           val bottomSheetEnabled: Boolean = false) {

  val tvFrameInfo : TextView by lazy { act.findViewById(R.id.frame_info) }
  val tvCropInfo: TextView by lazy { act.findViewById(R.id.crop_info) }
  val tvTimeInfo: TextView by lazy { act.findViewById(R.id.time_info) }
  val layoutInternal: LinearLayout by lazy {act.findViewById(R.id.bottom_sheet_internal) }
  val ivArrowImg: ImageView by lazy {act.findViewById(R.id.bottom_sheet_arrow) }

  val utlUi by lazy { UtilUI(act.applicationContext, act.lifecycleScope) }

  fun setup() {
    LOG.W(TAG, " BSheet CV setup")
    if (!bottomSheetEnabled) hideBottomSheet()

    // CLR if ok
    // defaultSetup()
    // setupStatechanges(ivArrowImg, R.drawable.ic_icon_down, R.drawable.ic_icon_up)

    act.sheetBehavior = BottomSheetBehavior.from(act.bottomSheetLayout)
    act.sheetBehavior.isHideable = false

    val callback = BottomSheetCallback(ivArrowImg)
    act.sheetBehavior.addBottomSheetCallback(callback)

    setupSpecialize()
  }

  open fun setupSpecialize() {

    if (act is CvMapActivity) {
      val cvAct = act as CvMapActivity
      // no bottom sheet on smas/nav
      if (cvAct.actName==ACT_NAME_SMAS || cvAct.actName==ACT_NAME_NAV) return
    }

    // Setup peak height
    val vto = act.gestureLayout.viewTreeObserver

    // workaround?
    if(!act.gestureLayout.viewTreeObserver.isAlive || !vto.isAlive) {
      // CHECK: BUG?
      LOG.E(TAG, "FAILED TO SETUP BOTTOM SHEET: was not alive")
      return
    }

    // getViewTreeObserver
    vto.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
              override fun onGlobalLayout() {
                act.gestureLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val height = act.gestureLayout.measuredHeight
                act.sheetBehavior.peekHeight = height/8
              }
            })
  }

  open fun hideBottomSheet() {
    act.hideBottomSheet()
  }

  open fun showBottomSheet() {
    act.showBottomSheet()
  }


  @SuppressLint("SetTextI18n")
  fun refreshUi(scope: LifecycleCoroutineScope) {
    if (!bottomSheetEnabled) return

    scope.launch(Dispatchers.Main) {
      tvFrameInfo.text = "${act.previewWidth}x${act.previewHeight}"
      val w = act.cropCopyBitmap.width
      val h = act.cropCopyBitmap.height
      tvCropInfo.text = "${w}x${h}"
      tvTimeInfo.text =  "${act.lastProcessingTimeMs}ms"
    }
  }

  /**
   * Update arrow when interacting with bottom sheet
   */
  class BottomSheetCallback(
          private val ivArrow: ImageView) : BottomSheetBehavior.BottomSheetCallback() {
    override fun onStateChanged(bottomSheet: View, newState: Int) {
      when (newState) {
        BottomSheetBehavior.STATE_HIDDEN -> { }
        BottomSheetBehavior.STATE_EXPANDED -> { ivArrow.setImageResource(R.drawable.ic_icon_down) }
        BottomSheetBehavior.STATE_COLLAPSED -> { ivArrow.setImageResource(R.drawable.ic_icon_up) }
        BottomSheetBehavior.STATE_DRAGGING -> { }
        BottomSheetBehavior.STATE_SETTLING -> { ivArrow.setImageResource(R.drawable.ic_icon_up) }
        BottomSheetBehavior.STATE_HALF_EXPANDED -> { }
      }
    }
    override fun onSlide(bottomSheet: View, slideOffset: Float) {}
  }

}