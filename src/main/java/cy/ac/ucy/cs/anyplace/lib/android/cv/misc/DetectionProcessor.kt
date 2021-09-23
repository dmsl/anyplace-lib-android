package cy.ac.ucy.cs.anyplace.lib.android.cv.misc

import android.graphics.*
import android.util.DisplayMetrics
import android.util.Log
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.Detector
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.utils.ImageUtils
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.visualization.MultiBoxTracker
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.visualization.TrackingOverlayView
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import kotlin.system.measureTimeMillis

class DetectionProcessor(
    private val displayMetrics: DisplayMetrics,
    private var detector: Detector,
    private var trackingOverlay: TrackingOverlayView
) {
    private companion object {
        const val SHOW_SCORE: Boolean = true
    }

    private lateinit var tracker: MultiBoxTracker
    private var croppedBitmap: Bitmap? = null
    private var cropToFrameTransform: Matrix? = null

    private val paint: Paint = Paint().also {
        it.color = Color.RED
        it.style = Paint.Style.STROKE
        it.strokeWidth = 2.0f
    }

    fun initializeTrackingLayout(
        previewWidth: Int,
        previewHeight: Int,
        cropSize: Int,
        rotation: Int
    ) {
        LOG.I(TAG, "Camera orientation relative to screen canvas : $rotation")
        LOG.I(TAG, "Initializing with size ${previewWidth}x${previewHeight}")

        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888)

        cropToFrameTransform = ImageUtils.getTransformationMatrix(
            srcWidth = cropSize,
            srcHeight = cropSize,
            dstWidth = previewWidth,
            dstHeight = previewHeight,
            rotation = ((rotation + 2) % 4) * 90
        )

        tracker = MultiBoxTracker(
            displayMetrics,
            previewWidth,
            previewHeight,
            ((rotation + 1) % 4) * 90,
            showScore = SHOW_SCORE
        )
        trackingOverlay.setTracker(tracker)
    }

    fun processImage(bitmap: Bitmap): Long {
        Log.v(TAG, "Running detection on image")
        val detections: List<Detector.Detection>
        val detectionTime: Long = measureTimeMillis {
            detections = detector.runDetection(bitmap)
        }

        Log.v(TAG, "Recognized objects : ${detections.size}")
        val cropCopyBitmap: Bitmap = Bitmap.createBitmap(croppedBitmap!!)
        val canvas = Canvas(cropCopyBitmap)

        for (detection in detections) {
            val boundingBox: RectF = detection.boundingBox
            canvas.drawRect(boundingBox, paint)
            cropToFrameTransform!!.mapRect(boundingBox)
        }

        tracker.trackResults(detections)
        trackingOverlay.postInvalidate()

        return detectionTime
    }

}