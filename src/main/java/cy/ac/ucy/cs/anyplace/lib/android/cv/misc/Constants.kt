package cy.ac.ucy.cs.anyplace.lib.android.cv.misc

import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.enums.DetectionModel

object Constants {
  const val MINIMUM_SCORE: Float = 0.5f

  val DETECTION_MODEL: DetectionModel = DetectionModel.COCO
  // val DETECTION_MODEL: DetectionModel = DetectionModel.VESSEL
  // val DETECTION_MODEL: DetectionModel = DetectionModel.CAMPUS_UCY_
  // val DETECTION_MODEL: DetectionModel = DetectionModel.HOME_PM2
}