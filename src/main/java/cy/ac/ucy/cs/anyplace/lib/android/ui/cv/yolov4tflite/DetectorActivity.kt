package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite

import android.graphics.*
import android.media.ImageReader.OnImageAvailableListener
import android.os.SystemClock
import android.util.Size
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.cv.misc.YoloConstants
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite.customview.OverlayView
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite.env.BorderedText
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite.env.ImageUtils
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite.tracking.MultiBoxTracker
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite.Classifier.Recognition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import java.io.IOException
import java.util.*

/**
 *
 * TODO:
 * - COMMON:
 *   - bottom sheet
 *   - settings button
 *   - setup those from here
 *
 *  - BaseClass:
 *   - CvMapActivity:
 *    - COMMON:
 *      - floor picker, etc..
 *
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
class DetectorActivity : CameraActivity(), OnImageAvailableListener {
  companion object {
    private val MODE = DetectorMode.TF_OD_API
    private const val MAINTAIN_ASPECT = false
    private val DESIRED_PREVIEW_SIZE = Size(640, 480)
    private const val SAVE_PREVIEW_BITMAP = false
    private const val TEXT_SIZE_DIP = 10f
  }

  private lateinit var rgbFrameBitmap: Bitmap
  private lateinit var croppedBitmap: Bitmap
  private lateinit var frameToCropTransform: Matrix
  private lateinit var cropCopyBitmap: Bitmap

  var trackingOverlay: OverlayView? = null
  private var sensorOrientation: Int? = null
  private var detector: Classifier? = null
  private var lastProcessingTimeMs: Long = 0

  private var computingDetection = false
  private var timestamp: Long = 0
  private var cropToFrameTransform: Matrix? = null
  private var tracker: MultiBoxTracker? = null
  private var borderedText: BorderedText? = null


  public override fun onPreviewSizeChosen(size: Size, rotation: Int) {
    //ViewModel
    val textSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, resources.displayMetrics
    )
    borderedText = BorderedText(textSizePx)
    borderedText!!.setTypeface(Typeface.MONOSPACE)
    tracker = MultiBoxTracker(this)

    val model = YoloConstants.DETECTION_MODEL
    val cropSize = model.inputSize

    try {
      detector = YoloV4Classifier.create(
              assets,
              model.modelFilename,
              model.labelFilePath,
              model.isQuantized)
    } catch (e: IOException) {
      e.printStackTrace()
      LOG.E(TAG_METHOD, "Initializing classifier", e)
      val toast = Toast.makeText(
              applicationContext, "Classifier could not be initialized", Toast.LENGTH_SHORT
      )
      toast.show()
      finish()
    }

    previewWidth = size.width
    previewHeight = size.height
    sensorOrientation = rotation - screenOrientation
    LOG.I(TAG_METHOD, "Camera orientation relative to screen canvas: $sensorOrientation")
    LOG.I(TAG_METHOD, "Initializing at size ${previewWidth}x${previewHeight}")
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
    croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888)
    frameToCropTransform = ImageUtils.getTransformationMatrix(
            previewWidth, previewHeight,
            cropSize, cropSize,
            sensorOrientation!!, MAINTAIN_ASPECT
    )
    cropToFrameTransform = Matrix()
    frameToCropTransform.invert(cropToFrameTransform)

    //View
    trackingOverlay = findViewById<View>(R.id.tracking_overlay) as OverlayView
    trackingOverlay!!.addCallback { canvas ->
      tracker!!.draw(canvas)
      if (isDebug) { tracker!!.drawDebug(canvas)  }
    }
    tracker!!.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation!!)
  }

  override fun processImage() {
    //ViewModel
    ++timestamp
    val currTimestamp = timestamp
    trackingOverlay!!.postInvalidate()

    // No mutex needed as this method is not reentrant.
    if (computingDetection) {
      readyForNextImage()
      return
    }
    computingDetection = true
    LOG.I("Preparing image $currTimestamp for detection in bg thread.")
    rgbFrameBitmap.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight)
    readyForNextImage()
    val canvas = Canvas(croppedBitmap)
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null)

    // For examining the actual TF input.
    if (SAVE_PREVIEW_BITMAP) {
      ImageUtils.saveBitmap(croppedBitmap)
    }

    // TODO COROUTINE: run on IO thread
    lifecycleScope.launch(Dispatchers.IO) {
      // delay(1000) // TODO: REMOVE

      // runInBackground {
      LOG.I("Running detection on image $currTimestamp")
      val startTime = SystemClock.uptimeMillis()
      val results = detector!!.recognizeImage(croppedBitmap)
      lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime
      LOG.E("CHECK", "run: " + results.size)
      cropCopyBitmap = Bitmap.createBitmap(croppedBitmap)
      val canvas = Canvas(cropCopyBitmap)
      val paint = Paint()
      paint.color = Color.RED
      paint.style = Paint.Style.STROKE
      paint.strokeWidth = 2.0f

      val minScore = YoloConstants.MINIMUM_SCORE
      val minimumConfidence: Float = when (MODE) {
        DetectorMode.TF_OD_API -> minScore
      }

      //Flows here
      val mappedRecognitions: MutableList<Recognition> =LinkedList()
      for (result in results) {
        val location = result.location
        if (location != null && result.confidence >= minimumConfidence) {
          canvas.drawRect(location, paint)
          cropToFrameTransform!!.mapRect(location)
          result.location = location
          mappedRecognitions.add(result)
        }
      }

      //View
      tracker!!.trackResults(mappedRecognitions, currTimestamp)
      trackingOverlay!!.postInvalidate()
      computingDetection = false
      runOnUiThread {
        showFrameInfo(previewWidth.toString() + "x" + previewHeight)

        showCropInfo(
                cropCopyBitmap?.getWidth().toString() + "x" + cropCopyBitmap?.getHeight()
        )
        showInference(lastProcessingTimeMs.toString() + "ms")
      }
      // }
    }

  }

  override fun getLayoutId(): Int {
    return R.layout.tfe_od_camera_connection_fragment_tracking
  }

  override fun getDesiredPreviewFrameSize(): Size {
    return DESIRED_PREVIEW_SIZE
  }

  // Which detection model to use: by default uses Tensorflow Object Detection API frozen
  // checkpoints.
  private enum class DetectorMode {
    TF_OD_API
  }

  override fun setUseNNAPI(isChecked: Boolean) {
    runInBackground { detector!!.setUseNNAPI(isChecked) }
  }

  override fun setNumThreads(numThreads: Int) {
    runInBackground { detector!!.setNumThreads(numThreads) }
  }
}