package cy.ac.ucy.cs.anyplace.lib.android.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.Detector
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.YoloV4Detector
import cy.ac.ucy.cs.anyplace.lib.android.data.Repository
import cy.ac.ucy.cs.anyplace.lib.android.data.datastore.CvLoggerPrefs
import cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers.CvMapHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers.FloorHelper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.network.RetrofitHolder
import cy.ac.ucy.cs.anyplace.lib.android.utils.uTime
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.system.measureTimeMillis

enum class Logging {
  started,
  stopped,
  stoppedMustStore,
  stoppedNoDetections,
  finished,
}

enum class TimerAnimation {
  running,
  paused,
  reset
}

/**
 * Extending [CvViewModelBase] for the additional ViewModel functionality of the Logger
 */
@HiltViewModel
class CvLoggerViewModel @Inject constructor(
  application: Application,
  repository: Repository,
  // dataStoreCvLogger: DataStoreCvLogger,
  retrofitHolder: RetrofitHolder):
  CvViewModelBase(application, repository, retrofitHolder) {

  // var longClickFinished: Boolean = false
  var circleTimerAnimation: TimerAnimation = TimerAnimation.paused
  lateinit var prefs: CvLoggerPrefs

  val windowDetections: MutableLiveData<List<Detector.Detection>> = MutableLiveData()
  val status: MutableLiveData<Logging> = MutableLiveData(Logging.stopped)
  var storedDetections: MutableMap<LatLng, List<Detector.Detection>> = mutableMapOf()
  val objectsWindowAll: MutableLiveData<Int> = MutableLiveData(0)
  /** for stats, and for enabling scanned objects clear (on current window) */
  var objectsWindowUnique = 0
  var objectsTotal = 0
  var previouslyPaused = false

  var windowStart : Long = 0
  /** stores the elapsed time on stops/pauses */
  var windowElapsedPause : Long = 0
  var currentTime : Long = 0
  var firstDetection = false

  fun canStoreDetections() : Boolean {
    return (status.value == Logging.started) || (status.value == Logging.stoppedMustStore)
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
    if (status.value == Logging.started) {
      LOG.V4(TAG, "Conversion time : $conversionTime ms")
      val detectionTime: Long = detectionProcessor.processImage(bitmap)
      LOG.V4(TAG, "Detection time : $detectionTime ms")

      val processingTime = conversionTime + detectionTime
      LOG.V3(TAG, "Analysis time : $processingTime ms")

      val detections = detectionProcessor.frameDetections
      // LOG.W(TAG, "Detected: ${detections.size}") // CLR
      updateDetections(detections)

      return detectionTime
    } else {  // Clear objects
      detectionProcessor.clearObjects()
    }
    return 0
  }

  private fun updateDetections(detections: List<Detector.Detection>) {
    currentTime = System.currentTimeMillis()
    LOG.V5(TAG, "updateDetections: ${status.value}")

    val appendedDetections = windowDetections.value.orEmpty() + detections
    objectsWindowAll.postValue(appendedDetections.size)
    when {
      firstDetection -> {
        LOG.D("updateDetections: Initing window: $currentTime")
        windowStart = currentTime
        firstDetection=false
        this.windowDetections.postValue(appendedDetections)
      }
      status.value == Logging.stoppedMustStore -> {
        windowStart = currentTime
        LOG.E("updateDetections: new window: $currentTime")
      }

      currentTime-windowStart > prefWindowMillis() -> { // Window finished
        windowElapsedPause = 0 // resetting any pause time
        previouslyPaused=false
        if (appendedDetections.isEmpty()) {
          status.postValue(Logging.stoppedNoDetections)
        } else {
          status.postValue(Logging.stoppedMustStore)
          LOG.D3("updateDetections: status: $status objects: ${appendedDetections.size}")
          val detectionsDedup = YoloV4Detector.NMS(appendedDetections, detector.labels, 0.01f)
          windowDetections.postValue(detectionsDedup)
          LOG.D3("updateDetections: status: $status objects: ${detectionsDedup.size} (dedup)")
          objectsWindowUnique=detectionsDedup.size
          objectsTotal+=objectsWindowUnique
        }
      }
      else -> { // Within a window
        this.windowDetections.postValue(appendedDetections)
      }
    }
  }

  private fun prefWindowMillis(): Int { return prefs.windowSeconds.toInt()*1000 }

  fun finalizeLogging() {
    // TODO update must be avail now
    status.value = Logging.finished
  }

  /** Toggle [status] between stopped (or notStarted), and started.
   *  There will be no effect when in stoppedMustStore mode.
   *
   *  In that case it will wait for the user to store the logging data.
   */
  fun toggleLogging() {
    when (status.value) {
      Logging.finished-> {}
      Logging.stoppedNoDetections,
      Logging.stopped -> {
        status.value = Logging.started
        val now = System.currentTimeMillis()
        windowStart=now-windowElapsedPause
      }
      Logging.started-> {
        previouslyPaused = true
        status.value = Logging.stopped
        LOG.D("toggleLogging: paused")

        // pause timer:
        val now = System.currentTimeMillis()
        windowElapsedPause = now-windowStart
      }
      else ->  {
        LOG.W(TAG, "toggleLoggingStatus: Ignoring: ${status.value}")
      }
    }
  }

  fun getElapsedSeconds(): Float { return (currentTime - windowStart)/1000f }
  fun getElapsedSecondsStr(): String { return uTime.getSecondsPretty(getElapsedSeconds()) }

  fun resetWindow() {
    objectsWindowUnique=0
    windowDetections.value = emptyList()
    status.value= Logging.stopped// CHECK:PM this was stopped. starting directly
    // status.value= Logging.started // CHECK:PM this was stopped. starting directly
  }

  fun startNewWindow() {
    objectsWindowUnique=0
    windowDetections.value = emptyList()
    status.value= Logging.stopped
    toggleLogging()
  }

  /**
   * Stores the detections on the [storedDetections],
   * a Hash Map of locations and object fingerprints
   */
  fun addDetections(latLong: LatLng) {
    objectsTotal+=objectsWindowUnique
    storedDetections[latLong] = windowDetections.value.orEmpty()
  }

  /**
   * Generates a [cvMap] from the stored detections.
   * Then it reads any local [CvMap] and merges with it.
   * Finally the merged [CvMap] is written to cache (overriding previous one),
   * and it is returned.
   */
  fun storeDetections(floorH: FloorHelper?) : CvMapHelper? {
    if (floorH == null) {
      LOG.E(TAG, "storeDetections: floorHelper is null.")
      return null
    }

    val cvMap = CvMapHelper.generate(floorH, storedDetections)
    val cvMapH = CvMapHelper(cvMap, floorH)
    LOG.D(TAG, "storeDetections: has cache: ${cvMapH.hasCache()}") // CLR:PM
    val merged = cvMapH.readLocalAndMerge()
    val mergedH = CvMapHelper(merged, floorH)
    mergedH.cache() // store merged CvMap to cache

    LOG.D(TAG, "storeDetections: has cache: ${cvMapH.hasCache()}") // CLR:PM

    return mergedH
  }
}