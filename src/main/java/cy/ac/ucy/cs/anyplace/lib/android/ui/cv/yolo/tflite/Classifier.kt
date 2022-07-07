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
import android.graphics.*
import android.util.SparseArray
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import javax.xml.transform.OutputKeys.METHOD

/** Generic interface for interacting with different recognition engines.  */
interface Classifier {

  fun recognizeImage(bitmap: Bitmap): List<Recognition>
  fun enableStatLogging(debug: Boolean)
  fun close()
  fun setNumThreads(num_threads: Int)
  fun setUseNNAPI(isChecked: Boolean)

  // CHECK: should these be nullable?
  val statString: String
  val objThresh: Float
  val labels: List<String>

  /** An immutable result returned by a Classifier describing what was recognized. */
  class Recognition {
    /** Unique id for what has been recognized. Specific to the class, not the object instance */
    val id: String

    /** Display name for the recognition. */
    val title: String

    /** Sortable score for how good the recognition is relative to others. Higher is better. */
    val confidence: Float

    /** Optional location within the source image for the location of the recognized object. */
    var location: RectF
    /** YOLO ID for the class detection (zero based) */
    var detectedClass = 0

    /** Filled with OCR */
    var ocr: String? = null

    constructor(
      id: String, title: String, confidence: Float, location: RectF) {
      this.id = id
      this.title = title
      this.confidence = confidence
      this.location = location
    }

    constructor(
            ctx: Context, bitmap: Bitmap,
            id: String, title: String, confidence: Float,
            location: RectF, detectedClass: Int) {
      this.id = id
      this.title = title
      this.confidence = confidence
      this.location = location
      this.detectedClass = detectedClass
      this.ocr=getOCR(ctx, bitmap)
    }

    fun getOCR(ctx: Context, bitmap: Bitmap) : String? {
      if (!isOCR(title)) return null

      LOG.D2(TAG, "$METHOD: has OCR")
      val cropped_bitmap = getCropBitmapByRect(bitmap, location)
      val str=getOcrFromBitmap(ctx, cropped_bitmap)
      LOG.D2(TAG, "$METHOD: text: '$str'")

      return str
    }

    fun isOCR(className: String) = className.endsWith("OCR")

    private fun getOcrFromBitmap(ctx: Context, source: Bitmap): String? {
      val textRecognizer = TextRecognizer.Builder(ctx).build()
      val frame = Frame.Builder().setBitmap(source).build()
      val sparseArray: SparseArray<TextBlock> = textRecognizer.detect(frame)
      var stringBuilder = ""
      for (i in 0 until sparseArray.size()) {
        val tx = sparseArray[i]
        val str = tx.value
        stringBuilder+=str+"\n"
      }
      return stringBuilder.ifEmpty { null }
    }

    private fun getCropBitmapByRect(source: Bitmap, cropRectF: RectF): Bitmap {
      val resultBitmap = Bitmap.createBitmap(cropRectF.width().toInt(),
              cropRectF.height().toInt(), source.config)
      val cavas = Canvas(resultBitmap)

      // draw background
      val paint = Paint(Paint.FILTER_BITMAP_FLAG)
      paint.setColor(Color.WHITE)
      cavas.drawRect( //from  w w  w. ja v  a  2s. c  om
              RectF(0F, 0F, cropRectF.width(), cropRectF.height()), paint)
      val matrix = Matrix()
      matrix.postTranslate(-cropRectF.left, -cropRectF.top)
      cavas.drawBitmap(source, matrix, paint)

      return resultBitmap
    }



    // fun getLocation(): RectF {  return RectF(location) }
    // fun setLocation(location: RectF?) { this.location = location }

    override fun toString(): String {
      var resultString = ""
      resultString += "[$id] "
      resultString += "$title "
      resultString += String.format("(%.1f%%) ", confidence * 100.0f)
      resultString += "$location "
      return resultString.trim { it <= ' ' }
    }
  }
}