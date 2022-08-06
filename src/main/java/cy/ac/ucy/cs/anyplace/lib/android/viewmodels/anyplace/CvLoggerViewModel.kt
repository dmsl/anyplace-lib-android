package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace

import android.app.Application
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.Classifier
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.di.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.di.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.extensions.notify
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerUI
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.DetectorActivityBase
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilColor
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.UtilUI
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.UserCoordinates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LoggingMode {
  recognizeOnly,
  running,
  mustStore,
  stopped,
}

enum class TimerAnimation { running,  reset  }

/**
 * Extends [CvViewModel] and specializes for the logger
 */
@HiltViewModel
class CvLoggerViewModel @Inject constructor(
        application: Application,
        dsCv: CvDataStore,
        dsCvMap: CvMapDataStore,
        dsMisc: MiscDS,
        // dsCvLog: CvLoggerDataStore,
        repoAP: RepoAP,
        repoSmas: RepoSmas,
        RHap: RetrofitHolderAP,
        RHsmas: RetrofitHolderSmas):
        CvViewModel(application, dsCv, dsMisc, dsCvMap, repoAP, RHap, repoSmas, RHsmas) {
  private val TG = "vm-cv-logger"

  val utlUi by lazy { UtilUI(application, viewModelScope) }
  val utlColor by lazy { UtilColor(app.applicationContext) }
  lateinit var uiLog: CvLoggerUI

  var circleTimerAnimation: TimerAnimation = TimerAnimation.reset

  val statusLogging = MutableStateFlow(LoggingMode.stopped)

  /** Detections of the current logger scan-window */
  val objWindowLOG: MutableLiveData<List<Classifier.Recognition>> = MutableLiveData()
  /** Detections assigned to map locations */
  var objOnMAP: MutableMap<LatLng, List<Classifier.Recognition>> = mutableMapOf()
  /** Counter over all detections? (CHECK) */
  val statObjWindowAll: MutableLiveData<Int> = MutableLiveData(0)
  /** for stats, and for enabling scanned objects clear (on current window) */
  var statObjWindowUNQ = 0
  var statObjTotal = 0

  var initedUiLog: Boolean = false

  override fun prefWindowLocalizationMs(): Int {
    return C.DEFAULT_PREF_CV_WINDOW_LOCALIZATION_MS.toInt()
  }

  fun canStoreDetections() : Boolean {
    return (statusLogging.value == LoggingMode.mustStore)
  }

  fun canRecognizeObjects() : Boolean {
    return app.cvUtils.isModelInited()
  }

  /**
   * It processes the detections once the inference has run,
   * only if logging mode is running.
   *
   * Otherwise, if localization mode was running, the call to [super] would
   * have done this
   */
  override fun processDetections(recognitions: List<Classifier.Recognition>,
                                 activity: DetectorActivityBase) {
    val MT = ::processDetections.name
    LOG.V2(TG, "$MT: ${recognitions.size}")
    super.processDetections(recognitions, activity)

    when (statusLogging.value) {
      LoggingMode.running -> {
        updateDetectionsLogging(recognitions)
      }
      else -> {
        LOG.V2(TG, "$MT: (ignoring objects)")
      }
    }
  }

  /**
   * Update detections that concern only the logging phase.
   */
  private fun updateDetectionsLogging(recognitions: List<Classifier.Recognition>) {
    val MT = ::updateDetectionsLogging.name
    LOG.D3(TG, "$MT: ${statusLogging.value}")
    currentTime = System.currentTimeMillis()

    val appendedDetections = objWindowLOG.value.orEmpty() + recognitions
    statObjWindowAll.postValue(appendedDetections.size)

    LOG.V3(TG, "$MT: appended detections: ${objWindowLOG.value?.size} (in mem)")

    if (windowStart==0L) windowStart=currentTime

    when {
      // window finished:
      currentTime-windowStart > prefWindowLoggingMs() -> {
        windowStart=0L // resetting window

        LOG.D2(TG, "WINDOW FINISHED")

        if (appendedDetections.isEmpty()) {
          if (trackingMode.value != TrackingMode.on) {
            notify.short(viewModelScope, "No detections.")
          }
          statusLogging.update { LoggingMode.stopped }
        } else {
          LOG.D3(TG, "$MT: status: objects: ${appendedDetections.size}")
          // not running nms
          val detectionsDedup = appendedDetections // YoloV4Classifier.NMS(appendedDetections, detector.labels)

          LOG.W(TG, "$MT: detections to store: dedup: ${detectionsDedup.size}")

          objWindowLOG.postValue(detectionsDedup)

          statObjWindowUNQ = detectionsDedup.size
          statObjTotal += statObjWindowUNQ

          LOG.E(TG, "$MT: detections unique: $statObjWindowUNQ")

          viewModelScope.launch(Dispatchers.IO) {
            delay(50) // workaround:
            // little bit of delay before updating status,
            // so the [objWindowLOG] gets fully propagated
            statusLogging.update { LoggingMode.mustStore }
          }
        }
      }
      else -> { // Within a window
        objWindowLOG.postValue(appendedDetections)
      }
    }
  }


  private suspend fun cacheUniqueDetections(userCoords: UserCoordinates, recognitions: List<Classifier.Recognition>) {
    val MT = ::cacheUniqueDetections.name
    LOG.D(TG,"$MT: CvModel: detections: ${recognitions.size}")

    app.cvUtils.initConversionTables(model.idSmas)
    val detectionsReq = app.cvUtils.toCvDetections(viewModelScope, recognitions, model)

    cache.storeFingerprints(userCoords, detectionsReq, model)
    // INFO: this used to upload fingerprints on the spot
    // nwCvFingerprintSend.safeCall(userCoords, detectionsReq, model)
  }

  fun prefWindowLoggingMs(): Int { return prefsCvMap.windowLoggingMs.toInt() }
  fun prefWindowLoggingSeconds(): Int { return prefsCvMap.windowLoggingMs.toInt() /1e3.toInt() }
  fun getElapsedSeconds(): Float { return (currentTime - windowStart)/1000f }
  fun getElapsedSecondsStr(): String {
    val res = "%.1f".format(getElapsedSeconds()) + "s"
    if (res.length > 4) return "0.0s"
    return res
  }

  fun resetLoggingWindow() {
    statObjWindowUNQ=0
    objWindowLOG.postValue(emptyList())
    statusLogging.update { LoggingMode.stopped }

    if (!cache.hasFingerprints()) ui.localization.show()
  }

  /**
   * Stores the detections on the [objOnMAP],
   * a Hash Map of locations and object fingerprints
   */

  suspend fun cacheDetectionsLocally(userCoord: UserCoordinates, latLong: LatLng) {
    statObjTotal+=statObjWindowUNQ
    val detections = objWindowLOG.value.orEmpty()
    objOnMAP[latLong] = detections

    cacheUniqueDetections(userCoord, detections)
  }

  override fun onLocalizationStarted() {
    super.onLocalizationStarted()
    val MT = ::onLocalizationStarted.name

    LOG.W(TG, "$MT: RUNNING")
    // hide all logging UI when localizing

    uiLog.bottom.logging.uploadWasVisible = uiLog.groupUpload.isVisible
    utlUi.fadeOut(uiLog.groupUpload)
    ui.levelSelector.hide()
    uiLog.bottom.logging.hide()
  }

  override fun onLocalizationEnded() {
    super.onLocalizationEnded()

    viewModelScope.launch(Dispatchers.IO) {
      waitForUiLogger()
      if (uiLog.bottom.logging.uploadWasVisible) utlUi.fadeIn(uiLog.groupUpload)
      ui.levelSelector.show()
      uiLog.bottom.logging.show()
    }
  }

  suspend fun waitForUiLogger() {
    val MT = ::waitForUiLogger.name
    while (!initedUiLog || !uiLog.uiBottomLazilyInited) {
     delay(100)
      LOG.V(TG, "$MT ..")
    }
  }
}