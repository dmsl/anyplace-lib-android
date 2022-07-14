/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/
package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite

import android.content.Context
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.Classifier.Recognition
import android.graphics.RectF
import android.graphics.Bitmap
import kotlin.Throws
import android.content.res.AssetManager
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.env.Utils
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.IOException
import java.lang.Exception
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.math.sqrt

/**
 * Wrapper for frozen detection models trained using the Tensorflow Object Detection API:
 * - https://github.com/tensorflow/models/tree/master/research/object_detection
 * where you can find the training code.
 *
 *
 * To use pretrained models in the API or convert to TF Lite models, please see docs for details:
 * - https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/detection_model_zoo.md
 * - https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/running_on_mobile_tensorflowlite.md#running-our-model-on-android
 */
open class YoloV4Classifier private constructor()
  : Classifier {
  companion object {

    /**
     * Initializes a native TensorFlow session for classifying images.
     *
     * @param assetManager  The asset manager to be used to load assets.
     * @param modelFilename The filepath of the model GraphDef protocol buffer.
     * @param labelFilename The filepath of label file for classes.
     * @param isQuantized   Boolean representing model is quantized or not
     */
    @Throws(IOException::class)
    fun create(
            ctx: Context,
            assetManager: AssetManager,
            modelFilename: String?,
            labelFilename: String,
            isQuantized: Boolean
    ): Classifier {
      val d = YoloV4Classifier()
      
      d.ctx=ctx

      val labelsFilename = labelFilename
              .split("file:///android_asset/")
              .toTypedArray()[1]
      d.labels = assetManager.open(labelsFilename)
              .use { it.readBytes() }
              .decodeToString()
              .trim()
              .split("\n")
              .map { it.trim() }

      try {
        val options = Interpreter.Options()
        options.setNumThreads(NUM_THREADS)
        if (isNNAPI) {
          var nnApiDelegate: NnApiDelegate? = null
          // Initialize interpreter with NNAPI delegate for Android Pie or above
          nnApiDelegate = NnApiDelegate()
          options.addDelegate(nnApiDelegate)
          options.setNumThreads(NUM_THREADS)
          options.setUseNNAPI(false)
          options.setAllowFp16PrecisionForFp32(true)
          options.setAllowBufferHandleOutput(true)
          options.setUseNNAPI(true)
        }
        if (isGPU) {
          val gpuDelegate = GpuDelegate()
          options.addDelegate(gpuDelegate)
        }
        d.tfLite = Interpreter(Utils.loadModelFile(assetManager, modelFilename), options)
      } catch (e: Exception) {
        throw RuntimeException(e)
      }
      d.isModelQuantized = isQuantized
      // Pre-allocate buffers.
      val numBytesPerChannel: Int = if (isQuantized) {
        1 // Quantized
      } else {
        4 // Floating point
      }
      d.imgData = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3 * numBytesPerChannel)
      d.imgData.order(ByteOrder.nativeOrder())
      d.intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
      return d
    }

    // Float model
    private const val IMAGE_MEAN = 0f
    private const val IMAGE_STD = 255.0f

    //config yolov4
    private const val INPUT_SIZE = 416
    private val OUTPUT_WIDTH = intArrayOf(52, 26, 13)
    private val MASKS = arrayOf(intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8))
    private val ANCHORS = intArrayOf(
            12, 16, 19, 36, 40, 28, 36, 75, 76, 55, 72, 146, 142, 110, 192, 243, 459, 401
    )
    private val XYSCALE = floatArrayOf(1.2f, 1.1f, 1.05f)
    private const val NUM_BOXES_PER_BLOCK = 3

    // Number of threads in the java app
    private const val NUM_THREADS = 4
    private const val isNNAPI = false
    private const val isGPU = true

    // tiny or not
    private const val isTiny = true

    // config yolov4 tiny
    private val OUTPUT_WIDTH_TINY = intArrayOf(2535, 2535)
    private val OUTPUT_WIDTH_FULL = intArrayOf(10647, 10647)
    private val MASKS_TINY = arrayOf(intArrayOf(3, 4, 5), intArrayOf(1, 2, 3))
    private val ANCHORS_TINY = intArrayOf(
            23, 27, 37, 58, 81, 82, 81, 82, 135, 169, 344, 319
    )
    private val XYSCALE_TINY = floatArrayOf(1.05f, 1.05f)
    protected const val BATCH_SIZE = 1
    protected const val PIXEL_SIZE = 3

    fun NMS(list: List<Recognition>, labels: List<String>): ArrayList<Recognition> {
      val nmsList = ArrayList<Recognition>()
      for (k in labels.indices) {
        //1.find max confidence per class
        val pq = PriorityQueue<Recognition>(
                50) { lhs, rhs -> // Intentionally reversed to put high confidence at the head of the queue.
          java.lang.Float.compare(rhs.confidence!!, lhs.confidence!!)
        }
        for (i in list.indices) {
          if (list[i].detectedClass == k) {
            pq.add(list[i])
          }
        }

        //2.do non maximum suppression
        while (pq.size > 0) {
          //insert detection with max confidence
          val a = arrayOfNulls<Recognition>(pq.size)
          val detections: Array<Recognition> = pq.toArray(a)
          val max = detections[0]
          nmsList.add(max)
          pq.clear()
          for (j in 1 until detections.size) {
            val detection = detections[j]
            val b = detection.location!!
            if (box_iou(max.location!!, b) < mNmsThresh) {
              pq.add(detection)
            }
          }
        }
      }
      return nmsList
    }

    protected var mNmsThresh = 0.6f
    private fun box_iou(a: RectF, b: RectF): Float {
      return box_intersection(a, b) / box_union(a, b)
    }

    private fun box_intersection(a: RectF, b: RectF): Float {
      val w = overlap(
              (a.left + a.right) / 2, a.right - a.left,
              (b.left + b.right) / 2, b.right - b.left
      )
      val h = overlap(
              (a.top + a.bottom) / 2, a.bottom - a.top,
              (b.top + b.bottom) / 2, b.bottom - b.top
      )
      return if (w < 0 || h < 0) 0f else w * h
    }

    private fun box_union(a: RectF, b: RectF): Float {
      val i = box_intersection(a, b)
      return (a.right - a.left) * (a.bottom - a.top) + (b.right - b.left) * (b.bottom - b.top) - i
    }

    protected fun overlap(x1: Float, w1: Float, x2: Float, w2: Float): Float {
      val l1 = x1 - w1 / 2
      val l2 = x2 - w2 / 2
      val left = if (l1 > l2) l1 else l2
      val r1 = x1 + w1 / 2
      val r2 = x2 + w2 / 2
      val right = if (r1 < r2) r1 else r2
      return right - left
    }
  }


  override fun enableStatLogging(logStats: Boolean) {}
  // override fun getStatString(): String {
  //   return ""
  // }

  override fun close() {}
  override fun setNumThreads(num_threads: Int) {
    if (tfLite != null) tfLite!!.setNumThreads(num_threads)
  }

  override fun setUseNNAPI(isChecked: Boolean) {
    if (tfLite != null) tfLite!!.setUseNNAPI(isChecked)
  }

  private lateinit var ctx: Context
  private var isModelQuantized = false

  // Config values.
  // Pre-allocated buffers.
  override lateinit var labels: List<String>
  override var objThresh = CONST.MINIMUM_SCORE
  override var statString = ""

  private lateinit var intValues: IntArray
  private lateinit var imgData: ByteBuffer
  private var tfLite: Interpreter? = null

  /**
   * Non-maximum Suppression.
   * Removes overlapping bboxes, which is a side-effect of YOLO's operation.
   */
  protected fun nms(list: ArrayList<Recognition>): ArrayList<Recognition> {
    return NMS(list, labels)
  }

  /**
   * Writes Image data into a `ByteBuffer`.
   */
  protected fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
    val byteBuffer =
      ByteBuffer.allocateDirect(4 * BATCH_SIZE * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE)
    byteBuffer.order(ByteOrder.nativeOrder())
    val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
    bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    var pixel = 0
    for (i in 0 until INPUT_SIZE) {
      for (j in 0 until INPUT_SIZE) {
        val `val` = intValues[pixel++]
        byteBuffer.putFloat((`val` shr 16 and 0xFF) / 255.0f)
        byteBuffer.putFloat((`val` shr 8 and 0xFF) / 255.0f)
        byteBuffer.putFloat((`val` and 0xFF) / 255.0f)
      }
    }
    return byteBuffer
  }

  /**
   * For yolov4-tiny, the situation would be a little different from the yolov4, it only has two
   * output. Both has three dimenstion. The first one is a tensor with dimension [1, 2535,4], containing all the bounding boxes.
   * The second one is a tensor with dimension [1, 2535, class_num], containing all the classes score.
   * @param byteBuffer input ByteBuffer, which contains the image information
   * @param bitmap pixel disenty used to resize the output images
   * @return an array list containing the recognitions
   */
  private fun getDetectionsForFull(byteBuffer: ByteBuffer, bitmap: Bitmap): ArrayList<Recognition> {
    val detections = ArrayList<Recognition>()
    val outputMap: MutableMap<Int, Any> = HashMap()
    outputMap[0] = Array(1) {
      Array(OUTPUT_WIDTH_FULL[0]) { FloatArray(4) }
    }
    outputMap[1] = Array(1) {
      Array(OUTPUT_WIDTH_FULL[1]) {
        FloatArray(labels.size)
      }
    }
    val inputArray = arrayOf<Any>(byteBuffer)
    tfLite!!.runForMultipleInputsOutputs(inputArray, outputMap)
    val gridWidth = OUTPUT_WIDTH_FULL[0]
    val bboxes = outputMap[0] as Array<Array<FloatArray>>
    val out_score = outputMap[1] as Array<Array<FloatArray>>
    for (i in 0 until gridWidth) {
      var maxClass = 0f
      var detectedClass = -1
      val classes = FloatArray(labels.size)
      for (c in labels.indices) {
        classes[c] = out_score!![0][i][c]
      }
      for (c in labels.indices) {
        if (classes[c] > maxClass) {
          detectedClass = c
          maxClass = classes[c]
        }
      }
      val score = maxClass
      if (score > objThresh) {
        val xPos = bboxes[0][i][0]
        val yPos = bboxes[0][i][1]
        val w = bboxes[0][i][2]
        val h = bboxes[0][i][3]
        val rectF = RectF(
          Math.max(0f, xPos - w / 2),
          Math.max(0f, yPos - h / 2),
          Math.min((bitmap.width - 1).toFloat(), xPos + w / 2),
          Math.min((bitmap.height - 1).toFloat(), yPos + h / 2)
        )
        detections.add(Recognition(ctx, bitmap,"" + i, labels[detectedClass], score, rectF, detectedClass))
      }
    }
    return detections
  }

  private fun getDetectionsForTiny(byteBuffer: ByteBuffer, bitmap: Bitmap): ArrayList<Recognition> {
    val detections = ArrayList<Recognition>()
    val outputMap: MutableMap<Int, Any> = HashMap()
    outputMap[0] = Array(1) {
      Array(OUTPUT_WIDTH_TINY[0]) {
        FloatArray(
          4
        )
      }
    }
    outputMap[1] = Array(1) {
      Array(OUTPUT_WIDTH_TINY[1]) {
        FloatArray(
          labels.size
        )
      }
    }
    val inputArray = arrayOf<Any>(byteBuffer)
    tfLite!!.runForMultipleInputsOutputs(inputArray, outputMap)
    val gridWidth = OUTPUT_WIDTH_TINY[0]
    val bboxes = outputMap[0] as Array<Array<FloatArray>>
    val out_score = outputMap[1] as Array<Array<FloatArray>>
    for (i in 0 until gridWidth) {
      var maxClass = 0f
      var detectedClass = -1
      val classes = FloatArray(labels.size)
      for (c in labels.indices) {
        classes[c] = out_score!![0][i][c]
      }
      for (c in labels.indices) {
        if (classes[c] > maxClass) {
          detectedClass = c
          maxClass = classes[c]
        }
      }
      val score = maxClass
      if (score > objThresh) {
        val xPos = bboxes!![0][i][0]
        val yPos = bboxes[0][i][1]
        val w = bboxes[0][i][2]
        val h = bboxes[0][i][3]
        val rectF = RectF(
          Math.max(0f, xPos - w / 2),
          Math.max(0f, yPos - h / 2),
          Math.min((bitmap.width - 1).toFloat(), xPos + w / 2),
          Math.min((bitmap.height - 1).toFloat(), yPos + h / 2)
        )

        detections.add(Recognition(ctx, bitmap,"" + i, labels[detectedClass], score, rectF, detectedClass))
      }
    }
    return detections
  }

  override fun recognizeImage(bitmap: Bitmap): ArrayList<Recognition> {
    val byteBuffer = convertBitmapToByteBuffer(bitmap)
    val detections: ArrayList<Recognition> = if (isTiny) {
      getDetectionsForTiny(byteBuffer, bitmap)
    } else {
      getDetectionsForFull(byteBuffer, bitmap)
    }
    return nms(detections)
  }

  fun checkInvalidateBox(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    oriW: Float,
    oriH: Float,
    intputSize: Int
  ): Boolean {
    // (1) (x, y, w, h) --> (xmin, ymin, xmax, ymax)
    val halfHeight = height / 2.0f
    val halfWidth = width / 2.0f
    val pred_coor = floatArrayOf(x - halfWidth, y - halfHeight, x + halfWidth, y + halfHeight)

    // (2) (xmin, ymin, xmax, ymax) -> (xmin_org, ymin_org, xmax_org, ymax_org)
    val resize_ratioW = 1.0f * intputSize / oriW
    val resize_ratioH = 1.0f * intputSize / oriH
    val resize_ratio = if (resize_ratioW > resize_ratioH) resize_ratioH else resize_ratioW //min
    val dw = (intputSize - resize_ratio * oriW) / 2
    val dh = (intputSize - resize_ratio * oriH) / 2
    pred_coor[0] = 1.0f * (pred_coor[0] - dw) / resize_ratio
    pred_coor[2] = 1.0f * (pred_coor[2] - dw) / resize_ratio
    pred_coor[1] = 1.0f * (pred_coor[1] - dh) / resize_ratio
    pred_coor[3] = 1.0f * (pred_coor[3] - dh) / resize_ratio

    // (3) clip some boxes those are out of range
    pred_coor[0] = (if (pred_coor[0] > 0) pred_coor[0] else 0) as Float
    pred_coor[1] = (if (pred_coor[1] > 0) pred_coor[1] else 0) as Float
    pred_coor[2] = if (pred_coor[2] < oriW - 1) pred_coor[2] else oriW - 1
    pred_coor[3] = if (pred_coor[3] < oriH - 1) pred_coor[3] else oriH - 1
    if (pred_coor[0] > pred_coor[2] || pred_coor[1] > pred_coor[3]) {
      pred_coor[0] = 0f
      pred_coor[1] = 0f
      pred_coor[2] = 0f
      pred_coor[3] = 0f
    }

    // (4) discard some invalid boxes
    val temp1 = pred_coor[2] - pred_coor[0]
    val temp2 = pred_coor[3] - pred_coor[1]
    val temp = temp1 * temp2
    if (temp < 0) {
      LOG.E("checkInvalidateBox", "temp < 0")
      return false
    }
    if (sqrt(temp.toDouble()) > Float.MAX_VALUE) {
      LOG.E("checkInvalidateBox", "temp max")
      return false
    }
    return true
  }
}