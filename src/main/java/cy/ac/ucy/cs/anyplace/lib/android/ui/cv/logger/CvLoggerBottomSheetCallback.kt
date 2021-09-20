package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger

import android.view.View
import android.widget.ImageView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import cy.ac.ucy.cs.anyplace.lib.R

class CvLoggerBottomSheetCallback(
  private val ivArrow: ImageView
) : BottomSheetBehavior.BottomSheetCallback() {

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