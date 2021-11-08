package cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers

import com.google.android.gms.maps.model.LatLng
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.Detector
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.models.CvDetection
import cy.ac.ucy.cs.anyplace.lib.models.CvLocation
import cy.ac.ucy.cs.anyplace.lib.models.CvMap

/**
 * Extra functionality on top of the below data classes:
 * - [CvMap]
 * - [CvMap]
 */
class DetectionMapHelper {
  companion object {
   val SCORE_LIMIT  = 0.7f

    fun toCvDetection(d: Detector.Detection) =
      CvDetection(d.className, d.boundingBox.width(), d.boundingBox.height(), d.ocr)

    fun toCvLocation(latLng: LatLng, cvDetections: List<CvDetection>) =
      CvLocation(latLng.latitude.toString(), latLng.longitude.toString(), cvDetections)


    // map: Map<LatLng, List<Detector.Detection>>
    fun generate(input: Map<LatLng, List<Detector.Detection>>): CvMap? {
      val cvLocations :MutableList<CvLocation> = mutableListOf()
      input.forEach { (latLng, detections) ->
        LOG.D(TAG, "LOCATION: $latLng: : ${detections.size}")
        val cvDetections: MutableList<CvDetection> = mutableListOf()
        detections.forEach { detection ->
          LOG.D(TAG, "  - ${detection.className}:${detection.detectedClass}: score: ${detection.score}")
          cvDetections.add(toCvDetection(detection))
        }
        cvLocations.add(toCvLocation(latLng, cvDetections))
      }

      return CvMap(cvLocations)
    }
  }
}