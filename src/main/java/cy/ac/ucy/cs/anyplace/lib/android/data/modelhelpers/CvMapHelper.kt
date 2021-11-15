package cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers

import android.graphics.Bitmap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.heatmaps.WeightedLatLng
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.cache.Cache
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.Detector
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.models.CvDetection
import cy.ac.ucy.cs.anyplace.lib.models.CvLocation
import cy.ac.ucy.cs.anyplace.lib.models.CvMap
import cy.ac.ucy.cs.anyplace.lib.models.Floor

/**
 * Extra functionality on top of the below data classes:
 * - [CvMap]
 * - [CvMap]
 */
class CvMapHelper(val cvMap: CvMap,
                  val floorH: FloorHelper) {
  companion object {
    // val SCORE_LIMIT  = 0.7f
    fun toCvDetection(d: Detector.Detection) =
      CvDetection(d.className, d.boundingBox.width(), d.boundingBox.height(), d.ocr)

    fun toLatLng(cvLoc: CvLocation) = LatLng(cvLoc.lat.toDouble(), cvLoc.lon.toDouble())

    fun toCvLocation(latLng: LatLng, cvDetections: List<CvDetection>) =
      CvLocation(latLng.latitude.toString(), latLng.longitude.toString(), cvDetections)

    fun generate(floorH: FloorHelper, input: Map<LatLng, List<Detector.Detection>>): CvMap {
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

      return CvMap(floorH.spaceH.space.id, floorH.floor.floorNumber, cvLocations)
    }

    /**
     * Merges two CvMaps
     */
    fun merge(newCvMap: CvMap, oldCvMap: CvMap?): CvMap {
      if (oldCvMap == null) return newCvMap

      if (newCvMap.buid != oldCvMap.buid) {
        LOG.E(TAG, "merge: space ids don't match")
        return newCvMap
      } else if (newCvMap.floorNumber != oldCvMap.floorNumber) {
        LOG.E(TAG, "merge: floor numbers don't match")
        return newCvMap
      }

      val combined: MutableMap<LatLng, MutableList<CvDetection>> = HashMap()
      newCvMap.locations.forEach {  cvLoc ->
       val latLng = toLatLng(cvLoc)
        if (combined.containsKey(latLng)) {
          combined[latLng]?.addAll(cvLoc.detections)
        } else {
          combined[latLng]= cvLoc.detections.toMutableList()
        }
      }
      oldCvMap.locations.forEach { cvLoc ->
        val latLng = toLatLng(cvLoc)
        if (combined.containsKey(latLng)) {
          combined[latLng]?.addAll(cvLoc.detections)
        } else {
          combined[latLng]= cvLoc.detections.toMutableList()
        }
      }

      // combine:
      val cvLocations :MutableList<CvLocation> = mutableListOf()
      combined.forEach { (latLng, detections) ->
        LOG.D(TAG, "MERGE-LOC: $latLng: : ${detections.size}")
        val cvDetections: MutableList<CvDetection> = mutableListOf()
        detections.forEach { detection ->
          cvDetections.add(detection)
        }
        cvLocations.add(toCvLocation(latLng, cvDetections))
      }

      return CvMap(newCvMap.buid, newCvMap.floorNumber, cvLocations)
    }
  }

  private val cache by lazy { Cache(floorH.spaceH.ctx) }

  fun getLocationList() : List<LatLng> {
    var locations : MutableList<LatLng> = mutableListOf()
    cvMap.locations.forEach { cvLoc ->
      try {
        locations.add(LatLng(cvLoc.lat.toDouble(), cvLoc.lon.toDouble()))
      } catch (e: Exception) {}
    }
    return locations
  }

  fun getWeightedLocationList() : List<WeightedLatLng> {
    var locations : MutableList<WeightedLatLng> = mutableListOf()
    cvMap.locations.forEach { cvLoc ->
      try {
        // TODO:CV calculate intensity (how strong a cvLoc is) differently.
        // e.g., unique objects count extra..
        val intensity : Double = cvLoc.detections.size.toDouble()
        val loc = LatLng(cvLoc.lat.toDouble(), cvLoc.lon.toDouble())
        locations.add(WeightedLatLng(loc, intensity))
      } catch (e: Exception) {}
    }
    return locations
  }

  fun hasCache() = cache.hasJsonFloorCvMap(cvMap)
  fun clearCache() = cache.deleteFloorCvMap(cvMap)
  fun readLocalAndMerge(): CvMap {
      val localCvMap  = cache.readFloorCvMap(cvMap)
      return merge(cvMap, localCvMap)
  }
  fun cache() = cache.saveFloorCvMap(cvMap)
}