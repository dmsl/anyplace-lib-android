package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.View
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp_MembersInjector
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.cv.misc.Constants
import cy.ac.ucy.cs.anyplace.lib.android.cv.misc.DetectionProcessor
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.Detector
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.DetectorFactory
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.YoloV4Detector
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.utils.ImageToBitmapConverter
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.utils.RenderScriptImageToBitmapConverter
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.visualization.TrackingOverlayView
import cy.ac.ucy.cs.anyplace.lib.android.data.Repository
import cy.ac.ucy.cs.anyplace.lib.android.data.datastore.CvLoggerPrefs
import cy.ac.ucy.cs.anyplace.lib.android.data.datastore.DataStoreCvLogger
import cy.ac.ucy.cs.anyplace.lib.android.data.db.entities.UserOwnership
import cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers.FloorHelper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.extensions.observeOnce
import cy.ac.ucy.cs.anyplace.lib.android.maps.Markers
import cy.ac.ucy.cs.anyplace.lib.android.utils.ImgUtils
import cy.ac.ucy.cs.anyplace.lib.android.utils.demo.AssetReader
import cy.ac.ucy.cs.anyplace.lib.android.utils.network.RetrofitHolder
import cy.ac.ucy.cs.anyplace.lib.models.Spaces
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.lang.Exception
import java.net.ConnectException
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

@HiltViewModel
class CvLoggerViewModel @Inject constructor(
  app: Application, // this is not the AnyplaceApp, hence it is not a field.
  // AnyplaceApp can be used below as app through an Extension function
  val repository: Repository,
  // dataStoreCvLogger: DataStoreCvLogger,
  private val retrofitHolder: RetrofitHolder): AndroidViewModel(app) {

  private val assetReader by lazy { AssetReader(app.applicationContext) }
  /** Initialized onMapReady */
  var markers : Markers? = null

  companion object {
    /** Surface.ROTATION_0: portrait, Surface.ROTATION_270: landscape */
    const val CAMERA_ROTATION: Int = Surface.ROTATION_0

    // TODO move in utils
    fun getSecondsRounded(num: Float, maxAllowed: Int): String {
      var rounded = num.toInt() + 1
      if (rounded > maxAllowed) rounded = maxAllowed
      return rounded.toString()
    }

    fun getSecondsPretty(num: Float): String {
      val res = "%.1f".format(num) + "s"
      if (res.length > 4) return "0.0s"
      return res
    }
  }

  var circleTimerAnimation: TimerAnimation = TimerAnimation.paused
  lateinit var prefs: CvLoggerPrefs

  val floorplanResp: MutableLiveData<NetworkResult<Bitmap>> = MutableLiveData()

  private lateinit  var detectionProcessor: DetectionProcessor
  private lateinit var detector: YoloV4Detector
  private var imageConverter: ImageToBitmapConverter? = null // TODO:PM LATE INIT?

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

  fun canStoreObjects() : Boolean {
    return (status.value == Logging.started) || (status.value == Logging.stoppedMustStore)
  }

  fun setUpDetectionProcessor(
    assetManager: AssetManager,
    displayMetrics: DisplayMetrics,
    trackingOverlayView: TrackingOverlayView,
    previewView: PreviewView) = viewModelScope.launch(Dispatchers.Main) {
    // use: prefs.expImagePadding.
    // this setting is experimental anyway. It also has to be read after the preferences are initialized
    val usePadding = false

    detector = DetectorFactory.createDetector(
      assetManager,
      Constants.DETECTION_MODEL,
      Constants.MINIMUM_SCORE,
      usePadding) as YoloV4Detector

    detectionProcessor = DetectionProcessor(
      displayMetrics = displayMetrics,
      detector = detector,
      trackingOverlay = trackingOverlayView)

    while (previewView.childCount == 0) { delay(200) }
    val surfaceView: View = previewView.getChildAt(0)
    while (surfaceView.height == 0) { delay(200) } // BUGFIX

    detectionProcessor.initializeTrackingLayout(
      previewWidth = surfaceView.width,
      previewHeight =  surfaceView.height,
      cropSize = detector.getDetectionModel().inputSize,
      rotation = CAMERA_ROTATION)
  }

  fun imageConvertedIsSetUpped(): Boolean { return imageConverter != null }

  @SuppressLint("UnsafeOptInUsageError")
  fun setUpImageConverter(context: Context, image: ImageProxy) {
    Log.v(TAG, "Image size : ${image.width}x${image.height}")
    imageConverter = RenderScriptImageToBitmapConverter(context, image.image!!)
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

  private fun rotateImage(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
  }


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
  fun getElapsedSecondsStr(): String { return getSecondsPretty(getElapsedSeconds()) }

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
  fun storeDetections(latLong: LatLng) {
    objectsTotal+=objectsWindowUnique
    storedDetections[latLong] = windowDetections.value.orEmpty()
  }

  fun getFloorplan(FH: FloorHelper) = viewModelScope.launch { getFloorplanSafeCall(FH) }

  private fun loadFloorplanFromAsset() {
    LOG.W(TAG, "loading from asset file")
    val base64 = assetReader.getFloorplan64Str()
    val bitmap = base64?.let { ImgUtils.stringToBitmap(it) }
    floorplanResp.value =
      when (bitmap) {
        null -> NetworkResult.Error("Cant read asset deckplan.")
        else -> NetworkResult.Success(bitmap)
      }
  }

  /**
   * Requests a floorplan from remote and publishes outcome to [floorplanResp].
   */
  private suspend fun getFloorplanSafeCall(FH: FloorHelper) {
    floorplanResp.value = NetworkResult.Loading()
    // loadFloorplanFromAsset()
    if (app.hasInternetConnection()) {
      val bitmap = FH.requestRemoteFloorplan()
      if (bitmap != null) {
        floorplanResp.value = NetworkResult.Success(bitmap)
        FH.cacheFloorplan(bitmap)
      } else {
        val msg ="Failed to get ${FH.spaceH.prettyFloorplan}. "
        "Base URL: ${retrofitHolder.retrofit.baseUrl()}"
        LOG.E(msg)
        floorplanResp.value = NetworkResult.Error(msg)
      }
    } else {
      floorplanResp.value = NetworkResult.Error("No Internet Connection.")
    }
  }

  fun addMarker(latLng: LatLng, msg: String) {
    markers?.addCvMarker(latLng, msg)
  }

  fun hideActiveMarkers() {
    markers?.hideActiveMakers()
  }

}