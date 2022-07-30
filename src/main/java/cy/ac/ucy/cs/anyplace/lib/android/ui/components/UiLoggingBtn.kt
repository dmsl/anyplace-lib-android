package cy.ac.ucy.cs.anyplace.lib.android.ui.components

import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerUI
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerUI.Companion.ANIMATION_DELAY
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.CvUI
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
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
        private val ui: CvUI,
        private val uiLog: CvLoggerUI,
        private val button_id: Int) {

  val btn: AppCompatButton by lazy { act.findViewById(button_id) }

  private val utlUi by lazy { UtilUI(act, scope) }
  private val app = act.app
  private val ctx = act.applicationContext
  private val C by lazy { CONST(ctx) }

  private val alphaMin = 0f
  private val alphaMax = 1f

  /**
   * When logging button is clicked and we must store, else: toggle logging
   */
  var btnInit = false
  fun setupClick() {
    if (btnInit) return
    btnInit = true

    LOG.E(TAG, "$METHOD: setup logging button")

    btn.setOnClickListener {
      LOG.D2(TAG, "loggingBtn: clicked: ${VM.statusLogging}")
      when (VM.statusLogging.value) {

        LoggingStatus.recognizeOnly -> { endRecognitionDemo() }
        LoggingStatus.running -> { resetLogging() }

        LoggingStatus.mustStore -> {
          app.showToast(scope, "Long-click on map to store detections", Toast.LENGTH_LONG)
        }

        LoggingStatus.stopped -> {
          if (!VM.canRecognizeObjects()) {
            app.showToast(scope, C.ERR_NO_CV_CLASSES)
            return@setOnClickListener
          }

          VM.statusLogging.update { LoggingStatus.running }
        }
        else ->  { LOG.D2(TAG, "$METHOD: ignoring click..") }
      }
    }

    btn.setOnLongClickListener {
      when (VM.statusLogging.value)  {
        LoggingStatus.stopped -> {
          VM.statusLogging.update { LoggingStatus.recognizeOnly }
        }
        LoggingStatus.recognizeOnly -> { endRecognitionDemo() }
        else -> {}
      }
      true
    }
  }

  private fun resetLogging() {
    VM.resetLoggingWindow()
    uiLog.bottom.timer.reset()
  }

  /**
   * TODO:PM put uiLog.updateUi logic IN TWO METHODS
   */
  var collecting = false
  fun collectStatus() {
    if (collecting) return
    collecting=true

    scope.launch (Dispatchers.IO){
      collectLoggingStatus()
    }
  }

  private suspend fun collectLoggingStatus() {
    VM.statusLogging.collect { status ->
      LOG.D2(TAG, "logging status: $status")
      when(status) {
        LoggingStatus.recognizeOnly -> { startRecognitionDemo() }
        LoggingStatus.running -> {  startLogging()  }
        LoggingStatus.stopped -> {  stopLogging() }
        LoggingStatus.mustStore -> {  handleMustStore() }
      }
    }
  }

  fun handleMustStore() {
    VM.disableCvDetection()

    LOG.D(TAG, "$METHOD: stopped must store: visible")
    uiLog.bottom.timer.resetBtnClearObjects()
    utlUi.animateAlpha(ui.map.mapView, alphaMax, ANIMATION_DELAY)
    ui.localization.visibilityGone() // dont show this yet..
    utlUi.changeBackgroundCompat(btn, R.color.yellowDark)
    utlUi.text(btn, "long-click on map")
    uiLog.bottom.timer.setToStoreMode()
  }


  var uploadWasVisible=false
  fun startLogging() {
    LOG.W(TAG, "$METHOD")

    uploadWasVisible = uiLog.groupUpload.isVisible
    if (uploadWasVisible) utlUi.fadeOut(uiLog.groupUpload)

    ui.floorSelector.disable()
    utlUi.disable(act.btnSettings)
    uiLog.bottom.hide()

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

  fun stopLogging() {
    if (uploadWasVisible) showUploadBtn()
    ui.floorSelector.enable()
    utlUi.enable(act.btnSettings)
    uiLog.bottom.show()

    LOG.W(TAG, "$METHOD: logging")
    VM.disableCvDetection()
    ui.map.mapView.alpha = alphaMax

    VM.circleTimerAnimation = TimerAnimation.reset
    utlUi.fadeOut(uiLog.bottom.timer.btnTimer)
    utlUi.fadeOut(uiLog.bottom.timer.progressBar)

    utlUi.changeBackgroundCompat(btn, R.color.colorPrimary)

    // LOG.D2(TAG, "call: showLocalizationButton (from notRunning)") // PMX: V22
    // uiLog.showLocalizationButton(VM.cache.hasFingerprints())
    utlUi.text(btn, "scan")
  }

  fun startRecognitionDemo() {
    LOG.W(TAG, "$METHOD: Not logging. Only obj-rec.")
    uploadWasVisible = uiLog.groupUpload.isVisible
    if (uploadWasVisible) utlUi.fadeOut(uiLog.groupUpload)

    VM.objWindowLOG.postValue(emptyList())
    VM.statObjWindowUNQ=0

    VM.enableCvDetection()

    ui.localization.hide()
    ui.map.mapView.alpha = alphaMin

    utlUi.text(btn, "stop demo")
    utlUi.changeBackgroundCompat(uiLog.bottom.logging.btn, R.color.darkGray)
    app.showSnackbar(scope, "Object Recognition Demo")

    utlUi.animateAlpha(ui.map.mapView, CvLoggerUI.OPACITY_MAP_LOGGING, ANIMATION_DELAY)
  }


  fun endRecognitionDemo() {
    LOG.D3(TAG, "$METHOD: stopping demo")
    VM.statusLogging.update { LoggingStatus.stopped }
    uiLog.showLocalizationButton(VM.cache.hasFingerprints())
  }

  fun hide() = utlUi.fadeOut(btn)
  fun show() = utlUi.fadeIn(btn)

  fun visibilityGone() {
    btn.visibility = View.GONE
  }

  // TODO:PMX UPL OK?
  // implement something like the below
  fun showUploadBtn() {
    if (!DBG.UPL) return

    ui.localization.hide()
    ui.localization.visibilityGone()
    utlUi.changeMaterialIcon(uiLog.btnUpload, R.drawable.ic_upload)
    utlUi.text(uiLog.btnUpload, ctx.getString(R.string.upload_scans))
    utlUi.enable(uiLog.groupUpload)
    utlUi.fadeInAnyway(uiLog.groupUpload)
  }

}