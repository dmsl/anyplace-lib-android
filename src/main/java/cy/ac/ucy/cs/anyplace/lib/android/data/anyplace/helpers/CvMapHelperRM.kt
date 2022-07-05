package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.heatmaps.WeightedLatLng
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.cache.anyplace.Cache
import cy.ac.ucy.cs.anyplace.lib.android.utils.cv.CvUtils
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.DetectionModel
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.Classifier
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlLoc
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.*
import cy.ac.ucy.cs.anyplace.lib.smas.models.CvDetection
import kotlinx.coroutines.CoroutineScope

/**
 * Extra functionality on top of the below data classes:
 * - [CvMapRM]
 *
 */
@Deprecated("DELETE")
class CvMapHelperRM(
        val cvMapRM: CvMapRM,
        /** Label names of the used [DetectionModel] */
        val labels: List<String>,
        val floorH: FloorWrapper) {

  lateinit var cvMapFast: CvMapFastRM

  companion object {
    /**
     * Tries to get also the oid (SMAS id for the class).
     * This happens only if the [cvUtils] hashmaps were pre-initialized somewhere in the
     * app flow earlier
     */
    fun toCvDetection(scope: CoroutineScope, cvUtils: CvUtils, model: DetectionModel, d: Classifier.Recognition) : CvDetection {
      val cvd = cvUtils.toCvDetection(scope, d, model)
      if (cvd !=null) return cvd

      return CvDetection(0, d.location.width().toDouble(), d.location.height().toDouble(), d.ocr, d.title)
    }


    fun toCvLocation(latLng: LatLng, cvDetections: List<CvDetection>) =
            CvLocationOLD(latLng.latitude.toString(), latLng.longitude.toString(), cvDetections)

    /**
     * Generates a CvMap from a list of [input] detections
     */
    @Deprecated("No longer in use")
    fun generate(
      app: AnyplaceApp,
      scope: CoroutineScope,
      model: DetectionModel, floorH: FloorWrapper, input: Map<LatLng, List<Classifier.Recognition>>): CvMapRM {
      val cvLocationOLDS :MutableList<CvLocationOLD> = mutableListOf()
      LOG.D(TAG, "generate:")
      input.forEach { (latLng, detections) ->
        LOG.D(TAG, "location: $latLng: : ${detections.size}")
        val cvDetections: MutableList<CvDetection> = mutableListOf()
        detections.forEach { detection ->

          val cvd = toCvDetection(scope, app.cvUtils, model, detection)
          LOG.D(TAG, "  - ${detection.detectedClass}:${detection.detectedClass}: score: ${detection.confidence}")
          cvDetections.add(cvd)
        }
        cvLocationOLDS.add(toCvLocation(latLng, cvDetections))
      }

      return CvMapRM(model.modelName,
              floorH.spaceH.obj.id,
              floorH.obj.floorNumber,
              cvLocationOLDS, CvMapRM.SCHEMA)
    }

    /**
     * Merges two CvMaps
     */
    @Deprecated("No longer in use")
    fun merge(cvm1: CvMapRM, cvm2: CvMapRM?): CvMapRM {
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
      cvm1.locationOLDS.forEach { cvLoc ->
        val latLng = utlLoc.toLatLng(cvLoc)
        if (combined.containsKey(latLng)) {
          combined[latLng]?.addAll(cvLoc.detections)
        } else {
          combined[latLng]= cvLoc.detections.toMutableList()
        }
      }
      cvm2.locationOLDS.forEach { cvLoc ->
        val latLng = utlLoc.toLatLng(cvLoc)
        if (combined.containsKey(latLng)) {
          combined[latLng]?.addAll(cvLoc.detections)
        } else {
          combined[latLng]= cvLoc.detections.toMutableList()
        }
      }

      // iterate the hashmap and combine entries:
      // - for each KV: put all
      val cvLocationOLDS: MutableList<CvLocationOLD> = mutableListOf()
      combined.forEach { (latLng, detections) ->
        LOG.D(TAG, "merge: location: $latLng: : ${detections.size}")
        // val cvDetections: MutableList<CvDetection> = mutableListOf()
        // CHECK:PM CLR:PM doing this directly..(no immutable obj restriction) ..
        // cvDetections.addAll(detections)
        // detections.forEach { cvDetections.add(it) } // CHECK:PM and CLR:PM
        cvLocationOLDS.add(toCvLocation(latLng, detections))
      }

      return CvMapRM(cvm2.detectionModel,
              cvm1.buid,
              cvm1.floorNumber,
              cvLocationOLDS,
              cvm1.schema)
    }
  }

  private val cache by lazy { Cache(floorH.spaceH.ctx) }

  fun getLocationList() : List<LatLng> {
    val locations : MutableList<LatLng> = mutableListOf()
    cvMapRM.locationOLDS.forEach { cvLoc ->
      try {
        locations.add(LatLng(cvLoc.lat.toDouble(), cvLoc.lon.toDouble()))
      } catch (e: Exception) {}
    }
    return locations
  }

  fun getWeightedLocationList() : List<WeightedLatLng> {
    var locations : MutableList<WeightedLatLng> = mutableListOf()
    cvMapRM.locationOLDS.forEach { cvLoc ->
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

  fun hasCache() = cache.hasDirFloorCvMapsLocal(cvMapRM)
  fun clearCache() = cache.deleteFloorCvMapsLocal(cvMapRM)
  fun readLocalAndMerge(): CvMapRM {
    val localCvMap  = cache.readFloorCvMap(cvMapRM)
    return merge(cvMapRM, localCvMap)
  }
  fun storeToCache() = cache.saveFloorCvMap(cvMapRM)

  fun generateCvMapFast() {
    LOG.W(TAG, "generateCvMapFast TODO:PM coroutine?")
    val s = System.currentTimeMillis()
    cvMapFast = CvMapFastRM(cvMapRM, labels)
    val time = System.currentTimeMillis()-s
    LOG.W(TAG_METHOD, "in ${time}ms.")
  }
}

