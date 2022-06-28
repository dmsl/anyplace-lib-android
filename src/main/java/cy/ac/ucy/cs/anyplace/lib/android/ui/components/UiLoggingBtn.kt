package cy.ac.ucy.cs.anyplace.lib.android.ui.components

import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerUI
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerUI.Companion.ANIMATION_DELAY
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.CvCommonUI
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.UtilUI
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvLoggerViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.LoggingStatus
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.TimerAnimation
// import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.LocalizingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch



/**
 * UI Logging Button
 */
class UiLoggingBtn(
        private val act: CvLoggerActivity,
        private val VM: CvLoggerViewModel,
        val scope: CoroutineScope,
        private val ui: CvCommonUI,
        private val uiLog: CvLoggerUI,
        private val button_id: Int
        ) {

  val btn: AppCompatButton by lazy { act.findViewById(button_id) }

  private val utlUi by lazy { UtilUI(act, scope) }
  private val app = act.app

  private val ctx = act.applicationContext
  // val btn: MaterialButton by lazy { act.findViewById(button_id) }

  /**
   * When logging button is clicked and we must store, else: toggle logging
   */
  fun setupClick() {
    btn.setOnClickListener {
      LOG.D2(TAG, "loggingBtn: clicked: ${VM.statusLogging}")
      when (VM.statusLogging.value) {
        LoggingStatus.mustStore -> {
          app.showToast(scope, "Long-click on map to store detections", Toast.LENGTH_LONG)
        }
        LoggingStatus.stopped -> {
          VM.statusLogging.update { LoggingStatus.running }
          // if (VM.objOnMAP.isEmpty())  handleStoreNoDetections() CLR:PM
          //  else handleStoreDetections(ui.map.obj)
        }
        else ->  {
          LOG.W(TAG, "$METHOD: ignoring click..")
        }
      }
    }
  }

  /**
   * TODO:PM put uiLog.updateUi logic IN TWO METHODS
   */
  var collectingStatus = false
  fun collectStatus() {
    if (collectingStatus) return
    collectingStatus=true

    scope.launch (Dispatchers.IO){
      VM.statusLogging.collect { status ->
        LOG.W(TAG, "$METHOD: status: $status")
        when(status) {
          LoggingStatus.running -> {  startLogging()  }
          LoggingStatus.stopped -> {  notRunning() }
          LoggingStatus.mustStore -> {  handleMustStore() }
        }
      }
    }
  }

  fun handleMustStore() {
    VM.disableCvDetection()

    LOG.D(TAG, "$METHOD: stopped must store: visible")
    utlUi.animateAlpha(ui.map.mapView, 1f, ANIMATION_DELAY)
    ui.localization.visibilityGone() // dont show this yet..
    utlUi.changeBackgroundCompat(btn, R.color.yellowDark)
    utlUi.text(btn, "long-click on position")

    // utlUi.fadeIn(uiLog.bottom.btnTimer)
    // VM.circleTimerAnimation = TimerAnimation.reset
    // utlUi.changeBackgroundCompat(uiLog.bottom.btnTimer, R.color.yellowDark)
    uiLog.setCameraTimerToMustStore()
  }

  fun startLogging() {
    LOG.E(TAG, "$METHOD: TODO")
  //   btn.isEnabled=false
    VM.enableCvDetection()

    VM.currentTime = System.currentTimeMillis()
    VM.windowStart = VM.currentTime

    ui.localization.hide()
    ui.map.mapView.alpha = 0f

    VM.circleTimerAnimation = TimerAnimation.paused
    utlUi.text(btn, "cancel")
    utlUi.changeBackgroundCompat(uiLog.bottom.logging.btn, R.color.darkGray)

    // TODO:PM make a method: prepare time button..
    utlUi.removeMaterialIcon(uiLog.bottom.btnTimer)
    utlUi.changeBackgroundMaterial(uiLog.bottom.btnTimer, R.color.redDark)
    utlUi.fadeIn(uiLog.bottom.btnTimer)
    utlUi.animateAlpha(ui.map.mapView, CvLoggerUI.OPACITY_MAP_LOGGING, CvLoggerUI.ANIMATION_DELAY)

    //   // CHECK: replace in the UiLocalization too?
    //   // VM.statusLocalization.value = LocalizationStatus.running
    //   btn.visibility = View.VISIBLE
    //   // utlButton.changeBackgroundDONT_USE(btn, R.color.colorPrimary)
    //   val mapAlpha = VM.prefsCvNav.mapAlpha.toFloat()/100
  }

  /* TODO:PM
  - react to UNPLACED detections!!!!!
    (unplaced on map detections..)
    - these are also NOT STORED ON DISK!
   */

  //
  fun notRunning() {
    LOG.W(TAG, "$METHOD: logging")
    VM.disableCvDetection()
    ui.map.mapView.alpha = 1f

    VM.circleTimerAnimation = TimerAnimation.reset
    utlUi.fadeOut(uiLog.bottom.btnTimer)
    utlUi.fadeOut(uiLog.bottom.progressBarTimer)
    // VM.circleTimerAnimation = TimerAnimation.reset

    utlUi.changeBackgroundCompat(btn, R.color.colorPrimary)

    ui.localization.show()
    btn.text = "scan"
  }

  fun hide() = utlUi.fadeOut(btn)
  fun show() = utlUi.fadeIn(btn)

  fun visibilityGone() {
    btn.visibility = View.GONE
  }

}