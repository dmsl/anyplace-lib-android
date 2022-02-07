package cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.heatmaps.WeightedLatLng
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.cache.Cache
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.legacy.gnk.utils.Detector
import cy.ac.ucy.cs.anyplace.lib.android.cv.enums.DetectionModel
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.utils.converters.toLatLng
import cy.ac.ucy.cs.anyplace.lib.models.*

/**
 * Extra functionality on top of the below data classes:
 * - [CvMap]
 *
 */
class CvMapHelper(val cvMap: CvMap,
                  /** Label names of the used [DetectionModel] */
                  val labels: List<String>,
                  val floorH: FloorHelper) {

  lateinit var cvMapFast: CvMapFast

  companion object {
    // val SCORE_LIMIT  = 0.7f
    fun toCvDetection(d: Detector.Detection) =
      CvDetection(d.className, d.boundingBox.width(), d.boundingBox.height(), d.ocr)

    fun toCvLocation(latLng: LatLng, cvDetections: List<CvDetection>) =
      CvLocation(latLng.latitude.toString(), latLng.longitude.toString(), cvDetections)

    /**
     * Generates a CvMap from a list of [input] detections
     */
    fun generate(model: DetectionModel, floorH: FloorHelper, input: Map<LatLng, List<Detector.Detection>>): CvMap {
      val cvLocations :MutableList<CvLocation> = mutableListOf()
      LOG.D(TAG, "generate:")
      input.forEach { (latLng, detections) ->
        LOG.D(TAG, "location: $latLng: : ${detections.size}")
        val cvDetections: MutableList<CvDetection> = mutableListOf()
        detections.forEach { detection ->
          LOG.D(TAG, "  - ${detection.className}:${detection.detectedClass}: score: ${detection.score}")
          cvDetections.add(toCvDetection(detection))
        }
        cvLocations.add(toCvLocation(latLng, cvDetections))
      }

      return CvMap(model.modelName,
              floorH.spaceH.space.id,
              floorH.floor.floorNumber,
              cvLocations, CvMap.SCHEMA)
    }

    /**
     * Merges two CvMaps
     */
    fun merge(cvm1: CvMap, cvm2: CvMap?): CvMap {
      if (cvm2 == null) return cvm1

      if (cvm1.buid != cvm2.buid) {
        LOG.E(TAG, "merge: Space IDs don't match")
        return cvm1
      } else if (cvm1.floorNumber != cvm2.floorNumber) {
        LOG.E(TAG, "merge: floor number don't match")
        return cvm1
      }

      // fill a hashmap with entries from [cvm1] (then for [cvm2]). O(n+m)
      // key is the location. If a key exists, then append the [CvDetection] list
      val combined: MutableMap<LatLng, MutableList<CvDetection>> = HashMap()
      cvm1.locations.forEach { cvLoc ->
       val latLng = toLatLng(cvLoc)
        if (combined.containsKey(latLng)) {
          combined[latLng]?.addAll(cvLoc.detections)
        } else {
          combined[latLng]= cvLoc.detections.toMutableList()
        }
      }
      cvm2.locations.forEach { cvLoc ->
        val latLng = toLatLng(cvLoc)
        if (combined.containsKey(latLng)) {
          combined[latLng]?.addAll(cvLoc.detections)
        } else {
          combined[latLng]= cvLoc.detections.toMutableList()
        }
      }

      // iterate the hashmap and combine entries:
      // - for each KV: put all
      val cvLocations: MutableList<CvLocation> = mutableListOf()
      combined.forEach { (latLng, detections) ->
        LOG.D(TAG, "merge: location: $latLng: : ${detections.size}")
        // val cvDetections: MutableList<CvDetection> = mutableListOf()
        // CHECK:PM CLR:PM doing this directly..(no immutable obj restriction) ..
        // cvDetections.addAll(detections)
        // detections.forEach { cvDetections.add(it) } // CHECK:PM and CLR:PM
        cvLocations.add(toCvLocation(latLng, detections))
      }

      return CvMap(cvm2.detectionModel,
              cvm1.buid,
              cvm1.floorNumber,
              cvLocations,
              cvm1.schema)
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

  fun hasCache() = cache.hasDirFloorCvMapsLocal(cvMap)
  fun clearCache() = cache.deleteFloorCvMapsLocal(cvMap)
  fun readLocalAndMerge(): CvMap {
      val localCvMap  = cache.readFloorCvMap(cvMap)
      return merge(cvMap, localCvMap)
  }
  fun storeToCache() = cache.saveFloorCvMap(cvMap)

  fun generateCvMapFast() {
    LOG.W(TAG, "generateCvMapFast TODO:PM coroutine?")
    val s = System.currentTimeMillis()
    cvMapFast = CvMapFast(cvMap, labels)
    val time = System.currentTimeMillis()-s
    LOG.W(TAG_METHOD, "in ${time}ms.")
  }
}

