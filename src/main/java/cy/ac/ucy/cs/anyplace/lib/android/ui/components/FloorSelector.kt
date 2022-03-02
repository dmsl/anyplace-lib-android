package cy.ac.ucy.cs.anyplace.lib.android.ui.components

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers.FloorHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers.FloorsHelper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.fadeIn
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.FloorplanLoader
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.buttonUtils
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.CvMapViewModel
import cy.ac.ucy.cs.anyplace.lib.models.Floor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

  private val fpLoader by lazy { FloorplanLoader() }

  fun updateFloorSelector(floor: Floor?, FH: FloorsHelper) {
   if (true)  return
    // LEFTHERE: crashes
    // LEFTHERE: crashes
    // LEFTHERE: crashes

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

  private var floorChangeRequestTime : Long = 0
  private var isLazilyChangingFloor = false
  private val DELAY_CHANGE_FLOOR = 300

  /**
   * Wait some time, and then change floor
   */
  fun lazilyChangeFloor(VM: CvMapViewModel, scope: CoroutineScope) {
    if (VM.floorH == null) {
      LOG.E(TAG_METHOD, "Null floor")
      return
    }

    LOG.W()
    val FH= VM.floorH!!
    if (floorChangeRequestTime == 0L) {
      floorChangeRequestTime = System.currentTimeMillis()
      loadFloor(VM, FH, scope)
      return
    }

    floorChangeRequestTime = System.currentTimeMillis()
    LOG.W(TAG_METHOD, "isChanging: $isLazilyChangingFloor")

    scope.launch(Dispatchers.IO) {
      if (!isLazilyChangingFloor) {
        LOG.W(LOG.TAG, "will change to floor: ${FH.prettyFloorName()}")
        isLazilyChangingFloor = true
        do {
          val curTime = System.currentTimeMillis()
          val diff = curTime-floorChangeRequestTime
          LOG.D(LOG.TAG, "delay: $diff")
          delay(100)
        } while(diff < DELAY_CHANGE_FLOOR)

        LOG.W(LOG.TAG, "changing to floor: ${FH.prettyFloorName()} (after delay)")

        loadFloor(VM, FH, scope)
        isLazilyChangingFloor = false
      } else {
        LOG.E(LOG.TAG, "skipping floor: ${FH.prettyFloorName()}")
      }
    }
  }

  /**
   * Loads a floor into the UI
   * Reads a floorplan (from cache or remote) using the [VMB] and a [FloorHelper]
   * Once it's read, then it is loaded it is posted on [VMB.floorplanFlow],
   * and through an observer it is loaded on the map.
   *
   * Must be called each time wee want to load a floor.
   */
  private fun loadFloor(VM: CvMapViewModel, FH: FloorHelper, scope: CoroutineScope) {
    LOG.W(TAG_METHOD, FH.prettyFloorName())
    scope.launch {
      if (FH.hasFloorplanCached()) {
        fpLoader.readFromCache(VM, FH)
      } else {
        LOG.D2(TAG, "readFloorplan: remote")
        VM.getFloorplanFromRemote(FH)
      }
    }
  }

}