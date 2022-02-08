package cy.ac.ucy.cs.anyplace.lib.android.cv.enums

import cy.ac.ucy.cs.anyplace.lib.android.cv.enums.DetectionModel

@Deprecated("TODO SETTINGS")
object YoloConstants {
  // TODO:PM OPTIONS
  // TODO:PM SETTINGS
  const val MINIMUM_SCORE: Float = 0.5f

  @Deprecated("replace all usages with the active model. and make it a setting..")
  val DETECTION_MODEL_LOGGER: DetectionModel = DetectionModel.COCO

  // val DETECTION_MODEL: DetectionModel = DetectionModel.COCO
  @Deprecated("replace all usages with the active model. and make it a setting..")
  val DETECTION_MODEL: DetectionModel = DetectionModel.VESSEL_b64sb8

  // val DETECTION_MODEL: DetectionModel = DetectionModel.VESSEL_b64sb1
  // val DETECTION_MODEL: DetectionModel = DetectionModel.VESSEL
  // val DETECTION_MODEL: DetectionModel = DetectionModel.VESSEL_v1
  // val DETECTION_MODEL: DetectionModel = DetectionModel.CAMPUS_UCY
  // val DETECTION_MODEL: DetectionModel = DetectionModel.HOME_PM2
}