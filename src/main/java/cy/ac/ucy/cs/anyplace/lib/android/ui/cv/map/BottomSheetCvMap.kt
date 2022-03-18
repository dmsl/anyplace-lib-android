package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.google.android.material.bottomsheet.BottomSheetBehavior
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.DetectorActivityBase

class BottomSheetCvMap(private val act: DetectorActivityBase,
                       private val showBottomSheet: Boolean = false) {

  lateinit var frameValueTextView: TextView
  lateinit var cropValueTextView: TextView
  lateinit var inferenceTimeTextView: TextView
  lateinit var bottomSheetArrowImageView: ImageView

  fun setup() {
    if (!showBottomSheet) act.hideBottomSheet()

    bottomSheetArrowImageView = act.findViewById(R.id.bottom_sheet_arrow)
    act.sheetBehavior = BottomSheetBehavior.from(act.bottomSheetLayout)
    val vto = act.gestureLayout.viewTreeObserver

    vto.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
              override fun onGlobalLayout() {
                act.gestureLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val height = act.gestureLayout.measuredHeight
                act.sheetBehavior.peekHeight = height/10
              }
            })

    act.sheetBehavior.isHideable = false

    frameValueTextView = act.findViewById(R.id.frame_info)
    cropValueTextView = act.findViewById(R.id.crop_info)
    inferenceTimeTextView = act.findViewById(R.id.time_info)

    setupStatechanges(bottomSheetArrowImageView, R.drawable.ic_icon_down, R.drawable.ic_icon_up)
  }

  /**
   * Opening/Closing UI changes by the bottomsheet
   */
  fun setupStatechanges(iv: ImageView, @DrawableRes icDown: Int, @DrawableRes icUp: Int) {
    act.sheetBehavior.setBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
              override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                  BottomSheetBehavior.STATE_EXPANDED -> { iv.setImageResource(icDown) }
                  BottomSheetBehavior.STATE_COLLAPSED -> { iv.setImageResource(icUp) }
                  BottomSheetBehavior.STATE_SETTLING -> iv.setImageResource(icUp)
                  else -> {}
                }
              }
              override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            })
  }

  @SuppressLint("SetTextI18n")
  fun refreshUi() {
    if (!showBottomSheet) return

    frameValueTextView.text = "${act.previewWidth}x${act.previewHeight}"
    val w = act.cropCopyBitmap.width
    val h = act.cropCopyBitmap.height
    cropValueTextView.text = "${w}x${h}"
    inferenceTimeTextView.text =  "${act.lastProcessingTimeMs}ms"
  }
}