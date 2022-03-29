package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite

import android.graphics.*
import android.media.ImageReader.OnImageAvailableListener
import android.os.SystemClock
import android.util.Size
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.cv.enums.DetectionModel
import cy.ac.ucy.cs.anyplace.lib.android.cv.enums.YoloConstants
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.customview.OverlayView
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.env.BorderedText
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.env.ImageUtils
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.tracking.MultiBoxTracker
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.DetectorViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*

/**
 * This used to be the DetectorActivity from:
 * - https://github.com/hunglc007/tensorflow-yolov4-tflite
 *
 * Now it is an abstract that has to be extended.
 * It contains **only** whatever is related with detection.
 *
 * Things like BottomSheet were removed.
 *
 * -------------------------------
 *
 * TODO: review the below comments:
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
@AndroidEntryPoint
abstract class DetectorActivityBase() : CameraActivity(),
        OnImageAvailableListener {
  companion object {
    private const val SAVE_PREVIEW_BITMAP = false // debug option
    private const val MAINTAIN_ASPECT = false
    private val DESIRED_PREVIEW_SIZE = Size(640, 480)
    const val TEXT_SIZE_DIP = 10f
  }

  abstract val view_model_class: Class<DetectorViewModel>
  protected lateinit var _vm: ViewModel
  private lateinit var VM: DetectorViewModel

  private lateinit var rgbFrameBitmap: Bitmap
  private lateinit var croppedBitmap: Bitmap
  private lateinit var frameToCropTransform: Matrix
  lateinit var cropCopyBitmap: Bitmap
  lateinit var trackingOverlay: OverlayView
  private lateinit var borderedText: BorderedText

  private var sensorOrientation: Int? = null
  var lastProcessingTimeMs: Long = 0

  private var computingDetection = false
  private var timestamp: Long = 0
  private var cropToFrameTransform: Matrix? = null

  private var tracker: MultiBoxTracker? = null


  override fun postCreate() {
    _vm = ViewModelProvider(this).get(view_model_class)
    VM = _vm as DetectorViewModel
  }


  // TODO: this method is problematic even for the original project.
  // It should run on an IO Dispatcher. Otherwise the app will lag on boot.
  override fun onPreviewSizeChosen(size: Size?, rotation: Int) {
    if (size==null) return

    lifecycleScope.launch {
      if(!setupDetector()) {
        val toast = Toast.makeText(applicationContext, "Can't set up detector.",Toast.LENGTH_LONG)
        toast.show()
        finish()
      }

      val textSizePx = TypedValue.applyDimension(
              TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP,
              app.resources.displayMetrics)

      borderedText = BorderedText(textSizePx)
      borderedText.setTypeface(Typeface.MONOSPACE)
      tracker = MultiBoxTracker(this@DetectorActivityBase)
      previewWidth = size.width
      previewHeight = size.height
      sensorOrientation = rotation - screenOrientation

      LOG.V3(TAG_METHOD, "Camera orientation relative to screen canvas: $sensorOrientation")
      LOG.V3(TAG_METHOD, "Initializing at size ${previewWidth}x${previewHeight}")

      val cropSize = VM.model.inputSize
      rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
      croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888)
      frameToCropTransform = ImageUtils.getTransformationMatrix(
              previewWidth, previewHeight, cropSize, cropSize,
              sensorOrientation!!, MAINTAIN_ASPECT)
      cropToFrameTransform = Matrix()
      frameToCropTransform.invert(cropToFrameTransform)

      // View
      trackingOverlay = findViewById<View>(R.id.tracking_overlay) as OverlayView
      trackingOverlay.addCallback { canvas ->
        tracker!!.draw(canvas)
        if (isDebug) { tracker!!.drawDebug(canvas)  }
      }
      tracker!!.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation!!)
    }
  }

  suspend fun setupDetector(): Boolean {
    LOG.I()
    try {

     val modelName = VM.dsCv.read.first().modelName
      VM.setModel(modelName)

      VM.detector = YoloV4Classifier.create(
              assets,
              VM.model.modelFilename,
              VM.model.labelFilePath,
              VM.model.isQuantized)
    } catch (e: IOException) {
      val msg = "Cant initialize classifier"
      LOG.E(TAG_METHOD, msg, e)
      return false
    }
    return true
  }

  override fun processImage() {
    // ViewModel
    ++timestamp
    trackingOverlay.postInvalidate()

    // No mutex needed as this method is not reentrant.
    if (computingDetection) {
      readyForNextImage()
      return
    }

    val currTimestamp = timestamp
    computingDetection = true
    LOG.V3(TAG_METHOD, "Preparing image $currTimestamp for detection in bg thread.")
    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight)

    readyForNextImage()

    // For examining the actual TF input.
    if (SAVE_PREVIEW_BITMAP) { ImageUtils.saveBitmap(croppedBitmap) }

    lifecycleScope.launch(Dispatchers.IO) {
      runDetection(currTimestamp)
      onProcessImageFinished()
    }
  }

  private fun runDetection(currTimestamp: Long) {
    LOG.V4(TAG_METHOD, "Running detection on image $currTimestamp")
    var canvas = Canvas(croppedBitmap)
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null)

    val startTime = SystemClock.uptimeMillis()
    val results = VM.detector.recognizeImage(croppedBitmap)
    lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime

    LOG.V3(TAG_METHOD, "Detections: ${results?.size}")
    cropCopyBitmap = Bitmap.createBitmap(croppedBitmap)
    canvas = Canvas(cropCopyBitmap)

    // Flows here
    val detections = storeResults(results, canvas)

    // finalize detection:
    // View
    tracker!!.trackResults(detections, currTimestamp)
    trackingOverlay.postInvalidate()
    computingDetection = false
  }

  private val MODE = DetectorMode.TF_OD_API
  // Which detection model to use:
  // by default uses Tensorflow Object Detection API frozen checkpoints.
  private enum class DetectorMode { TF_OD_API }

  fun storeResults(results: List<Classifier.Recognition>, canvas: Canvas):
          MutableList<Classifier.Recognition> {
    val minScore = YoloConstants.MINIMUM_SCORE
    val minimumConfidence: Float = when (MODE) {
      DetectorMode.TF_OD_API -> minScore
    }

    val paint = Paint()
    paint.color = Color.RED
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 2.0f

    val mappedRecognitions: MutableList<Classifier.Recognition> = LinkedList()

    for (result in results) {
      val location = result.location
      if (location != null && result.confidence!! >= minimumConfidence) {
        canvas.drawRect(location, paint)
        cropToFrameTransform!!.mapRect(location)
        result.location = location
        mappedRecognitions.add(result)
      }
    }
    return mappedRecognitions
  }

  override val layout_camera_fragment: Int
    get() = R.layout.tfe_od_camera_connection_fragment_tracking

  override val desiredPreviewFrameSize: Size?
    get() = DESIRED_PREVIEW_SIZE

  override fun setUseNNAPI(isChecked: Boolean) {
    lifecycleScope.launch(Dispatchers.IO) { VM.detector.setUseNNAPI(isChecked) }
  }

  override fun setNumThreads(numThreads: Int) {
    lifecycleScope.launch(Dispatchers.IO) { VM.detector.setNumThreads(numThreads) }
  }
}