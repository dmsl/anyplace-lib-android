package cy.ac.ucy.cs.anyplace.lib.android.ui.components

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.FloorWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.FloorsWrapper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.FloorplanLoader
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.UtilUI
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Floor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Encapsulating all functionality of the Floor Selection UI component.
 */
class FloorSelector(
        private val ctx: Context,
        private val scope: CoroutineScope,
        private val group: Group,
        private val tvFloorTitle: TextView,
        /** this is not treated as a button. used a button just for the background color.. */
        private val btnSelectedFloor: MaterialButton,
        private val btnFloorUp: MaterialButton,
        private val btnFloorDown: MaterialButton,
) {

  abstract class Callback {
    /** Right after a floor is selected. For rendering new elements. */
    abstract fun before()
    /** Right before a floor is selected. For doing cleanups */
    abstract fun after()
  }

  private val utlUi by lazy { UtilUI(ctx, scope) }

  private val fpLoader by lazy { FloorplanLoader() }
  var callback : Callback ?= null

  fun show() = utlUi.fadeIn(group)
  fun hide() = utlUi.fadeOut(group)

  fun enable() {
    LOG.V2(TAG, "Enabling FloorSelector")

    if (!DBG.FLD) { show(); return }

    utlUi.changeBackgroundMaterial(btnSelectedFloor, R.color.colorPrimary)
    utlUi.alpha(btnSelectedFloor, 1f)
    utlUi.changeBackgroundDrawable(btnFloorUp, R.drawable.button_round_top_normal)
    utlUi.changeBackgroundDrawable(btnFloorDown, R.drawable.button_round_top_normal)

    utlUi.enable(btnSelectedFloor)
    utlUi.enable(btnFloorUp)
    utlUi.enable(btnFloorDown)
    utlUi.textColor(tvFloorTitle, R.color.colorPrimary)
    utlUi.alpha(tvFloorTitle, 1f)
  }

  fun disable() {
    LOG.V2(TAG, "Disabling FloorSelector")
    if (!DBG.FLD) { hide(); return }
    utlUi.changeBackgroundMaterial(btnSelectedFloor, R.color.darkGray)
    utlUi.alpha(btnSelectedFloor, 0.4f)
    utlUi.changeBackgroundDrawable(btnFloorUp, R.drawable.button_round_top_disabled)
    utlUi.changeBackgroundDrawable(btnFloorDown, R.drawable.button_round_top_disabled)

    utlUi.disable(btnSelectedFloor)
    utlUi.disable(btnFloorUp)
    utlUi.disable(btnFloorDown)
    utlUi.textColor(tvFloorTitle, R.color.darkGray)
    utlUi.alpha(tvFloorTitle, 0.3f)
  }

  fun updateFloorSelector(floor: Floor?, FW: FloorsWrapper) {
    // if it has floors, then fade in..
    if (group.visibility != View.VISIBLE)
      utlUi.fadeIn(group)

    if (floor == null) {
      updateSelectionButton(btnFloorUp, ctx, false)
      updateSelectionButton(btnFloorUp, ctx, false)
    } else {
      btnSelectedFloor.text = floor.floorNumber
      tvFloorTitle.text = FW.spaceH.prettyFloor

      val isFirstFloor = floor.floorNumber==FW.getFirstFloor().floorNumber
      updateSelectionButton(btnFloorDown, ctx, !isFirstFloor)

      val isLastFloor = floor.floorNumber==FW.getLastFloor().floorNumber
      updateSelectionButton(btnFloorUp, ctx, !isLastFloor)
      LOG.V2(TAG_METHOD, "floors: ${FW.getFirstFloor().floorNumber} - ${FW.getLastFloor().floorNumber}")
    }
  }

  private fun updateSelectionButton(btn: MaterialButton, ctx: Context, enable: Boolean) {
    if (enable) {
      btn.isClickable=true
      utlUi.changeMaterialIcon(btn, R.drawable.arrow_up)
    } else {
      btn.isClickable= false
      utlUi.changeMaterialIcon(btn, R.drawable.ic_arrow_up_disabled)
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
  fun lazilyChangeFloor(VM: CvViewModel, scope: CoroutineScope) {
    val app  = VM.app
    if (app.wFloor == null) {
      LOG.E(TAG_METHOD, "Null floor")
      return
    }

    LOG.V2()
    if (floorChangeRequestTime == 0L) {
      floorChangeRequestTime = System.currentTimeMillis()
      loadFloor(VM, scope)
      return
    }

    floorChangeRequestTime = System.currentTimeMillis()

    scope.launch(Dispatchers.IO) {
      if (!isLazilyChangingFloor) {
        LOG.D4(TAG_METHOD, "Might change to floor: ${app.wFloor!!.prettyFloorName()}")
        isLazilyChangingFloor = true
        do {
          val curTime = System.currentTimeMillis()
          val diff = curTime-floorChangeRequestTime
          LOG.D3(TAG_METHOD, "delay: $diff")
          delay(200)
        } while(diff < DELAY_CHANGE_FLOOR)

        LOG.V2(TAG, "lazilyChangeFloor: to floor: ${app.wFloor!!.prettyFloorName()} (after delay)")

        isLazilyChangingFloor = false

        // BUG: VM or FH has the wrong floor number?
        loadFloor(VM, scope)
      } else {
        LOG.D4(TAG_METHOD, "Skipping floor: ${app.wFloor!!.prettyFloorName()}")
      }
    }
  }

  /**
   * Loads a floor into the UI
   * Reads a floorplan (from cache or remote) using the [VMB] and a [FloorWrapper]
   * Once it's read, then it is loaded it is posted on [VMB.floorplanFlow],
   * and through an observer it is loaded on the map.
   *
   * Must be called each time wee want to load a floor.
   */
  private fun loadFloor(VM: CvViewModel, scope: CoroutineScope) {
    callback?.before()

    val app = VM.app

    if (app.wFloor==null) {
      LOG.E(TAG, "$METHOD: floor is null.")
      return
    }

    val FW = app.wFloor!!
    LOG.D2(TAG, "loadFloor: ${FW.prettyFloorName()}")
    scope.launch(Dispatchers.IO) {
      if (FW.hasFloorplanCached()) {
        LOG.W(TAG, "loadFloor: local")
        fpLoader.readFromCache(VM, FW)
      } else {
        LOG.D2(TAG, "loadFloor: remote")
        VM.getFloorplanFromRemote(FW)
      }
    }

    callback?.after()
  }

}