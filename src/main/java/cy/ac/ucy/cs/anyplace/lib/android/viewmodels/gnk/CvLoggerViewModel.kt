package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.gnk

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.legacy.gnk.utils.Detector
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.legacy.gnk.utils.YoloV4Detector
import cy.ac.ucy.cs.anyplace.lib.android.data.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.store.CvLoggerPrefs
import cy.ac.ucy.cs.anyplace.lib.android.data.models.helpers.CvMapHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.models.helpers.FloorHelper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.network.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.system.measureTimeMillis

// TODO: keep just logger stuff, and put generic in CvVM

enum class Logging {
  running,
  stopped,
  stoppedMustStore,
  stoppedNoDetections,
  demoNavigation, // DemoLocalization
  // finished, CLR:PM
}

enum class TimerAnimation {
  running,
  paused,
  reset,
}

/**
 * Extending [CvViewModelBase] for the additional ViewModel functionality of the Logger
 */
@HiltViewModel
class CvLoggerViewModel @Inject constructor(
        application: Application,
        repoAP: RepoAP,
  // dataStoreCvLogger: DataStoreCvLogger,
        retrofitHolderAP: RetrofitHolderAP):
  CvViewModelBase(application, repoAP, retrofitHolderAP) {

  // var longClickFinished: Boolean = false
  var circleTimerAnimation: TimerAnimation = TimerAnimation.paused
  lateinit var prefs: CvLoggerPrefs

  val logging: MutableLiveData<Logging> = MutableLiveData(Logging.stopped)
  /** Detections of the current logger scan-window */
  val detectionsLogging: MutableLiveData<List<Detector.Detection>> = MutableLiveData()
  /** Detections assigned to map locations */
  var storedDetections: MutableMap<LatLng, List<Detector.Detection>> = mutableMapOf()
  val objectsWindowAll: MutableLiveData<Int> = MutableLiveData(0)
  /** for stats, and for enabling scanned objects clear (on current window) */
  var objectsWindowUnique = 0
  var objectsTotal = 0
  /** whether there was a pause in the current scanning window */
  var previouslyPaused = false
  /** no logging operations performed before */
  var initialStart = true

  /** stores the elapsed time on stops/pauses */
  var windowElapsedPause : Long = 0
  var firstDetection = false

  fun canStoreDetections() : Boolean {
    return (logging.value == Logging.running) || (logging.value == Logging.stoppedMustStore)
  }

  @SuppressLint("UnsafeOptInUsageError")
  fun detectObjectsOnImage(image: ImageProxy): Long {
    var bitmap: Bitmap
    val conversionTime = measureTimeMillis {
      bitmap = imageConverter!!.imageToBitmap(image.image!!)
      if (CAMERA_ROTATION % 2 == 0) {
        bitmap = rotateImage(bitmap, 90.0f)
      }
    }

    when (logging.value) {
      Logging.running -> {
        val detectionTime: Long = detectionProcessor.processImage(bitmap)
        val processingTime = conversionTime + detectionTime
        val detections = detectionProcessor.frameDetections
        LOG.V4(TAG, "Conversion time : $conversionTime ms")
        LOG.V4(TAG, "Detection time : $detectionTime ms")
        LOG.V3(TAG, "Analysis time : $processingTime ms")
        LOG.V3(TAG, "Detected: ${detections.size}")

        updateDetectionsLogging(detections)
        return detectionTime
      }
      Logging.demoNavigation -> {
        val detectionTime: Long = detectionProcessor.processImage(bitmap)
        val detections = detectionProcessor.frameDetections
        LOG.V4(TAG, "Detection time : $detectionTime ms")

        updateDetectionsLocalization(detections)
        return detectionTime
      }
      else -> {  // Clear objects
        detectionProcessor.clearObjects()
      }
    }
    return 0
  }

  /**
   * Update detections that concern only the logging phase.
   */
  private fun updateDetectionsLogging(detections: List<Detector.Detection>) {
    currentTime = System.currentTimeMillis()
    LOG.V5(TAG, "updateDetectionsLogging: ${logging.value}")

    val appendedDetections = detectionsLogging.value.orEmpty() + detections
    objectsWindowAll.postValue(appendedDetections.size)
    when {
      firstDetection -> {
        LOG.D3("updateDetectionsLogging: Initing window: $currentTime")
        windowStart = currentTime
        firstDetection=false
        this.detectionsLogging.postValue(appendedDetections)
      }
      logging.value == Logging.stoppedMustStore -> {
        windowStart = currentTime
        LOG.D("updateDetectionsLogging: new window: $currentTime")
      }

      currentTime-windowStart > prefWindowLoggingMillis() -> { // Window finished
        windowElapsedPause = 0 // resetting any pause time
        previouslyPaused=false
        if (appendedDetections.isEmpty()) {
          logging.postValue(Logging.stoppedNoDetections)
        } else {
          logging.postValue(Logging.stoppedMustStore)
          LOG.D3("updateDetectionsLogging: status: $logging objects: ${appendedDetections.size}")
          val detectionsDedup = YoloV4Detector.NMS(appendedDetections, detector.labels, 0.01f)
          detectionsLogging.postValue(detectionsDedup)
          LOG.D3("updateDetectionsLogging: status: $logging objects: ${detectionsDedup.size} (dedup)")
          objectsWindowUnique=detectionsDedup.size
          objectsTotal+=objectsWindowUnique
        }
      }
      else -> { // Within a window
        this.detectionsLogging.postValue(appendedDetections)
      }
    }
  }

  fun prefWindowLoggingMillis(): Int { return prefs.windowLoggingSeconds.toInt()*1000 }
  override fun prefWindowLocalizationMillis(): Int { return prefs.windowLocalizationSeconds.toInt()*1000 }

  /** Toggle [logging] between stopped (or notStarted), and started.
   *  There will be no effect when in stoppedMustStore mode.
   *
   *  In that case it will wait for the user to store the logging data.
   */
  fun toggleLogging() {
    initialStart = false
    when (logging.value) {
      // Logging.finished-> {}
      Logging.stoppedNoDetections,
      Logging.stopped -> {
        logging.value = Logging.running
        val now = System.currentTimeMillis()
        windowStart=now-windowElapsedPause
      }
      Logging.running -> {
        previouslyPaused = true
        logging.value = Logging.stopped
        LOG.D("toggleLogging: paused")

        // pause timer:
        val now = System.currentTimeMillis()
        windowElapsedPause = now-windowStart
      }
      else ->  {
        LOG.W(TAG, "toggleLoggingStatus: Ignoring: ${logging.value}")
      }
    }
  }

  fun getElapsedSeconds(): Float { return (currentTime - windowStart)/1000f }
  fun getElapsedSecondsStr(): String { return utlTime.getSecondsPretty(getElapsedSeconds()) }

  fun resetLoggingWindow() {
    objectsWindowUnique=0
    detectionsLogging.value = emptyList()
    logging.value= Logging.stopped// CHECK:PM this was stopped. starting directly
    // status.value= Logging.started // CHECK:PM this was stopped. starting directly
  }

  fun startNewWindow() {
    objectsWindowUnique=0
    detectionsLogging.value = emptyList()
    logging.value= Logging.stopped
    toggleLogging()
  }

  /**
   * Stores the detections on the [storedDetections],
   * a Hash Map of locations and object fingerprints
   */
  fun addDetections(latLong: LatLng) {
    objectsTotal+=objectsWindowUnique
    storedDetections[latLong] = detectionsLogging.value.orEmpty()
  }

  /**
   * Generates a [cvMap] from the stored detections.
   * Then it reads any local [CvMap] and merges with it.
   * Finally the merged [CvMap] is written to cache (overriding previous one),
   * and stored in [CvViewModelBase].
   */
  fun storeDetections(FH: FloorHelper?) {
    if (FH == null) {
      LOG.E(TAG, "storeDetections: floorHelper is null.")
      return
    }

    // TODO:PM UPDATE radiomap TODO:TRIAL
    val curMap = CvMapHelper.generate(detector.getDetectionModel(), FH, storedDetections)
    val curMapH = CvMapHelper(curMap, detector.labels, FH)
    LOG.D(TAG, "storeDetections: has cache: ${curMapH.hasCache()}") // CLR:PM
    val merged = curMapH.readLocalAndMerge()
    val mergedH = CvMapHelper(merged, detector.labels, FH)
    mergedH.storeToCache()

    LOG.D(TAG, "storeDetections: has cache: ${cvMapH?.hasCache()}") // CLR:PM

    mergedH.generateCvMapFast()
    cvMapH = mergedH

    storedDetections.clear()
  }
}