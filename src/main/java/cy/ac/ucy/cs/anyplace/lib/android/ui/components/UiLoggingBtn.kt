package cy.ac.ucy.cs.anyplace.lib.android.ui.components

import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.isVisible
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerUI
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerUI.Companion.ANIMATION_DELAY
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.CvUI
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.UtilUI
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvLoggerViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.LoggingMode
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.TimerAnimation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


/**
 * UI Component:
 * - the Logging Button. handles:
 *   - logging mode and demo mode (RecognitionDemo)
 *   - uploading to the web locally cached fingerprints
 */
class UiLoggingBtn(
        private val act: CvLoggerActivity,
        private val VM: CvLoggerViewModel,
        val scope: CoroutineScope,
        private val ui: CvUI,
        private val uiLog: CvLoggerUI,
        private val button_id: Int) {
  private val TG = "ui-cv-logging"
  private val app = act.app
  private val notify = app.notify

  val btn: AppCompatButton by lazy { act.findViewById(button_id) }

  private val utlUi by lazy { UtilUI(act, scope) }
  private val ctx = act.applicationContext
  private val C by lazy { CONST(ctx) }

  private val alphaMin = 0f
  private val alphaMax = 1f

  /**
   * When logging button is clicked and we must store, else: toggle logging
   */
  var btnInit = false
  fun setupClick() {
    val MT = ::setupClick.name
    if (btnInit) return
    btnInit = true

    LOG.E(TG, "$MT: setup logging button")

    btn.setOnClickListener {
      LOG.D2(TG, "loggingBtn: clicked: ${VM.statusLogging}")
      when (VM.statusLogging.value) {

        LoggingMode.recognizeOnly -> { endRecognitionDemo() }
        LoggingMode.running -> { resetLogging() }

        LoggingMode.mustStore -> {
          notify.long(scope, "Long-click on map to store detections")
        }

        LoggingMode.stopped -> {
          if (!VM.canRecognizeObjects()) {
            notify.INF(scope, C.ERR_NO_CV_CLASSES)
            return@setOnClickListener
          }

          VM.statusLogging.update { LoggingMode.running }
        }
        else ->  { LOG.D2(TG, "$MT: ignoring click..") }
      }
    }

    btn.setOnLongClickListener {
      when (VM.statusLogging.value)  {
        LoggingMode.stopped -> {
          VM.statusLogging.update { LoggingMode.recognizeOnly }
        }
        LoggingMode.recognizeOnly -> { endRecognitionDemo() }
        else -> {}
      }
      true
    }
  }

  private fun resetLogging() {
    VM.resetLoggingWindow()
    uiLog.bottom.timer.reset()
  }

  var collecting = false
  fun collectStatus() {
    if (collecting) return
    collecting=true

    scope.launch (Dispatchers.IO){
      collectLoggingStatus()
    }
  }

  private suspend fun collectLoggingStatus() {
    val MT = ::collectLoggingStatus.name
    VM.statusLogging.collect { status ->
      LOG.D2(TG, "$MT: $status")
      when(status) {
        LoggingMode.recognizeOnly -> { startRecognitionDemo() }
        LoggingMode.running -> {  startLogging()  }
        LoggingMode.stopped -> {  stopLogging() }
        LoggingMode.mustStore -> {  handleMustStore() }
      }
    }
  }

  fun handleMustStore() {
    val MT = ::handleMustStore.name
    VM.disableCvDetection()

    LOG.D(TG, "$MT: stopped must store: visible")
    uiLog.bottom.timer.resetBtnClearObjects()
    utlUi.animateAlpha(ui.map.mapView, alphaMax, ANIMATION_DELAY)
    utlUi.changeBackgroundCompat(btn, R.color.yellowDark)
    utlUi.text(btn, "long-click on map")
    uiLog.bottom.timer.setToStoreMode()
  }


  var uploadWasVisible=false
  fun startLogging() {
    val MT = ::startLogging.name
    LOG.W(TG, MT)

    uploadWasVisible = uiLog.groupUpload.isVisible
    if (uploadWasVisible) utlUi.fadeOut(uiLog.groupUpload)

    ui.levelSelector.disable()
    utlUi.disable(act.btnSettings)
    uiLog.bottom.hideBottomSheet()

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
    val MT = ::stopLogging.name

    if (uploadWasVisible) showUploadBtn()
    ui.levelSelector.enable()
    utlUi.enable(act.btnSettings)
    uiLog.bottom.showBottomSheet()

    LOG.W(TG, "$MT: logging")
    VM.disableCvDetection()
    ui.map.mapView.alpha = alphaMax

    VM.circleTimerAnimation = TimerAnimation.reset
    utlUi.fadeOut(uiLog.bottom.timer.btnTimer)
    utlUi.fadeOut(uiLog.bottom.timer.progressBar)

    utlUi.changeBackgroundCompat(btn, R.color.colorPrimary)

    ui.localization.show() // show it anyway
    // uiLog.showLocalizationButton(VM.cache.hasFingerprints())
    utlUi.text(btn, "scan")
  }

  fun startRecognitionDemo() {
    val MT = ::startRecognitionDemo.name

    LOG.W(TG, "$MT: Not logging. Only obj-rec.")
    uploadWasVisible = uiLog.groupUpload.isVisible
    if (uploadWasVisible) utlUi.fadeOut(uiLog.groupUpload)

    VM.objWindowLOG.postValue(emptyList())
    VM.statObjWindowUNQ=0

    VM.enableCvDetection()
    ui.levelSelector.hide()

    ui.localization.hide()
    ui.map.mapView.alpha = alphaMin
    uiLog.bottom.hideBottomSheet()
    utlUi.disable(uiLog.btnSettings)

    utlUi.text(btn, "stop demo")
    utlUi.changeBackgroundCompat(uiLog.bottom.logging.btn, R.color.darkGray)
    utlUi.animateAlpha(ui.map.mapView, CvLoggerUI.OPACITY_MAP_LOGGING, ANIMATION_DELAY)

    act.layoutCamera.setBackgroundColor(VM.utlColor.DevMode())
  }


  fun endRecognitionDemo() {
    val MT = ::endRecognitionDemo.name
    LOG.D3(TG, "$MT: stopping demo")
    VM.statusLogging.update { LoggingMode.stopped }
    uiLog.bottom.showBottomSheet()
    utlUi.enable(uiLog.btnSettings)
    ui.levelSelector.show()
    act.layoutCamera.setBackgroundColor(VM.utlColor.Black())

    ui.localization.show() // show it anyway
    uiLog.showLocalizationButton(VM.cache.hasFingerprints())
  }

  fun hide() = utlUi.fadeOut(btn)
  fun show() = utlUi.fadeIn(btn)

  /**
   * Showing a button for uploading to the backend,
   * or discarding the locally stored scanned objects.
   */
  fun showUploadBtn() {
    utlUi.changeMaterialIcon(uiLog.btnUpload, R.drawable.ic_upload)
    utlUi.text(uiLog.btnUpload, ctx.getString(R.string.upload_scans))
    utlUi.enable(uiLog.groupUpload)
    utlUi.enable(uiLog.btnUpload)
    utlUi.enable(uiLog.btnUploadDiscard)

    utlUi.fadeInAnyway(uiLog.groupUpload)
  }

}