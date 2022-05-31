package cy.ac.ucy.cs.anyplace.lib.android.legacy_cv_gnk.tensorflow.legacy.gnk.utils

import android.content.res.AssetManager
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.DetectionModel

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
