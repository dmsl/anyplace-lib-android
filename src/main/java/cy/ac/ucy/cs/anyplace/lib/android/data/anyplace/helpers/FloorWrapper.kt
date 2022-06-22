package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Floor
import android.util.Base64
import com.google.gson.Gson
import cy.ac.ucy.cs.anyplace.lib.android.cache.anyplace.Cache
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.DetectionModel
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import okhttp3.ResponseBody
import retrofit2.Response

/**
 * Extra functionality on top of the [Floor] data class.
 */
class FloorWrapper(val obj: Floor,
                   val spaceH: SpaceWrapper) {

  override fun toString(): String = Gson().toJson(obj, Floor::class.java)

  companion object {
    fun parse(str: String): Floor = Gson().fromJson(str, Floor::class.java)
  }

  private val cache by lazy { Cache(spaceH.ctx) }

  fun floorNumber() : Int = obj.floorNumber.toInt()
  fun prettyFloorplanNumber() = "${spaceH.prettyFloorplan}${obj.floorNumber}"
  fun prettyFloorNumber() = "${spaceH.prettyFloor}${obj.floorName}"
  fun prettyFloorName() = "${spaceH.prettyFloor} ${obj.floorName}"

  val prettyFloor : String get() = spaceH.prettyFloor
  val prettyFloors : String get() = spaceH.prettyFloors
  val prettyFloorPlan : String get() = spaceH.prettyFloorplan
  val prettyFloorPlans : String get() = spaceH.prettyFloorplans

  fun northEast() : LatLng {
    val latNE = obj.topRightLat.toDouble()
    val lonNE = obj.topRightLng.toDouble()
    return LatLng(latNE, lonNE)
  }

  fun southWest() : LatLng {
    val latSW = obj.bottomLeftLat.toDouble()
    val lonSW = obj.bottomLeftLng.toDouble()
    return LatLng(latSW, lonSW)
  }

  fun bounds() : LatLngBounds {
    return LatLngBounds(southWest(), northEast())
  }

  fun hasFloorplanCached(): Boolean { return cache.hasFloorplan(obj) }
  fun loadFromCache() : Bitmap? { return cache.readFloorplan(obj) }
  fun clearCacheFloorplan() { cache.deleteFloorplan(obj) }
  // fun clearCacheCvMap() { cache.deleteFloorCvMap(floor) }
  /** Deletes the cvmap folder that might contain several CvMaps
   * created with different [DetectionModel]s */
  fun clearCacheCvMaps() { cache.deleteFloorCvMapsLocal(obj) }
  fun clearCache() {
    clearCacheFloorplan()
    clearCacheCvMaps()
  }
  fun cacheFloorplan(bitmap: Bitmap?) { bitmap.let { cache.saveFloorplan(obj, bitmap) } }
  fun hasFloorCvMap(model: DetectionModel) = cache.hasJsonFloorCvMapModelLocal(obj, model)
  fun loadCvMapFromCache(model: DetectionModel) = cache.readFloorCvMap(obj, model)

  // CLR:PM
  // https://ap-dev.cs.ucy.ac.cy:9001/api/floorplans64/vessel_9bdb1052-ff23-4f9b-b9f9-aae5095af468_1634646807927/-2
  /**
   * Request and cache a [Bitmap]
   */
  suspend fun requestRemoteFloorplan() : Bitmap? {
    LOG.D3(TAG,"$METHOD: ${obj.buid}: ${obj.floorNumber}")
    val response = spaceH.repo.remote.getFloorplanBase64(obj.buid, obj.floorNumber)
    return handleResponse(response)
  }

  private fun handleResponse(response: Response<ResponseBody>): Bitmap? {
    if (response.errorBody() != null) {
      LOG.E("Response: ErrorBody: ${response.errorBody().toString()}")
      return null
    }
    val base64 = response.body()?.string()
    val byteArray = Base64.decode(base64, Base64.DEFAULT)

    return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
  }
}