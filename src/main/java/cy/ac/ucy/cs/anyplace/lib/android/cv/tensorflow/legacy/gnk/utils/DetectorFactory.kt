package cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.legacy.gnk.utils

import android.content.res.AssetManager
import cy.ac.ucy.cs.anyplace.lib.android.cv.enums.DetectionModel

@Deprecated("")
object DetectorFactory {

    /**
     * Creates [YoloV4Detector] detector using given [detectionModel] and [minimumScore].
     */
    fun createDetector(assetManager: AssetManager,
                       detectionModel: DetectionModel,
                       minimumScore: Float,
                       usePadding: Boolean): Detector {
        return YoloV4Detector(assetManager, detectionModel, minimumScore, usePadding)
    }

}
