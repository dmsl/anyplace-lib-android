package cy.ac.ucy.cs.anyplace.lib.android.ui.components

import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.isVisible
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
import kotlinx.coroutines.delay
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

  private val alphaMin = 0f
  private val alphaMax = 1f

  /**
   * When logging button is clicked and we must store, else: toggle logging
   */
  var btnInit = false
  fun setupClick() {
    if (btnInit) return // PMX: NTI
    btnInit = true

    LOG.E(TAG, "$METHOD: setup logging button")

    btn.setOnClickListener {
      LOG.D2(TAG, "loggingBtn: clicked: ${VM.statusLogging}")
      when (VM.statusLogging.value) {

        LoggingStatus.running -> {
          resetLogging()
        }
        LoggingStatus.mustStore -> {
          app.showToast(scope, "Long-click on map to store detections", Toast.LENGTH_LONG)
        }
        LoggingStatus.stopped -> {
          VM.statusLogging.update { LoggingStatus.running }
        }
        else ->  {
          LOG.D2(TAG, "$METHOD: ignoring click..")
        }
      }
    }
  }

  private fun resetLogging() {
    VM.resetLoggingWindow()
    uiLog.bottom.timer.reset()
    if (!VM.cache.hasFingerprints()) ui.localization.show()
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
        LOG.D2(TAG, "logging status: $status")
        when(status) {
          LoggingStatus.running -> {  startLogging()  }
          LoggingStatus.stopped -> {  notRunning() }
          LoggingStatus.mustStore -> {  handleMustStore() }
        }
      }
    }
  }

  fun handleMustStore() {
    // if (uploadButtonWasVisible) showUploadBtn()
    VM.disableCvDetection()

    LOG.D(TAG, "$METHOD: stopped must store: visible")
    utlUi.animateAlpha(ui.map.mapView, alphaMax, ANIMATION_DELAY)
    ui.localization.visibilityGone() // dont show this yet..
    utlUi.changeBackgroundCompat(btn, R.color.yellowDark)
    utlUi.text(btn, "long-click on map")
    uiLog.bottom.timer.setToStoreMode()
  }


  var uploadButtonWasVisible=false
  fun startLogging() {
    LOG.W(TAG, "$METHOD")

    uploadButtonWasVisible = uiLog.btnUpload.isVisible
    if (uploadButtonWasVisible) utlUi.fadeOut(uiLog.btnUpload)

    ui.floorSelector.hide()

    VM.enableCvDetection()

    VM.currentTime = System.currentTimeMillis()
    VM.windowStart = VM.currentTime

    ui.localization.hide()
    ui.map.mapView.alpha = alphaMin

    VM.circleTimerAnimation = TimerAnimation.running
    utlUi.text(btn, "cancel")
    utlUi.changeBackgroundCompat(uiLog.bottom.logging.btn, R.color.darkGray)

    uiLog.bottom.timer.prepareForStart()
    utlUi.animateAlpha(ui.map.mapView, CvLoggerUI.OPACITY_MAP_LOGGING, ANIMATION_DELAY)
  }

  fun notRunning() {
    if (uploadButtonWasVisible) showUploadBtn()
    ui.floorSelector.show()

    LOG.W(TAG, "$METHOD: logging")
    VM.disableCvDetection()
    ui.map.mapView.alpha = alphaMax

    VM.circleTimerAnimation = TimerAnimation.reset
    utlUi.fadeOut(uiLog.bottom.timer.btn)
    utlUi.fadeOut(uiLog.bottom.timer.progressBar)

    utlUi.changeBackgroundCompat(btn, R.color.colorPrimary)

    // LOG.D2(TAG, "call: showLocalizationButton (from notRunning)")
    // uiLog.showLocalizationButton(VM.cache.hasFingerprints())
    utlUi.text(btn, "scan")
  }

  fun hide() = utlUi.fadeOut(btn)
  fun show() = utlUi.fadeIn(btn)

  fun visibilityGone() {
    btn.visibility = View.GONE
  }

  fun showUploadBtn() {
    ui.localization.hide()
    ui.localization.visibilityGone()

    utlUi.changeMaterialIcon(uiLog.btnUpload, R.drawable.ic_upload)
    utlUi.text(uiLog.btnUpload, ctx.getString(R.string.upload_scans))
    utlUi.enable(uiLog.btnUpload)

    utlUi.fadeInAnyway(uiLog.btnUpload)
  }

}