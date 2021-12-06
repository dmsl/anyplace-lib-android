package cy.ac.ucy.cs.anyplace.lib.android.ui.components

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers.FloorsHelper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.fadeIn
import cy.ac.ucy.cs.anyplace.lib.android.extensions.fadeOut
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.buttonUtils
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.CvViewModelBase
import cy.ac.ucy.cs.anyplace.lib.models.Floor

/**
 * Encapsulating the Floor Selection UI component.
 */
class FloorSelector(
        private val ctx: Context,
        private val groupFloorSelector: Group,
        private val tvFloorTitle: TextView,
        /** this is not treated as a button. used a button just for the background color.. */
        private val btnSelectedFloor: Button,
        private val btnFloorUp: MaterialButton,
        private val btnFloorDown: MaterialButton) {

  fun updateFloorSelector(floor: Floor?, FH: FloorsHelper) {
    // TODO if it has floors, then fade in..
    if (groupFloorSelector.visibility != View.VISIBLE) groupFloorSelector.fadeIn()

    if (floor == null) {
      updateSelectionButton(btnFloorUp, ctx, false)
      updateSelectionButton(btnFloorUp, ctx, false)
    } else {
      btnSelectedFloor.text = floor.floorNumber
      tvFloorTitle.text = FH.spaceH.prettyFloor

      val isFirstFloor = floor.floorNumber==FH.getFirstFloor().floorNumber
      updateSelectionButton(btnFloorDown, ctx, !isFirstFloor)

      val isLastFloor = floor.floorNumber==FH.getLastFloor().floorNumber
      updateSelectionButton(btnFloorUp, ctx, !isLastFloor)
      LOG.D(TAG_METHOD, "floors: ${FH.getFirstFloor().floorNumber} - ${FH.getLastFloor().floorNumber}")
    }

    // TODO CHANGE FLOOR?
    // TODO CLEAR HEATMAPS...

  }

  private fun updateSelectionButton(btn: MaterialButton, ctx: Context, enable: Boolean) {
    if (enable) {
      btn.isClickable=true
      buttonUtils.changeMaterialButtonIcon(btn, ctx, R.drawable.arrow_up)
    } else {
      btn.isClickable= false
      buttonUtils.changeMaterialButtonIcon(btn, ctx, R.drawable.ic_arrow_up_disabled)
    }
  }

  fun onFloorUp(method: () -> Unit) { btnFloorUp.setOnClickListener { method() } }
  fun onFloorDown(method: () -> Unit) { btnFloorDown.setOnClickListener { method() } }
}