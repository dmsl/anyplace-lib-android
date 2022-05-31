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

import android.graphics.Bitmap
import android.graphics.RectF

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
    var detectedClass = 0

    /** TODO:PM OCM. any other field? */
    var ocr: String? = null

    constructor(
      id: String, title: String, confidence: Float, location: RectF) {
      this.id = id
      this.title = title
      this.confidence = confidence
      this.location = location
    }

    constructor(
      id: String, title: String, confidence: Float,
      location: RectF, detectedClass: Int) {
      this.id = id
      this.title = title
      this.confidence = confidence
      this.location = location
      this.detectedClass = detectedClass
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