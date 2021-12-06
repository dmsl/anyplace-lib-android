package cy.ac.ucy.cs.anyplace.lib.android.cv.misc

import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.enums.DetectionModel

object Constants {
  const val MINIMUM_SCORE: Float = 0.5f

  // XXX BUG VESSEL model not working..
  // load on empty mmvm project and try again..

  // TODO:PM replace this...
  @Deprecated("replace all usages with the active model. and make it a setting..")
  val DETECTION_MODEL: DetectionModel = DetectionModel.COCO
  // val DETECTION_MODEL: DetectionModel = DetectionModel.VESSEL
  // val DETECTION_MODEL: DetectionModel = DetectionModel.VESSEL_v1
  // val DETECTION_MODEL: DetectionModel = DetectionModel.CAMPUS_UCY
  // val DETECTION_MODEL: DetectionModel = DetectionModel.HOME_PM2
}