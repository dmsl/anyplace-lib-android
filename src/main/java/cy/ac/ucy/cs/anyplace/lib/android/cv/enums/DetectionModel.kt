package cy.ac.ucy.cs.anyplace.lib.android.cv.enums

import cy.ac.ucy.cs.anyplace.lib.android.cv.enums.CONFIG.Companion.INPUT_SIZE
import cy.ac.ucy.cs.anyplace.lib.android.cv.enums.CONFIG.Companion.OUTPUT_WIDTH_TINY

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
  LASHCO(
          "vessel",
          "models/vessel/model.tflite",
          "file:///android_asset/models/vessel/obj.names",
          INPUT_SIZE,
          OUTPUT_WIDTH_TINY,
          false
  ),

  UCYCO(
          "campus-ucy",
          "models/campus-ucy/model.tflite",
          "file:///android_asset/models/campus-ucy/obj.names",
          INPUT_SIZE,
          OUTPUT_WIDTH_TINY,
          false
  )
}