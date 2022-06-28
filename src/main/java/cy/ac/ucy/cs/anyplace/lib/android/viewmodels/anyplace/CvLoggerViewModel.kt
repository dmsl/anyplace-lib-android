package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.CvMapHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.FloorWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.*
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.Classifier
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.net.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.CHAT
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.DetectionModel
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.source.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.YoloV4Classifier
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.UserCoordinates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LoggingStatus {
  running,
  mustStore,
  stopped,
}

enum class TimerAnimation { running,  paused,  reset  }

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

  private val C by lazy { CHAT(app.applicationContext) }

  // TODO: move in new class..
  // var longClickFinished: Boolean = false
  var circleTimerAnimation: TimerAnimation = TimerAnimation.paused

  lateinit var prefsCvLog : CvLoggerPrefs

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

  // CLR:PM
  /** stores the elapsed time on stops/pauses */
  // var windowElapsedPause : Long = 0
  // var firstDetection = false

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

  /**
   * CHECK:PM this was part of a VM that had the detections
   * (deep in the CV engine).
   *
   * Now this is at a higher level
   *
   * TODO:PM convert to a post call?
   */
  override fun processDetections(recognitions: List<Classifier.Recognition>) {
    LOG.D2(TAG, "VM: CvLogger: $METHOD: ${recognitions.size}")
    super.processDetections(recognitions)

    when (statusLogging.value) {
      LoggingStatus.running -> {
        updateLoggingRecognitions(recognitions)
      }
      else -> {  // Clear objects
        // MERGE:
        // Dont run this often?
        // detectionProcessor.clearObjects()
        LOG.E(TAG, "$METHOD: (ignoring objects)")
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

    LOG.E(TAG, "DETECTIONS IN MEM: ${objWindowLOG.value?.size}")

    if (windowStart==0L) windowStart=currentTime

    when {
      // firstDetection -> { CLR:PM
      //   LOG.D3(TAG, "$METHOD: initing window: $currentTime")
      //   windowStart = currentTime
      //   firstDetection=false
      //   this.objWindowLOG.postValue(appendedDetections)
      // }
      // logging.value == Logging.stoppedMustStore -> { CLR:PM
      //   windowStart = currentTime
      //   LOG.D("updateDetectionsLogging: new window: $currentTime")
      // }

      // WINDOW FINISHED:
      currentTime-windowStart > prefWindowLoggingMillis() -> {
        // windowElapsedPause = 0 // resetting any pause time
        windowStart=0L // resetting window

        LOG.E(TAG, "WINDOW FINISHED")

        // previouslyPaused=false
        if (appendedDetections.isEmpty()) {
          app.showToast(viewModelScope, "No detections.")
          statusLogging.update { LoggingStatus.stopped }
        } else {
          LOG.E(TAG, "TODO: store in filesystem")
          LOG.D3("updateDetectionsLogging: status: objects: ${appendedDetections.size}")
          val detectionsDedup =
                  YoloV4Classifier.NMS(appendedDetections, detector.labels)

          LOG.E(TAG, "TODO: detections to store: dedup: $detectionsDedup")

          // objWindowLOG.postValue(detectionsDedup)   // TODO:CLR THIS


          // LOG.D3("updateDetectionsLogging: status: $logging objects: ${detectionsDedup.size} (dedup)")
          statObjWindowUNQ=detectionsDedup.size
          statObjTotal+=statObjWindowUNQ

          statusLogging.update { LoggingStatus.mustStore }
        }
      }
      else -> { // Within a window
        objWindowLOG.postValue(appendedDetections)
      }
    }
  }


  /**
   * Upload Unique detections
   */
  private fun uploadUniqueDetections(userCoords: UserCoordinates, recognitions: List<Classifier.Recognition>) {
    LOG.E(TAG,"$METHOD: CvModel: detections: ${recognitions.size}")

    viewModelScope.launch(Dispatchers.IO) {
      // TODO:PM store on database (still will be a resp success..)

      app.cvUtils.initConversionTables(model.idSmas)
      val detectionsReq = app.cvUtils.toCvDetections(recognitions, model)

      // LEFTHERE:
      /* TODO: BATCH STORING
      - file cache: append a line: in this method
      - NW call: storeDetectionsAndUpdateUI
        - or w/ an upload button
       */

      // 0. make NW call (DONE)
      // 1. read from DB and to oid from DB query DONE
      // 2. keep recording elements
      // 3. store then in a file, line by line:
      // // 3.a it will be 2 places: file line by line
      // // 3.b and the CVMap json
      // CHECK:PM
      // Can we upload at the end the local radiomap?
      // UI TODO
      // 1. BIND THE TIMER BUTTON

      LOG.E(TAG, "uploadUniqueDetections: SKIPPING NW call here")
      LOG.E(TAG, "TODO: [STORE THEM IN FILE CODE LOCATION]")
      // nwCvFingerprintSend.safeCall(userCoords, detectionsReq, model)
      // TODO test from different model too..
    }
  }

  fun prefWindowLoggingMillis(): Int { return prefsCvLog.windowLoggingSeconds.toInt()*1000 }

  /** Toggle [logging] between stopped (or notStarted), and started.
   *  There will be no effect when in stoppedMustStore mode.
   *
   *  In that case it will wait for the user to store the logging data.
   */
  // fun toggleLogging() {
  //   initialStart = false
  //   when (logging.value) {
  //     // Logging.finished-> {}
  //     Logging.stoppedNoDetections,
  //     Logging.stopped -> {
  //       logging.value = Logging.running
  //       val now = System.currentTimeMillis()
  //       windowStart=now-windowElapsedPause
  //     }
  //     Logging.running -> {
  //       previouslyPaused = true
  //       logging.value = Logging.stopped
  //       LOG.D(TAG, "$METHOD: paused")
  //
  //       // pause timer:
  //       val now = System.currentTimeMillis()
  //       windowElapsedPause = now-windowStart
  //     }
  //     else ->  {
  //       LOG.W(TAG, "$METHOD: Ignoring: ${logging.value}")
  //     }
  //   }
  // }

  fun getElapsedSeconds(): Float { return (currentTime - windowStart)/1000f }
  fun getElapsedSecondsStr(): String { return utlTime.getSecondsPretty(getElapsedSeconds()) }

  fun resetLoggingWindow() {
    statObjWindowUNQ=0
    objWindowLOG.value = emptyList()
    statusLogging.update { LoggingStatus.stopped }
  }

  // fun startNewWindow() {
  //   statObjWindowUNQ=0
  //   objWindowLOG.value = emptyList()
  //   // statusLogging=CvLoggerUI.LoggingStatus.running
  //   // logging.value= Logging.stopped
  //   // toggleLogging()
  // }

  /**
   * Stores the detections on the [objOnMAP],
   * a Hash Map of locations and object fingerprints
   */

  fun addDetections(FH: FloorWrapper?, model: DetectionModel, latLong: LatLng) {
    statObjTotal+=statObjWindowUNQ
    // TODO:PM do them in batch later on..
    val detections = objWindowLOG.value.orEmpty()
    objOnMAP[latLong] = detections

    // floorH.spaceH.obj.id
    val userCoord = UserCoordinates(wFloor?.spaceH?.obj?.id!!,
            wFloor?.obj!!.floorNumber.toInt(),
            latLong.latitude, latLong.longitude)

    uploadUniqueDetections(userCoord, detections)
  }


  /**
   * Generates a [cvMap] from the stored detections.
   * Then it reads any local [CvMap] and merges with it.
   * Finally the merged [CvMap] is written to cache (overriding previous one),
   * and stored in [CvViewModelBase].
   */
  fun storeDetectionsLOCAL(FH: FloorWrapper?) {
    LOG.E(TAG, "storeDetectionsLOCAL: updating heatmaps etc?")
    if (FH == null) {
      LOG.E(TAG, "$METHOD: floorHelper is null.")
      return
    }

    // MERGE:PM:TODO
    // TODO: UPDATE radiomap (this was a trial todo?)
    val curMap = CvMapHelper.generate(app, model, FH, objOnMAP)
    val curMapH = CvMapHelper(curMap, detector.labels, FH)
    LOG.D(TAG, "$METHOD: has cache: ${curMapH.hasCache()}") // CLR:PM
    val merged = curMapH.readLocalAndMerge()
    val mergedH = CvMapHelper(merged, detector.labels, FH)
    mergedH.storeToCache()

    LOG.D(TAG, "$METHOD: has cache: ${cvMapH?.hasCache()}") // CLR:PM
    mergedH.generateCvMapFast()
    cvMapH = mergedH
    objOnMAP.clear()
  }
}