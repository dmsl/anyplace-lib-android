package cy.ac.ucy.cs.anyplace.lib.android.cv.enums

import cy.ac.ucy.cs.anyplace.lib.android.cv.enums.CONFIG.Companion.INPUT_SIZE
import cy.ac.ucy.cs.anyplace.lib.android.cv.enums.CONFIG.Companion.OUTPUT_WIDTH_TINY

/**
 * Enum which describes tflite models used by Detector.
 *
 * TODO:PM download online.
 */
enum class DetectionModel(
        /** model's name */
        val model: String,
        val filename: String,
        val labelFilePath: String,
        val inputSize: Int,
        val outputSize: Int,
        val isQuantized: Boolean,
        val desc : String = "No Description"
) {
  COCO(
          "coco",
          "models/coco/model.tflite",
          "file:///android_asset/models/coco/obj.names",
          INPUT_SIZE,
          OUTPUT_WIDTH_TINY,
          false,
          "generic objects."
  ),
  LASHCO(
          "lashco",
          "models/lashco/model.tflite",
          "file:///android_asset/models/lashco/obj.names",
          INPUT_SIZE,
          OUTPUT_WIDTH_TINY,
          false,
          "objects on ro-ro ships."
  ),
  UCYCO(
          "ucyco",
          "models/ucyco/model.tflite",
          "file:///android_asset/models/ucyco/obj.names",
          INPUT_SIZE,
          OUTPUT_WIDTH_TINY,
          false,
          "objects on a university campus."
  );

  companion object {
     val list = listOf(LASHCO.model, UCYCO.model, COCO.model)

    fun getModelAndDescription(modelName: String) : String {
      return "${modelName.uppercase()}: ${getDescription(modelName)}"
    }

    private fun getDescription(modelName: String) : String {
      val detModel =  when (modelName.lowercase()) {
      "coco" -> COCO
      "lashco" -> LASHCO
      "ucyco" -> UCYCO
        else -> { null }
      }
      return detModel?.desc.toString()
    }
  }
}