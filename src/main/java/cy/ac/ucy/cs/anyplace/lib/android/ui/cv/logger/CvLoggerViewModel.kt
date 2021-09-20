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
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.cv.misc.Constants
import cy.ac.ucy.cs.anyplace.lib.android.cv.misc.DetectionProcessor
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.DetectorFactory
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.utils.ImageToBitmapConverter
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.utils.RenderScriptImageToBitmapConverter
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.visualization.TrackingOverlayView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

class CvLoggerViewModel : ViewModel() {

    companion object {
        private const val TAG: String = "DetectorViewModel"

        /*
        * Use Surface.ROTATION_0 for portrait and Surface.ROTATION_270 for landscape
        */
        const val CAMERA_ROTATION: Int = Surface.ROTATION_0
        var usePadding = false
    }

    private var detectionProcessor: DetectionProcessor? = null

    private var imageConverter: ImageToBitmapConverter? = null


    fun setUpDetectionProcessor(
        assetManager: AssetManager,
        displayMetrics: DisplayMetrics,
        trackingOverlayView: TrackingOverlayView,
        previewView: PreviewView
    ) = viewModelScope.launch(Dispatchers.Main) {
        val detector = DetectorFactory.createDetector(
            assetManager,
            Constants.DETECTION_MODEL,
            Constants.MINIMUM_SCORE
        )

        detectionProcessor = DetectionProcessor(
            displayMetrics = displayMetrics,
            detector = detector,
            trackingOverlay = trackingOverlayView,
        )

        while (previewView.childCount == 0) {
            delay(200)
        }

        val surfaceView: View = previewView.getChildAt(0)
        detectionProcessor!!.initializeTrackingLayout(
            previewWidth = surfaceView.width,
            previewHeight = surfaceView.height,
            cropSize = detector.getDetectionModel().inputSize,
            rotation = CAMERA_ROTATION
        )
    }

    fun imageConvertedIsSetUpped(): Boolean {
        return imageConverter != null
    }

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
        LOG.V3(TAG, "Conversion time : $conversionTime ms")

        // LEFTHERE .. modify image earlier..
        // LEFTHERE .. modify image earlier..
        // LEFTHERE .. modify image earlier..
        val detectionTime: Long = detectionProcessor!!.processImage(bitmap)
        LOG.V1(TAG, "Detection time : $detectionTime ms")

        val processingTime = conversionTime + detectionTime
        LOG.V2(TAG, "Analysis time : $processingTime ms")
        return detectionTime
    }

    private fun rotateImage(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

}