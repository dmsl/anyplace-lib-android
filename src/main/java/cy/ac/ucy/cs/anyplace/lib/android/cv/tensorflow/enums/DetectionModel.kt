package cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.enums

import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.utils.CONFIG.Companion.INPUT_SIZE
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.utils.CONFIG.Companion.OUTPUT_WIDTH_TINY

/**
 * TODO:PM Deprecate. download online.
 * Enum which describes tflite models used by Detector.
 */
enum class DetectionModel(
        val modelName: String,
    val modelFilename: String,
    val labelFilePath: String,
    val inputSize: Int,
    val outputSize: Int,
    val isQuantized: Boolean) {
    COCO(
        "coco",
        "models/coco/model.tflite",
        "file:///android_asset/models/coco/obj.names",
        INPUT_SIZE,
        OUTPUT_WIDTH_TINY,
        false
    ),
  VESSEL_WRONG(
          "vessel-wrong",
          "models/vessel-wrong/model.tflite",
          "file:///android_asset/models/vessel-wrong/obj.names",
          INPUT_SIZE,
          OUTPUT_WIDTH_TINY,
          false
  ),
  VESSEL_v1(
          "vessel-wrong",
          "models/vessel-v1/model.tflite",
          "file:///android_asset/models/vessel-v1/obj.names",
          INPUT_SIZE,
          OUTPUT_WIDTH_TINY,
          false
  ),

  VESSEL_b64sb1(
          "vessel-b64sb1",
          "models/vessel-211221-final-b64sb1/model.tflite",
          "file:///android_asset/models/vessel-211221-final-b64sb1/obj.names",
          INPUT_SIZE,
          OUTPUT_WIDTH_TINY,
          false
  ),

  VESSEL_b64sb8(
          "vessel-b64sb8",
          "models/vessel-231221-final-b64sb8/model.tflite",
          "file:///android_asset/models/vessel-231221-final-b64sb8/obj.names",
          INPUT_SIZE,
          OUTPUT_WIDTH_TINY,
          false
  ),

  CAMPUS_UCY(
          "campus-ucy",
          "models/campus-ucy/model.tflite",
          "file:///android_asset/models/campus-ucy/obj.names",
          INPUT_SIZE,
          OUTPUT_WIDTH_TINY,
          false
  )
}