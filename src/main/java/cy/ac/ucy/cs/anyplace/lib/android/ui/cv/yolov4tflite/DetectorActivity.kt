package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite

import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.view.ViewTreeObserver
import com.google.android.material.bottomsheet.BottomSheetBehavior
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG

/**
 * This is the default DetectorActivity as it comes from:
 * - https://github.com/hunglc007/tensorflow-yolov4-tflite
 */



// LEFTHERE: USE GETTERS !!!
// LEFTHERE: USE GETTERS !!!
// LEFTHERE: USE GETTERS !!!
// override val desiredPreviewFrameSize: Size?
//   get() = DetectorActivityBase.DESIRED_PREVIEW_SIZE
class DetectorActivity : DetectorActivityBase() {

  // Must provide the below:
  override val layout_activity: Int get() = R.layout.tfe_od_activity_camera
  override val id_bottomsheet: Int get() = R.id.bottom_sheet_layout
  override val id_gesture_layout: Int get() = R.id.bottom_sheet_layout
  override val id_bottomsheet_arrow: Int get() = R.id.bottom_sheet_arrow

  override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
    LOG.D()
    super.onCreate(savedInstanceState, persistentState)
  }

  override fun setupBottomSheet() {
    sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout)
    val vto = gestureLayout.viewTreeObserver

    vto.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
              override fun onGlobalLayout() {
                gestureLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val height = gestureLayout.measuredHeight
                sheetBehavior.peekHeight = height/3
              }
            })

    sheetBehavior.isHideable = false
    sheetBehavior.setBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
              override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                  BottomSheetBehavior.STATE_EXPANDED -> {
                    bottomSheetArrowImageView.setImageResource(R.drawable.ic_icon_down)
                  }
                  BottomSheetBehavior.STATE_COLLAPSED -> {
                    bottomSheetArrowImageView.setImageResource(R.drawable.ic_icon_up)
                  }
                  BottomSheetBehavior.STATE_SETTLING -> bottomSheetArrowImageView.setImageResource(R.drawable.ic_icon_up)
                  else -> {}
                }
              }

              override fun onSlide(bottomSheet: View, slideOffset: Float) {
                LOG.D("Sliding...")
              }
            })

    frameValueTextView = findViewById(R.id.frame_info)
    cropValueTextView = findViewById(R.id.crop_info)
    inferenceTimeTextView = findViewById(R.id.inference_info)
  }

}