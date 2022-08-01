package cy.ac.ucy.cs.anyplace.lib.android.ui.components

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.LevelWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.LevelsWrapper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.UtilUI
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Level
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Encapsulating all functionality of the Level Selection UI component.
 * Level is a floor or a deck
 *
 */
class LevelSelector(
        private val ctx: Context,
        private val scope: CoroutineScope,
        private val group: Group,
        private val tvLevelTitle: TextView,
        /** this is not treated as a button. used a button just for the background color.. */
        private val btnSelectedLevel: MaterialButton,
        private val btnLevelUp: MaterialButton,
        private val btnLevelDown: MaterialButton) {

  /** UI-Component: Level Selector */
  private val TG = "ui-lvlsel"

  abstract class Callback {
    /** Right after a floor is selected. For rendering new elements. */
    abstract fun before()
    /** Right before a floor is selected. For doing cleanups */
    abstract fun after()
  }

  private val utlUi by lazy { UtilUI(ctx, scope) }
  var callback : Callback ?= null

  fun show() = utlUi.fadeIn(group)
  fun hide() = utlUi.fadeOut(group)

  fun enable() {
    LOG.V2(TG, "enabling")

    if (!DBG.FLD) { show(); return }

    utlUi.changeBackgroundMaterial(btnSelectedLevel, R.color.colorPrimary)
    utlUi.alpha(btnSelectedLevel, 1f)
    utlUi.changeBackgroundDrawable(btnLevelUp, R.drawable.button_round_top_normal)
    utlUi.changeBackgroundDrawable(btnLevelDown, R.drawable.button_round_top_normal)

    utlUi.enable(btnSelectedLevel)
    utlUi.enable(btnLevelUp)
    utlUi.enable(btnLevelDown)
    utlUi.textColor(tvLevelTitle, R.color.colorPrimary)
    utlUi.alpha(tvLevelTitle, 1f)
  }

  fun disable() {
    LOG.V2(TG, "disabling")
    if (!DBG.FLD) { hide(); return }
    utlUi.changeBackgroundMaterial(btnSelectedLevel, R.color.darkGray)
    utlUi.alpha(btnSelectedLevel, 0.4f)
    utlUi.changeBackgroundDrawable(btnLevelUp, R.drawable.button_round_top_disabled)
    utlUi.changeBackgroundDrawable(btnLevelDown, R.drawable.button_round_top_disabled)

    utlUi.disable(btnSelectedLevel)
    utlUi.disable(btnLevelUp)
    utlUi.disable(btnLevelDown)
    utlUi.textColor(tvLevelTitle, R.color.darkGray)
    utlUi.alpha(tvLevelTitle, 0.3f)
  }

  fun updateFloorSelector(level: Level?, FW: LevelsWrapper) {
    scope.launch(Dispatchers.Main) {
      // if it has floors, then fade in..
      if (group.visibility != View.VISIBLE)
        utlUi.fadeIn(group)

      if (level == null) {
        updateSelectionButton(btnLevelUp, ctx, false)
        updateSelectionButton(btnLevelUp, ctx, false)
      } else {
        btnSelectedLevel.text = level.number
        tvLevelTitle.text = FW.spaceH.prettyLevel

        val isFirstLevel = level.number==FW.getFirstLevel().number
        updateSelectionButton(btnLevelDown, ctx, !isFirstLevel)

        val isLastLevel = level.number==FW.getLastLevel().number
        updateSelectionButton(btnLevelUp, ctx, !isLastLevel)
        LOG.V2(TAG_METHOD, "levels: ${FW.getFirstLevel().number} - ${FW.getLastLevel().number}")
      }
    }
  }

  private fun updateSelectionButton(btn: MaterialButton, ctx: Context, enable: Boolean) {
    // TODO:PMX use utlUi?
    if (enable) {
      btn.isClickable=true
      utlUi.changeMaterialIcon(btn, R.drawable.arrow_up)
    } else {
      btn.isClickable= false
      utlUi.changeMaterialIcon(btn, R.drawable.ic_arrow_up_disabled)
    }
  }

  fun onLevelUp(method: () -> Unit) { btnLevelUp.setOnClickListener { method() } }
  fun onLevelDown(method: () -> Unit) { btnLevelDown.setOnClickListener { method() } }

  private var levelChangeRequestTime : Long = 0
  private var isLazilyChangingLevel = false
  private val DELAY_LEVEL_CHANGE = 300

  /**
   * Wait some time, and then change floor
   */
  fun lazilyChangeLevel(VM: CvViewModel, scope: CoroutineScope) {
    val app  = VM.app
    if (app.wLevel == null) {
      LOG.E(TAG_METHOD, "Null floor")
      return
    }

    LOG.V2()
    if (levelChangeRequestTime == 0L) {
      levelChangeRequestTime = System.currentTimeMillis()
      loadLevel(VM, scope)
      return
    }

    levelChangeRequestTime = System.currentTimeMillis()

    scope.launch(Dispatchers.IO) {
      if (!isLazilyChangingLevel) {
        LOG.D4(TAG_METHOD, "Might change to level: ${app.wLevel!!.prettyLevelName()}")
        isLazilyChangingLevel = true
        do {
          val curTime = System.currentTimeMillis()
          val diff = curTime-levelChangeRequestTime
          LOG.D3(TAG_METHOD, "delay: $diff")
          delay(200)
        } while(diff < DELAY_LEVEL_CHANGE)

        LOG.V2(TG, "lazilyChangeFloor: to level: ${app.wLevel!!.prettyLevelName()} (after delay)")

        isLazilyChangingLevel = false

        // BUG: VM or FH has the wrong floor number?
        loadLevel(VM, scope)
      } else {
        LOG.D4(TAG_METHOD, "Skipping level: ${app.wLevel!!.prettyLevelName()}")
      }
    }
  }

  /**
   * Loads a floor into the UI
   * Reads a floorplan (from cache or remote) using the [VMB] and a [LevelWrapper]
   * Once it's read, then it is loaded it is posted on [VMB.floorplanFlow],
   * and through an observer it is loaded on the map.
   *
   * Must be called each time wee want to load a floor.
   */
  private fun loadLevel(VM: CvViewModel, scope: CoroutineScope) {
    val MT = ::loadLevel.name
    val app = VM.app
    
    LOG.D2(TG, MT)

    callback?.before()

    if (app.wLevel==null) {
      LOG.W(TG, "$MT: level is null.")
      return
    }

    val WF = app.wLevel!!
    LOG.D2(TG, "$MT: app space: ${app.wSpace.obj.name} level: ${WF.obj.number} ${WF.prettyLevelName()}")
    LOG.D2(TG, "$MT: ${WF.wSpace.obj.name} level: ${WF.obj.number} ${WF.prettyLevelName()}")

    scope.launch(Dispatchers.IO) {
      if (WF.hasLevelplanCached()) {
        LOG.D(TG, "$MT: local")
        VM.nwLevelPlan.readFromCache(VM, WF)
      } else {
        LOG.D(TG, "$MT: remote")
        VM.nwLevelPlan.getLevelplan(WF)
      }
    }
    callback?.after()
  }
}