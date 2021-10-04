package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.cv.misc.Constants
import cy.ac.ucy.cs.anyplace.lib.android.cv.misc.DetectionProcessor
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.Detector
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.DetectorFactory
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.YoloV4Detector
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.utils.ImageToBitmapConverter
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.utils.RenderScriptImageToBitmapConverter
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.visualization.TrackingOverlayView
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

enum class Logging {
    started,
    stopped,
    stoppedMustStore
}
class CvLoggerViewModel : ViewModel() {
    companion object {
        /*
        * Use Surface.ROTATION_0 for portrait and Surface.ROTATION_270 for landscape
        */
        const val CAMERA_ROTATION: Int = Surface.ROTATION_0
        var usePadding = false // TODO:PM option
        val LOG_WINDOW = 5000 // TODO:PM option
    }

    // private var detectionProcessor: DetectionProcessor? = null
    private lateinit  var detectionProcessor: DetectionProcessor
    private lateinit var detector: YoloV4Detector
    private var imageConverter: ImageToBitmapConverter? = null // TODO:PM LATE INIT?

    val windowDetections: MutableLiveData<List<Detector.Detection>> = MutableLiveData()
    val status: MutableLiveData<Logging> = MutableLiveData(Logging.stopped)
    var storedDetections: MutableMap<LatLng, List<Detector.Detection>> = mutableMapOf()
    var objectsWindow = 0
    var objectsTotal = 0

    var windowStart : Long = 0
    /** stores the elapsed time on stops/pauses */
    var windowElapsedPause : Long = 0
    var currentTime : Long = 0
    var firstDetection = false

    fun isLogging() : Boolean { return (status.value == Logging.started) }

    fun canStoreObjects() : Boolean {
        return (status.value == Logging.started) ||
            (status.value == Logging.stoppedMustStore)
    }

    fun setUpDetectionProcessor(
        assetManager: AssetManager,
        displayMetrics: DisplayMetrics,
        trackingOverlayView: TrackingOverlayView,
        previewView: PreviewView) = viewModelScope.launch(Dispatchers.Main) {

        detector = DetectorFactory.createDetector(
            assetManager,
            Constants.DETECTION_MODEL,
            Constants.MINIMUM_SCORE) as YoloV4Detector

        detectionProcessor = DetectionProcessor(
            displayMetrics = displayMetrics,
            detector = detector,
            trackingOverlay = trackingOverlayView,)

        while (previewView.childCount == 0) { delay(200) }

        val surfaceView: View = previewView.getChildAt(0)
        detectionProcessor.initializeTrackingLayout(
            previewWidth = surfaceView.width,
            previewHeight = surfaceView.height,
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
        } else {
            // LOG.W("clearing objects..")
            // TODO:PM process last objects

            // Clear objects
            detectionProcessor.clearObjects()
        }
        return 0
    }

    private fun updateDetections(detections: List<Detector.Detection>) {
        currentTime = System.currentTimeMillis()
        LOG.V5(TAG, "updateDetections: ${status.value}")

        // append detections
        val newDetections = windowDetections.value.orEmpty() + detections

        when {
            firstDetection -> {
                LOG.D("updateDetections: Initing window: $currentTime")
                windowStart = currentTime
                firstDetection=false
                windowDetections.postValue(newDetections)
            }
            status.value == Logging.stoppedMustStore -> {
                windowStart = currentTime
                LOG.E("updateDetections: new window: $currentTime")
            }
            // TODO handle when restarting...
            currentTime-windowStart > LOG_WINDOW -> {
                status.postValue(Logging.stoppedMustStore)

                LOG.D("updateDetections: status: $status objects: ${newDetections.size}")
                // TODO CHECK
                val detectionsDedup = YoloV4Detector.NMS(newDetections, detector.labels)
                windowDetections.postValue(detectionsDedup)
                LOG.D("updateDetections: status: $status objects: ${detectionsDedup.size} (dedup)")
                // TODO only when
                // objectsWindow+=detectionsDedup.size
                // objectsTotal+=objectsWindow
                // XXX store this?
                // currentWindow++
          }
          else -> {
              windowDetections.postValue(newDetections)
          }
        }
    }

    private fun rotateImage(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /** Toggle [status] between stopped (or notStarted), and started.
     *  There will be no effect when in stoppedMustStore mode.
     *
     *  In that case it will wait for the user to store the logging data.
     */
    fun toggleLogging(ctx: Context) {
        when (status.value) {
            Logging.stopped -> {
                status.value = Logging.started
                val now = System.currentTimeMillis()
                windowStart=now-windowElapsedPause
            }
            Logging.started-> {
                status.value = Logging.stopped
                // pause timer:
                val now = System.currentTimeMillis()
                windowElapsedPause = now-windowStart
            }
            else ->  {
               LOG.D(TAG, "toggleLoggingStatus: Ignoring: ${status.value}")
            }
        }
    }

    fun getElapsedSecondsStr(): String {
        val elapsed = (currentTime - windowStart)/1000f
        return "%.1f".format(elapsed) + "s"
    }

    fun resetWindow() {
        objectsWindow=0
        windowDetections.value = emptyList()
        status.value= Logging.stopped
    }

    /**
     * Stores the detections on the [storedDetections], a Hash Map of locations and object fingerprints
     */
    fun storeDetections(latLong: LatLng) {
        objectsTotal+=objectsWindow
        storedDetections[latLong] = windowDetections.value.orEmpty()
        resetWindow()
    }

}