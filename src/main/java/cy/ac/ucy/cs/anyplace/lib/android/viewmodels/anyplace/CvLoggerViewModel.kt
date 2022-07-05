package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace

import android.app.Application
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.*
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.Classifier
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.net.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.SMAS
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.source.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerUI
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.DetectorActivityBase
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.YoloV4Classifier
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.UtilUI
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.UserCoordinates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LoggingStatus {
  running,
  mustStore,
  stopped,
}

enum class TimerAnimation { running,  reset  }

/**
 * Extends [CvViewModel]:
 */
@HiltViewModel
class CvLoggerViewModel @Inject constructor(
        application: Application,
        dsCv: CvDataStore,
        dsCvNav: CvNavDataStore,
        dsMisc: MiscDataStore,
        // dsCvLog: CvLoggerDataStore,
        repoAP: RepoAP,
        repoSmas: RepoSmas,
        RHap: RetrofitHolderAP,
        RHsmas: RetrofitHolderSmas):
        CvViewModel(application, dsCv, dsMisc, dsCvNav, repoAP, RHap, repoSmas, RHsmas) {

  private val C by lazy { SMAS(app.applicationContext) }

  val utlUi by lazy { UtilUI(application, viewModelScope) }
  lateinit var uiLog: CvLoggerUI

  // TODO: move in new class..
  // var longClickFinished: Boolean = false
  var circleTimerAnimation: TimerAnimation = TimerAnimation.reset


  val statusLogging = MutableStateFlow(LoggingStatus.stopped)

  /** Detections of the current logger scan-window (MERGE: detectionsLogging) */
  val objWindowLOG: MutableLiveData<List<Classifier.Recognition>> = MutableLiveData()
  /** Detections assigned to map locations (MERGE:  with storedDetections) */
  var objOnMAP: MutableMap<LatLng, List<Classifier.Recognition>> = mutableMapOf()
  /** Counter over all detections? (CHECK) */
  val statObjWindowAll: MutableLiveData<Int> = MutableLiveData(0)
  /** for stats, and for enabling scanned objects clear (on current window) (MERGE: objectsWindowUnique) */
  var statObjWindowUNQ = 0
  var statObjTotal = 0

  // PREFERENCES (CHECK:PM these were SMAS).
  // val prefsChat = dsChat.read
  /** How often to refresh UI components from backend (in ms) */
  // var refreshMs : Long = C.DEFAULT_PREF_SMAS_LOCATION_REFRESH.toLong()*1000L

  override fun prefWindowLocalizationMs(): Int {
    // modify properly for Smas?
    return C.DEFAULT_PREF_CVLOG_WINDOW_LOCALIZATION_MS.toInt()
  }

  fun canStoreDetections() : Boolean {
    return (statusLogging.value == LoggingStatus.mustStore)
  }

  fun canRecognizeObjects() : Boolean {
    return app.cvUtils.isModelInited()
  }

  /**
   * CHECK:PM this was part of a VM that had the detections
   * (deep in the CV engine).
   *
   * Now this is at a higher level
   *
   * TODO:PM convert to a post call?
   */
  override fun processDetections(recognitions: List<Classifier.Recognition>,
                                 activity: DetectorActivityBase) {
    LOG.V2(TAG, "VM: CvLogger: $METHOD: ${recognitions.size}")
    super.processDetections(recognitions, activity)

    when (statusLogging.value) {
      LoggingStatus.running -> {
        updateLoggingRecognitions(recognitions)
      }
      else -> {
        LOG.V2(TAG, "$METHOD: (ignoring objects)")
      }
    }
  }

  /**
   * Update detections that concern only the logging phase.
   */
  private fun updateLoggingRecognitions(recognitions: List<Classifier.Recognition>) {
    LOG.D2(TAG, "$METHOD: ${statusLogging.value}")
    currentTime = System.currentTimeMillis()

    val appendedDetections = objWindowLOG.value.orEmpty() + recognitions
    statObjWindowAll.postValue(appendedDetections.size)

    LOG.V3(TAG, "$METHOD: appended detections: ${objWindowLOG.value?.size} (in mem)")

    if (windowStart==0L) windowStart=currentTime

    when {
      // window finished:
      currentTime-windowStart > prefWindowLoggingMs() -> {
        windowStart=0L // resetting window

        LOG.D2(TAG, "WINDOW FINISHED")

        if (appendedDetections.isEmpty()) {
          app.showToast(viewModelScope, "No detections.")
          statusLogging.update { LoggingStatus.stopped }
        } else {
          LOG.D3("updateDetectionsLogging: status: objects: ${appendedDetections.size}")
          val detectionsDedup = YoloV4Classifier.NMS(appendedDetections, detector.labels)

          LOG.W(TAG, "$METHOD: detections to store: dedup: ${detectionsDedup.size}")

          objWindowLOG.postValue(detectionsDedup)

          statObjWindowUNQ = detectionsDedup.size
          statObjTotal += statObjWindowUNQ

          LOG.E(TAG, "$METHOD: detections unique: $statObjWindowUNQ")

          viewModelScope.launch(Dispatchers.IO) {
            delay(50) // workaround:
            // little bit of delay before updating status,
            // so the [objWindowLOG] gets fully propagated

            statusLogging.update { LoggingStatus.mustStore }
          }
        }
      }
      else -> { // Within a window
        objWindowLOG.postValue(appendedDetections)
      }
    }
  }


  private fun cacheUniqueDetections(userCoords: UserCoordinates, recognitions: List<Classifier.Recognition>) {
    LOG.E(TAG,"$METHOD: CvModel: detections: ${recognitions.size}")

    viewModelScope.launch(Dispatchers.IO) {
      app.cvUtils.initConversionTables(model.idSmas)
      val detectionsReq = app.cvUtils.toCvDetections(this, recognitions, model)
      // cache.storeFingerprints(userCoords, detectionsReq, model) // TODO:PMX UPL
      nwCvFingerprintSend.safeCall(userCoords, detectionsReq, model)
    }
  }

  fun prefWindowLoggingMs(): Int { return prefsCvNav.windowLoggingMs.toInt() }
  fun prefWindowLoggingSeconds(): Int { return prefsCvNav.windowLoggingMs.toInt() /1e3.toInt() }
  fun getElapsedSeconds(): Float { return (currentTime - windowStart)/1000f }
  fun getElapsedSecondsStr(): String { return utlTime.getSecondsPretty(getElapsedSeconds()) }

  fun resetLoggingWindow() {
    statObjWindowUNQ=0
    objWindowLOG.value = emptyList()
    statusLogging.update { LoggingStatus.stopped }

    if (!cache.hasFingerprints()) ui.localization.show()
  }

  /**
   * Stores the detections on the [objOnMAP],
   * a Hash Map of locations and object fingerprints
   */

  fun cacheDetectionsLocally(userCoord: UserCoordinates, latLong: LatLng) {
    statObjTotal+=statObjWindowUNQ
    // TODO:PM do them in batch later on..
    val detections = objWindowLOG.value.orEmpty()
    objOnMAP[latLong] = detections

    cacheUniqueDetections(userCoord, detections)
  }

  override fun onLocalizationStarted() {
    super.onLocalizationStarted()
    if (!DBG.LCLG) return // PMX: LCLG

    LOG.E(TAG, "LOCALIZE: RUNNING")
    // hide all logging UI when localizing

    uiLog.bottom.logging.uploadWasVisible = uiLog.btnUpload.isVisible
    utlUi.fadeOut(uiLog.btnUpload)
    ui.floorSelector.hide()
    uiLog.bottom.logging.hide()
  }

  override fun onLocalizationEnded() {
    super.onLocalizationEnded()

    if (!DBG.LCLG) return // PMX: LCLG

    if (uiLog.bottom.logging.uploadWasVisible) utlUi.fadeIn(uiLog.btnUpload)
      ui.floorSelector.show()
      uiLog.bottom.logging.show()
  }
}