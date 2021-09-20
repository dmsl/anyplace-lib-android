package cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.os.SystemClock
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import org.tensorflow.lite.Interpreter
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.Detector.Detection
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.enums.DetectionModel
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerViewModel
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.*
import kotlin.math.max
import kotlin.math.min

@Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
internal class YoloV4Detector(
    assetManager: AssetManager,
    private val detectionModel: DetectionModel,
    private val minimumScore: Float,
) : Detector {

    private companion object {
        // TODO:PM options
        const val NUM_THREADS = 4
        const val IS_GPU: Boolean = false
        const val IS_NNAPI: Boolean = false
    }

    private val inputSize: Int = detectionModel.inputSize

    // Config values.
    private val labels: List<String>
    private val interpreter: Interpreter
    private val nmsThresh = 0.6f

    // Pre-allocated buffers.
    private val intValues = IntArray(inputSize * inputSize)
    private val byteBuffer: Array<ByteBuffer>
    private val outputMap: MutableMap<Int, Array<Array<FloatArray>>> = HashMap()

    init {
        val labelsFilename = detectionModel.labelFilePath
            .split("file:///android_asset/")
            .toTypedArray()[1]

        labels = assetManager.open(labelsFilename)
            .use { it.readBytes() }
            .decodeToString()
            .trim()
            .split("\n")
            .map { it.trim() }

        interpreter = initializeInterpreter(assetManager)

        val numBytesPerChannel = if (detectionModel.isQuantized) {
            1 // Quantized (int8)
        } else {
            4 // Floating point (fp32)
        }

        // input size * input size * pixel count (RGB) * pixel size (int8/fp32)
        byteBuffer = arrayOf(
            ByteBuffer.allocateDirect(inputSize * inputSize * 3 * numBytesPerChannel)
        )
        byteBuffer[0].order(ByteOrder.nativeOrder())

        outputMap[0] = arrayOf(Array(detectionModel.outputSize) { FloatArray(numBytesPerChannel) })
        outputMap[1] = arrayOf(Array(detectionModel.outputSize) { FloatArray(labels.size) })
    }

    override fun getDetectionModel(): DetectionModel {
        return detectionModel
    }

    private var usePadding : Boolean = false

    override fun runDetection(bitmap: Bitmap): List<Detection> {
        usePadding = CvLoggerViewModel.usePadding
        convertBitmapToByteBuffer(bitmap)
        val result = getDetections(bitmap.width, bitmap.height)
        return nms(result)
    }

    private fun initializeInterpreter(assetManager: AssetManager): Interpreter {
        val options = Interpreter.Options()
        options.setNumThreads(NUM_THREADS)

        when {
            IS_GPU -> {
                options.addDelegate(GpuDelegate())
            }
            IS_NNAPI -> {
                options.setUseNNAPI(true)
            }
            else -> {
                options.setUseXNNPACK(true)
            }
        }

        return assetManager.openFd(detectionModel.modelFilename).use { fileDescriptor ->
            val fileInputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileByteBuffer = fileInputStream.channel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            )

            return@use Interpreter(fileByteBuffer, options)
        }
    }

    /**
     * Writes Image data into a [ByteBuffer].
     */
    private fun convertBitmapToByteBuffer(bitmap: Bitmap) {
        val startTime = SystemClock.uptimeMillis()
        var scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        if (usePadding && bitmap.height > bitmap.width) { // add a white padding, left and right
            val padding : Float = (bitmap.height-bitmap.width)/2f
            val paddedBitmap = bitmap.pad(padding)
            scaledBitmap = Bitmap.createScaledBitmap(paddedBitmap, inputSize, inputSize, true)
            LOG.D1("Bitmap: ${bitmap.width}x${bitmap.height}  scaled: ${scaledBitmap.width}x${scaledBitmap.height}" )
        }

        scaledBitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)
        scaledBitmap.recycle()
        // 416 x 416 (from: 480 x 864

        byteBuffer[0].clear()
        for (pixel in intValues) {
            val r = (pixel and 0xFF) / 255.0f
            val g = (pixel shr 8 and 0xFF) / 255.0f
            val b = (pixel shr 16 and 0xFF) / 255.0f

            byteBuffer[0].putFloat(r)
            byteBuffer[0].putFloat(g)
            byteBuffer[0].putFloat(b)
        }
        LOG.V3(TAG, "ByteBuffer conversion time : ${SystemClock.uptimeMillis() - startTime}ms")
    }

    fun Bitmap.pad(left: Float): Bitmap {
        val outputimage = Bitmap.createBitmap(
            (width + left).toInt(), height, Bitmap.Config.ARGB_8888)
        val can = Canvas(outputimage)
        can.drawBitmap(this, left, 0f, null)

        val output = Bitmap.createBitmap((outputimage.width +left).toInt(),
            (outputimage.height).toInt(), Bitmap.Config.ARGB_8888)
        var canvas = Canvas(output)
        canvas.drawBitmap(outputimage, 0f, 0f, null)
        return output
    }

    /**
     * Yolo operates on normalized coordinates (positions and dimensions are expressed in percent's).
     * This method translates YOLO bounding boxes to Android Rect
     */
    private fun getDetections(imageWidth: Int, imageHeight: Int): List<Detection> {
        interpreter.runForMultipleInputsOutputs(byteBuffer, outputMap as Map<Int, Any>)

        var ratio = 0f
        var ratioQ= 0f
        if (usePadding && imageHeight>imageWidth) {
            ratio= (imageHeight.toFloat()/imageWidth)
            ratioQ = 1+((ratio-1)/8f)
            LOG.V5("ratioQ: $ratioQ")
        }

        val boundingBoxes = outputMap[0]!![0]
        val outScore = outputMap[1]!![0]

        return outScore.zip(boundingBoxes)
            .mapIndexedNotNull { index, (classScores, boundingBoxes) ->
                val bestClassIndex: Int = labels.indices.maxByOrNull { classScores[it] }!!
                val bestScore = classScores[bestClassIndex]

                if (bestScore <= minimumScore) {
                    return@mapIndexedNotNull null
                }

                var xPos = boundingBoxes[0]
                val yPos = boundingBoxes[1]
                var width = boundingBoxes[2]
                val height = boundingBoxes[3]

                if (usePadding && ratio > 1.0f) {
                    width*= ratio
                    xPos*=ratioQ
                }

                val left = max(0f, xPos - width / 2)
                val top = max(0f, yPos - (height / 2))
                val right = min(imageWidth - 1.toFloat(), (xPos + width / 2))
                val bottom = min(imageHeight - 1.toFloat(), yPos + height / 2)

                val rectF = RectF(left, top, right, bottom)

                var label =labels[bestClassIndex]
                if(usePadding) {  label+="_PAD" }

                return@mapIndexedNotNull Detection(
                    id = index.toString(),
                    className = label,
                    detectedClass = bestClassIndex,
                    score = bestScore,
                    boundingBox = rectF
                )
            }
    }

    /**
     * Non-maximum Suppression. Removes overlapping bboxes, which is a
     * side-effect of YOLO's operation.
     */
    private fun nms(detections: List<Detection>): List<Detection> {
        val nmsList: MutableList<Detection> = mutableListOf()

        for (labelIndex in labels.indices) {
            val priorityQueue = PriorityQueue<Detection>(50)
            priorityQueue.addAll(detections.filter { it.detectedClass == labelIndex })

            while (priorityQueue.size > 0) {
                val previousPriorityQueue: List<Detection> = priorityQueue.toList()
                val max = previousPriorityQueue[0]
                nmsList.add(max)
                priorityQueue.clear()
                priorityQueue.addAll(previousPriorityQueue.filter {
                    boxIoU(max.boundingBox, it.boundingBox) < nmsThresh
                })
            }
        }

        return nmsList
    }

    private fun boxIoU(a: RectF, b: RectF): Float {
        return boxIntersection(a, b) / boxUnion(a, b)
    }

    private fun boxIntersection(a: RectF, b: RectF): Float {
        val w = overlap(
            (a.left + a.right) / 2,
            a.right - a.left,
            (b.left + b.right) / 2,
            b.right - b.left
        )

        val h = overlap(
            (a.top + a.bottom) / 2,
            a.bottom - a.top,
            (b.top + b.bottom) / 2,
            b.bottom - b.top
        )

        return if (w < 0F || h < 0F) 0F else w * h
    }

    private fun boxUnion(a: RectF, b: RectF): Float {
        val i = boxIntersection(a, b)
        return (a.right - a.left) * (a.bottom - a.top) + (b.right - b.left) * (b.bottom - b.top) - i
    }

    private fun overlap(x1: Float, width1: Float, x2: Float, width2: Float): Float {
        val left1 = x1 - width1 / 2
        val left2 = x2 - width2 / 2
        val left = max(left1, left2)

        val right1 = x1 + width1 / 2
        val right2 = x2 + width2 / 2
        val right = min(right1, right2)

        return right - left
    }

}