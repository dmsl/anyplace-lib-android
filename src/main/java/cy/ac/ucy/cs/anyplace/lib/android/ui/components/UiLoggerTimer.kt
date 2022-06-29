package cy.ac.ucy.cs.anyplace.lib.android.ui.components

import android.view.View
import android.widget.ProgressBar
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerUI
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.CvCommonUI
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
 */
class UiLoggerTimer(
        private val act: CvLoggerActivity,
        private val VM: CvLoggerViewModel,
        val scope: CoroutineScope,
        private val ui: CvCommonUI,
        private val uiLog: CvLoggerUI,
        private val id_btn_timer: Int,
        private val id_progressBar: Int,
        private val id_btn_clearObjects: Int,
) {

  private val utlUi by lazy { UtilUI(act, scope) }

  private var clearConfirm=false
  private var clickedScannedObjects=false

  val btnClearObj: MaterialButton by lazy { act.findViewById(id_btn_clearObjects) }
  val btn: MaterialButton by lazy { act.findViewById(id_btn_timer) }
  val progressBar: ProgressBar by lazy { act.findViewById(id_progressBar) }

  /**
   * Initiate a circular progress bar animation, inside a coroutine for
   * smooth (and independent from other threads) updates.
   * It progresses according to the window time
   */
  private fun startAnimation(windowSecs: Int) {
    // showing timer button but not yet the progress bar
    if (btn.visibility == View.VISIBLE &&
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

  fun reset() {
    progressBar.visibility = View.INVISIBLE
    VM.circleTimerAnimation = TimerAnimation.reset
    progressBar.progress=0
    hideBtnClearObjects()
  }

  fun setup() {
    setupClick()
    uiLog.bottom.timer.setupClearObjectsBtn()
  }

  /**
   * When clicking the timer button
   */
  private fun setupClick() {
    btn.setOnClickListener {
      if (VM.statObjWindowUNQ > 0 &&!clickedScannedObjects) {
        clickedScannedObjects=true
        utlUi.fadeIn(btnClearObj)
        scope.launch {
          delay(5000)
          clickedScannedObjects=false
          if(clearConfirm) {
            clickedScannedObjects=true
            delay(5000) // an extra delay
            clearConfirm=false
            clickedScannedObjects=false
          }
          hideBtnClearObjects()
        }
      }
    }

  }

  /**
   * Allowing to clear the window that was just scanned
   */
  fun setupClearObjectsBtn() {
    btnClearObj.setOnClickListener {
      if (!clearConfirm) {
        clearConfirm = true
        btnClearObj.text = "Sure ?"
        btnClearObj.alpha = 1f
      } else {
        hideBtnClearObjects()
        utlUi.fadeOut(btn)
        VM.resetLoggingWindow()
      }
    }
  }

  fun hideBtnClearObjects() {
    clearConfirm=false
    utlUi.fadeOut(btnClearObj)
    scope.launch {
      delay(100)
      btnClearObj.alpha = 0.5f
      btnClearObj.text = "Clear"
    }
  }

  fun setToStoreMode() {
    progressBar.progress = 100
    btn.text = ""
    progressBar.visibility = View.INVISIBLE

    if (!VM.objWindowLOG.value.isNullOrEmpty()) {
      utlUi.changeMaterialIcon(btn, R.drawable.ic_objects)
      utlUi.changeBackgroundMaterial(btn, R.color.yellowDark)
    } else {   // no results, hide the timer
      utlUi.removeMaterialIcon(btn)
      utlUi.fadeOut(btn)
    }
  }


  /**
   * Observes [VM.windowDetections] changes and updates
   * [binding.bottomUi.buttonCameraWindow] accordingly.
   */
  fun render() {
    val elapsed = VM.getElapsedSeconds()
    val remaining = (VM.prefsCvLog.windowLoggingSec.toInt()) - elapsed

    if (remaining>0) {
      val windowSecs = VM.prefsCvLog.windowLoggingSec.toInt()
      startAnimation(windowSecs)
      btn.text = utlTime.getSecondsRounded(remaining, windowSecs)
    } else {
      setToStoreMode()
    }
  }

  fun init() {
    utlUi.removeMaterialIcon(uiLog.bottom.timer.btn)
    utlUi.changeBackgroundMaterial(uiLog.bottom.timer.btn, R.color.redDark)
    utlUi.fadeIn(uiLog.bottom.timer.btn)
  }

}