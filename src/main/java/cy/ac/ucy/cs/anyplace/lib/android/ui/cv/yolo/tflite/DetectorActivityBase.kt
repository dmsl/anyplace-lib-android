package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite

import android.graphics.*
import android.media.ImageReader.OnImageAvailableListener
import android.os.SystemClock
import android.util.Size
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.legacy_cv_gnk.enums.YoloConstants
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.customview.OverlayView
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.env.BorderedText
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.env.ImageUtils
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.tracking.MultiBoxTracker
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
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
abstract class DetectorActivityBase : CameraActivity(),
        OnImageAvailableListener {
  companion object {
    private const val SAVE_PREVIEW_BITMAP = false // debug option
    private const val MAINTAIN_ASPECT = false
    private val DESIRED_PREVIEW_SIZE = Size(640, 480)
    const val TEXT_SIZE_DIP = 10f
  }

  private lateinit var rgbFrameBitmap: Bitmap
  private lateinit var croppedBitmap: Bitmap
  private lateinit var frameToCropTransform: Matrix
  lateinit var cropCopyBitmap: Bitmap
  private lateinit var borderedText: BorderedText

  private var sensorOrientation: Int? = null
  var lastProcessingTimeMs: Long = 0

  private var computingDetection = false
  private var timestamp: Long = 0
  private var cropToFrameTransform: Matrix? = null

  override fun postResume() {
  }

  // TODO: this method is problematic even for the original project.
  // It should run on an IO Dispatcher. Otherwise the app will lag on boot.
  override fun onPreviewSizeChosen(size: Size?, rotation: Int) {
    if (size == null) return

    // TODO:PMX OPT
    lifecycleScope.launch {
      if (!setupDetector()) {
        Toast.makeText(applicationContext, "Can't set up detector.", Toast.LENGTH_LONG).show()
        Toast.makeText(applicationContext, "Are there any models available?", Toast.LENGTH_LONG).show()
        finish()
      }
      val textSizePx = TypedValue.applyDimension(
              TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP,
              app.resources.displayMetrics)

      borderedText = BorderedText(textSizePx)
      borderedText.setTypeface(Typeface.MONOSPACE)
      VMD.tracker = MultiBoxTracker(this@DetectorActivityBase)
      previewWidth = size.width
      previewHeight = size.height
      sensorOrientation = rotation - screenOrientation

      LOG.V3(TAG_METHOD, "Camera orientation relative to screen canvas: $sensorOrientation")
      LOG.V3(TAG_METHOD, "Initializing at size ${previewWidth}x${previewHeight}")

      val cropSize = VMD.model.inputSize
      rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
      croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888)
      frameToCropTransform = ImageUtils.getTransformationMatrix(
              previewWidth, previewHeight, cropSize, cropSize,
              sensorOrientation!!, MAINTAIN_ASPECT)
      cropToFrameTransform = Matrix()
      frameToCropTransform.invert(cropToFrameTransform)

      // View
      VMD.trackingOverlay = findViewById<View>(R.id.tracking_overlay) as OverlayView
      VMD.trackingOverlay.addCallback { canvas ->
        VMD.tracker.draw(canvas)
        if (isDebug) {
          VMD.tracker.drawDebug(canvas)
        }
      }
      VMD.tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation!!)
    }

    // MERGE: bind values with bottomSheet vars?
  }

  /** Artificial delay between inference calls (in ms) to save battery */
  private var scanDelay: Long = 100
  private var lastScan: Long = 0L

  /**
   * Allowing only one scan in a [scanDelay] window.
   *
   * TODO this has to be adjusted. It's not good enough.
   * We might have to take: 5-6 consecutive scans, every N seconds,
   * and not a single scan every N seconds
   * (we need a few consecutive ones to get a better pic of the environment)
   *
   * We might introduce another mode that will disable scanning also..
   *
   */
  private fun delayPassed(): Boolean {
    val currentTime = System.currentTimeMillis()
    when (lastScan) {
      0L -> { // first scan
        lastScan = currentTime
        return true
      }
      else -> {
        val elapsedTime = currentTime - lastScan
        LOG.V5(TAG, "elapsed: $elapsedTime")
        if (elapsedTime >= scanDelay) {
          lastScan = currentTime
          return true
        }
        // dropping frame
        return false
      }
    }
  }

  suspend fun setupDetector(): Boolean {
    LOG.I()
    try {
      // Read DS Preferences:
      val prefsCv = VMD.dsCv.read.first()
      VMD.setModel(prefsCv.modelName)
      LOG.D(TAG, "setupDetector: calls read")
      val prefsCvNav = VMD.dsCvNav.read.first()
      scanDelay = prefsCvNav.scanDelay.toLong()

      VMD.detector = YoloV4Classifier.create(
              assets,
              VMD.model.filename,
              VMD.model.labelFilePath,
              VMD.model.isQuantized)
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
    VMD.trackingOverlay.postInvalidate()

    // No mutex needed as this method is not reentrant.
    if (!delayPassed()) {
      LOG.V2(TAG, "$METHOD: Skipping inference.. (delay)")
      readyForNextImage()
      return
    }

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

    // No mutex needed as this method is not reentrant.
    // PMX: BUG
    if (!VMD.isDetecting() ) {
      LOG.V2(TAG, "$METHOD: Skipping inference.. (disabled)")
      if (DBG.CTR) skipDetection() // TODO: PMX: CTR
      return
    }

    LOG.V2(TAG_METHOD, "RUNNING INFERENCE..")

    // For examining the actual TF input.
    if (SAVE_PREVIEW_BITMAP) {
      ImageUtils.saveBitmap(croppedBitmap)
    }

    lifecycleScope.launch(Dispatchers.IO) {
      runDetection(currTimestamp)
      onProcessImageFinished()
    }
  }

  /**
   * Draw a an empty canvas:
   *
   * Similar to [runDetection], but instead it skips running inference
   * It draws an empty canvas (without any detections in it)
   *
   * CLR:PM
   */
  private fun skipDetection() {
    LOG.V4(TAG_METHOD, "Skip detection on image")
    val canvas = Canvas(croppedBitmap)
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null)

    cropCopyBitmap = Bitmap.createBitmap(croppedBitmap)

    // Flows here
    VMD.tracker.clear()
    VMD.trackingOverlay.postInvalidate()
    computingDetection = false
  }

  private fun runDetection(currTimestamp: Long) {
    LOG.V4(TAG_METHOD, "Running detection on image $currTimestamp")
    var canvas = Canvas(croppedBitmap)
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null)

    val startTime = SystemClock.uptimeMillis()
    val results = VMD.detector.recognizeImage(croppedBitmap)
    lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime

    LOG.V3(TAG_METHOD, "Detections: ${results.size}")
    results.forEach { rec ->
      LOG.V3(TAG, "Detection: ${rec.detectedClass} ${rec.title}")
    }

    cropCopyBitmap = Bitmap.createBitmap(croppedBitmap)
    canvas = Canvas(cropCopyBitmap)

    // Flows here
    val detections = storeResults(results, canvas)
    onInferenceRan(detections)

    // finalize detection:
    // View
    VMD.tracker.trackResults(detections, currTimestamp)
    VMD.trackingOverlay.postInvalidate()
    computingDetection = false
  }

  private val MODE = DetectorMode.TF_OD_API

  // Which detection model to use:
  // by default uses Tensorflow Object Detection API frozen checkpoints.
  private enum class DetectorMode { TF_OD_API }

  // public fun drawEmptyCanvas(canvas: Canvas) {
  //   cropCopyBitmap = Bitmap.createBitmap(croppedBitmap)
  //   storeResults(emptyList(), cropCopyBitmap)
  // }


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
      if (result.confidence >= minimumConfidence) {
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
    lifecycleScope.launch(Dispatchers.IO) { VMD.detector.setUseNNAPI(isChecked) }
  }

  override fun setNumThreads(numThreads: Int) {
    lifecycleScope.launch(Dispatchers.IO) { VMD.detector.setNumThreads(numThreads) }
  }

  override fun onInferenceRan(detections: MutableList<Classifier.Recognition>) {
    LOG.D2(TAG, "$METHOD: DetectorBaseActivity")
    // LOG.V3(TAG, "$METHOD: detections: $detections.size")
  }
}