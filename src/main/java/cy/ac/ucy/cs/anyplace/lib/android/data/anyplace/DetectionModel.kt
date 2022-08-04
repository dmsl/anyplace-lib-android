package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace

import cy.ac.ucy.cs.anyplace.lib.android.cache.anyplace.Cache
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.INPUT_SIZE
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.OUTPUT_WIDTH_TINY
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG

/**
 * Enum which describes tflite models used by Detector.
 * In future you may change this completely with some api call that gets all this info
 * - it is kinda hard-coded now..
 */
enum class DetectionModel(
        /** model's name */
        val modelName: String,
        val idSmas: Int,
        val filename: String,
        val labelFilePath: String,
        val inputSize: Int,
        val outputSize: Int,
        val isQuantized: Boolean,
        val desc : String = "No Description"
) {
  COCO(
          "coco", 3,
          "models/coco/model.tflite",
          "file:///android_asset/models/coco/obj.names",
          INPUT_SIZE,
          OUTPUT_WIDTH_TINY,
          false,
          "generic objects."
  ),
  LASHCO(
          "lashco", 1,
          "models/lashco/model.tflite",
          "file:///android_asset/models/lashco/obj.names",
          INPUT_SIZE,
          OUTPUT_WIDTH_TINY,
          false,
          "objects on ro-ro ships."
  ),
  UCYCO(
          "ucyco", 2,
          "models/ucyco/model.tflite",
          "file:///android_asset/models/ucyco/obj.names",
          INPUT_SIZE,
          OUTPUT_WIDTH_TINY,
          false,
          "objects on a university campus."
  );

  companion object {
     val list = listOf(LASHCO, UCYCO, COCO)

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

    /** Return the label files (classes / obj.names) */
    fun filenameLabels(model: DetectionModel, cache: Cache) : String {
      return if (DBG.USE_ASSETS) model.labelFilePath
      else cache.fileModelLabels(model.idSmas.toString())
    }

    /** Return the weights (tflite)  */
    fun filenameWeights(model: DetectionModel, cache: Cache) : String {
      return if (DBG.USE_ASSETS) model.filename
      else cache.fileModelWeights(model.idSmas.toString())
    }

  }
}