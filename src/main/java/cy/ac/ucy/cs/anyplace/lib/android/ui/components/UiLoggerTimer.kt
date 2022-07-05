package cy.ac.ucy.cs.anyplace.lib.android.ui.components

import android.view.View
import android.widget.ProgressBar
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerUI
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.CvUI
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.UtilUI
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvLoggerViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.TimerAnimation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Circular timer used in the logger
 * - circular prorgess bar
 * - button that shows remaining seconds
 * - clear window button
 */
class UiLoggerTimer(
        private val act: CvLoggerActivity,
        private val VM: CvLoggerViewModel,
        val scope: CoroutineScope,
        private val ui: CvUI,
        private val uiLog: CvLoggerUI,
        private val id_btn_timer: Int,
        private val id_progressBar: Int,
        private val id_btn_clearObjects: Int,
) {

  private val utlUi by lazy { UtilUI(act, scope) }

  /** timer button */
  val btnTimer: MaterialButton by lazy { act.findViewById(id_btn_timer) }
  val btnClearObj: MaterialButton by lazy { act.findViewById(id_btn_clearObjects) }
  val progressBar: ProgressBar by lazy { act.findViewById(id_progressBar) }

  fun prepareForStart() {
    utlUi.removeMaterialIcon(uiLog.bottom.timer.btnTimer)
    utlUi.changeBackgroundMaterial(uiLog.bottom.timer.btnTimer, R.color.redDark)
    utlUi.fadeIn(uiLog.bottom.timer.btnTimer)
  }


  /**
   * Initiate a circular progress bar animation, inside a coroutine for
   * smooth (and independent from other threads) updates.
   * It progresses according to the window time
   */
  private fun startAnimation(windowSecs: Int) {
    // showing timer button but not yet the progress bar
    if (btnTimer.visibility == View.VISIBLE &&
            progressBar.visibility != View.VISIBLE) {
      val delayMs = (windowSecs*1000/100).toLong()
      scope.launch(Dispatchers.Main) {
        var progress = 0
        progressBar.progress=progress
        progressBar.visibility = View.VISIBLE
        while(progress < 100) {
          when (VM.circleTimerAnimation) {
            TimerAnimation.reset -> { reset(); break }
            TimerAnimation.running -> { progressBar.setProgress(++progress, true) }
          }
          delay(delayMs)
        }
      }
    }
  }

  fun setup() {
    setupClick()
    setupClickClearObjects()
  }

  /**
   * When clicking the timer button
   */
  private fun setupClick() {
    btnTimer.setOnClickListener {
      // temporarily show "CONFIRM" button (if there are scanned objects)
      if (VM.statObjWindowUNQ > 0) {
        utlUi.fadeIn(btnClearObj)
        scope.launch(Dispatchers.IO) {
          delay(5000)
          resetBtnClearObjects()
        }
      }
    }
  }

  /**
   * Allowing to clear the window that was just scanned
   */
  fun setupClickClearObjects() {
    btnClearObj.setOnClickListener {
      resetLogging()
    }
  }

  fun resetLogging() {
    reset()
    VM.resetLoggingWindow()
  }


  fun reset() {
    resetBtnClearObjects()
    VM.circleTimerAnimation = TimerAnimation.reset
    resetProgressBar()
  }

  fun resetProgressBar() {
    progressBar.visibility = View.INVISIBLE
    progressBar.progress=0
  }


  fun resetBtnClearObjects() {
    utlUi.fadeOut(btnClearObj)
    // TODO: BUG
    // utlUi.fadeIn(btnClearObj)
    // scope.launch(Dispatchers.IO) {
    //   delay(100)
    //   utlUi.fadeIn(btnClearObj)
    // }
  }

  fun setToStoreMode() {
    progressBar.progress = 100
    btnTimer.text = ""
    progressBar.visibility = View.INVISIBLE

    LOG.D2(TAG, "$METHOD: storing: ${VM.objWindowLOG.value?.size}")
    if (!VM.objWindowLOG.value.isNullOrEmpty()) {
      utlUi.fadeIn(btnTimer)
      utlUi.changeMaterialIcon(btnTimer, R.drawable.ic_delete)
      utlUi.changeBackgroundMaterial(btnTimer, R.color.darkGray)
    } else {   // no results, hide the timer
      utlUi.removeMaterialIcon(btnTimer)
      utlUi.fadeOut(btnTimer)
      utlUi.fadeOut(btnClearObj)
    }
  }

  /**
   * Observes [VM.windowDetections] changes and updates
   * [binding.bottomUi.buttonCameraWindow] accordingly.
   */
  fun render() {
    val elapsed = VM.getElapsedSeconds()
    val remaining = VM.prefWindowLoggingSeconds() - elapsed
    if (remaining>0) {
      val windowSecs: Int = VM.prefWindowLoggingSeconds()
      startAnimation(windowSecs)
      btnTimer.text = utlTime.getSecondsRounded(remaining, windowSecs)
    } else {
      setToStoreMode()
    }
  }

}