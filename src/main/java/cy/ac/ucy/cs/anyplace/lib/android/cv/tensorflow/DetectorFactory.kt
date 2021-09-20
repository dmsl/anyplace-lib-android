package cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow

import android.content.res.AssetManager
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.enums.DetectionModel

object DetectorFactory {

    /**
     * Creates [YoloV4Detector] detector using given [detectionModel] and [minimumScore].
     */
    fun createDetector(assetManager: AssetManager,
                       detectionModel: DetectionModel,
                       minimumScore: Float): Detector {
        return YoloV4Detector(assetManager, detectionModel, minimumScore)
    }

}
