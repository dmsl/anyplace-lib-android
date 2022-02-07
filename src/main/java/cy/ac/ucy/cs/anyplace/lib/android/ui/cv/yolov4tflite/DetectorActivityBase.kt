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
import cy.ac.ucy.cs.anyplace.lib.android.cv.enums.YoloConstants
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
 * This used to be the DetectorActivity from:
 * - https://github.com/hunglc007/tensorflow-yolov4-tflite
 *
 * Now it is an abstract that has to be extended.
 * The below must be provided:
 * - TODO
 *
 * The changes from the original code:
 * - It is converted to Kotlin
 * - TODO It is using ViewBinding
 * - TODO It is using a ViewModel (MVVM):
 *   - storing the detections
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
 * ORIGINAL DOC:
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
abstract class DetectorActivityBase : CameraActivity(), OnImageAvailableListener {
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
  private lateinit var detector: Classifier

  var trackingOverlay: OverlayView? = null
  private var sensorOrientation: Int? = null
  private var lastProcessingTimeMs: Long = 0

  private var computingDetection = false
  private var timestamp: Long = 0
  private var cropToFrameTransform: Matrix? = null
  private var tracker: MultiBoxTracker? = null
  private lateinit var borderedText: BorderedText
  val model = YoloConstants.DETECTION_MODEL

  override fun onPreviewSizeChosen(size: Size?, rotation: Int) {

   setupDetector()

    // TODO setupUI()

    if (size==null) return;

    // ViewModel
    val textSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, resources.displayMetrics)
    borderedText = BorderedText(textSizePx)
    borderedText.setTypeface(Typeface.MONOSPACE)
    tracker = MultiBoxTracker(this)
    previewWidth = size.width
    previewHeight = size.height
    sensorOrientation = rotation - screenOrientation

    LOG.V(TAG_METHOD, "Camera orientation relative to screen canvas: $sensorOrientation")
    LOG.V(TAG_METHOD, "Initializing at size ${previewWidth}x${previewHeight}")

    val cropSize = model.inputSize
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
    croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888)
    frameToCropTransform = ImageUtils.getTransformationMatrix(
            previewWidth, previewHeight, cropSize, cropSize,
            sensorOrientation!!, MAINTAIN_ASPECT)
    cropToFrameTransform = Matrix()
    frameToCropTransform.invert(cropToFrameTransform)

    // View
    trackingOverlay = findViewById<View>(R.id.tracking_overlay) as OverlayView
    trackingOverlay!!.addCallback { canvas ->
      tracker!!.draw(canvas)
      if (isDebug) { tracker!!.drawDebug(canvas)  }
    }
    tracker!!.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation!!)
  }

  override fun processImage() {
    // ViewModel
    ++timestamp
    val currTimestamp = timestamp
    trackingOverlay!!.postInvalidate()

    // No mutex needed as this method is not reentrant.
    if (computingDetection) {
      readyForNextImage()
      return
    }
    computingDetection = true
    LOG.V3(TAG_METHOD, "Preparing image $currTimestamp for detection in bg thread.")
    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight)
    readyForNextImage()
    var canvas = Canvas(croppedBitmap)
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null)

    // For examining the actual TF input.
    if (SAVE_PREVIEW_BITMAP) {  ImageUtils.saveBitmap(croppedBitmap)  }

    lifecycleScope.launch(Dispatchers.IO) {
      LOG.V4(TAG_METHOD, "Running detection on image $currTimestamp")
      val startTime = SystemClock.uptimeMillis()
      val results = detector.recognizeImage(croppedBitmap)
      lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime
      LOG.V3(TAG_METHOD, "Detections: ${results.size}")
      cropCopyBitmap = Bitmap.createBitmap(croppedBitmap)
      canvas = Canvas(cropCopyBitmap)
      val paint = Paint()
      paint.color = Color.RED
      paint.style = Paint.Style.STROKE
      paint.strokeWidth = 2.0f

      val minScore = YoloConstants.MINIMUM_SCORE
      val minimumConfidence: Float = when (MODE) {
        DetectorMode.TF_OD_API -> minScore
      }

      // Flows here
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

      // View
      tracker!!.trackResults(mappedRecognitions, currTimestamp)
      trackingOverlay!!.postInvalidate()
      computingDetection = false

      lifecycleScope.launch(Dispatchers.Main) {
        // TODO runs after detection?
        showFrameInfo(previewWidth.toString() + "x" + previewHeight)
        val w = cropCopyBitmap.width
        val h = cropCopyBitmap.height
        showCropInfo("${w}x${h}")
        showInference(lastProcessingTimeMs.toString() + "ms")
      }
    }
  }

  private fun setupDetector() {
    LOG.I()
    try {
      detector = YoloV4Classifier.create(
              assets,
              model.modelFilename,
              model.labelFilePath,
              model.isQuantized)
    } catch (e: IOException) {
      val msg  ="Cant initialize classifier"
      LOG.E(TAG_METHOD, msg, e)
      val toast = Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT)
      toast.show()
      finish()
    }
  }

  override val layout_camera_fragment: Int
    get() = R.layout.tfe_od_camera_connection_fragment_tracking

  override val desiredPreviewFrameSize: Size?
    get() = DESIRED_PREVIEW_SIZE

  // Which detection model to use:
  // by default uses Tensorflow Object Detection API frozen checkpoints.
  private enum class DetectorMode { TF_OD_API }

  override fun setUseNNAPI(isChecked: Boolean) {
    runInBackground { detector.setUseNNAPI(isChecked) }
  }

  override fun setNumThreads(numThreads: Int) {
    runInBackground { detector.setNumThreads(numThreads) }
  }
}