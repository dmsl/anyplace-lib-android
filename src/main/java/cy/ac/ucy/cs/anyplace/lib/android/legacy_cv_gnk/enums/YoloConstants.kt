package cy.ac.ucy.cs.anyplace.lib.android.legacy_cv_gnk.enums

import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.DetectionModel

@Deprecated("TODO SETTINGS")
object YoloConstants {
  const val MINIMUM_SCORE: Float = 0.5f

  @Deprecated("replace all usages with the active model. and make it a setting..")
  val DETECTION_MODEL_LOGGER: DetectionModel = DetectionModel.COCO

  // val DETECTION_MODEL: DetectionModel = DetectionModel.COCO
  // @Deprecated("replace all usages with the active model. and make it a setting..")
  // val DETECTION_MODEL: DetectionModel = DetectionModel.VESSEL


}