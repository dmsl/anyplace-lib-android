package cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.enums

import org.tensorflow.lite.examples.detector.utils.CONFIG.Companion.INPUT_SIZE
import org.tensorflow.lite.examples.detector.utils.CONFIG.Companion.OUTPUT_WIDTH_TINY

/**
 * TODO:PM Deprecate. download online.
 * Enum which describes tflite models used by Detector.
 */
enum class DetectionModel(
    val modelFilename: String,
    val labelFilePath: String,
    val inputSize: Int,
    val outputSize: Int,
    val isQuantized: Boolean
) {
    COCO(
        "models/coco/model.tflite",
        "file:///android_asset/models/coco/obj.names",
        INPUT_SIZE,
        OUTPUT_WIDTH_TINY,
        false
    )
    ,
    CAMPUS_UCY(
    "models/home-pm-v2-60000+Final/model.tflite",
    "file:///android_asset/models/campus-ucy-v1-50000+final/obj.names",
        INPUT_SIZE,
        OUTPUT_WIDTH_TINY,
    false
    ),
    HOME_PM2(
        "models/home-pm-v2-60000+Final/model.tflite",
        "file:///android_asset/models/home-pm-v2-60000+Final/obj.names",
        INPUT_SIZE,
        OUTPUT_WIDTH_TINY,
        false
    )
}