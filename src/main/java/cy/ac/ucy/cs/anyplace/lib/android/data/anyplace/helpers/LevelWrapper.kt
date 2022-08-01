package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Level
import android.util.Base64
import com.google.gson.Gson
import cy.ac.ucy.cs.anyplace.lib.android.cache.anyplace.Cache
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.DetectionModel
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import okhttp3.ResponseBody
import retrofit2.Response

/**
 * Extra functionality on top of the [Level] data class.
 */
class LevelWrapper(val obj: Level,
                   val wSpace: SpaceWrapper,
) {

  val tag = "lvl-wrapper"

  override fun toString(): String = Gson().toJson(obj, Level::class.java)

  companion object {
    fun parse(str: String): Level = Gson().fromJson(str, Level::class.java)
  }

  private val cache by lazy { Cache(wSpace.ctx) }

  fun floorNumber() : Int = obj.number.toInt()
  fun prettyFloorplanNumber() = "${wSpace.prettyFloorplan}${obj.number}"
  fun prettyFloorNumber() = "${wSpace.prettyLevel}${obj.name}"
  fun prettyFloorName() = "${wSpace.prettyLevel} ${obj.name}"

  val prettyFloorCapitalize : String get() = prettyFloor.replaceFirstChar(Char::uppercase)
  val prettyFloor : String get() = wSpace.prettyLevel
  val prettyFloors : String get() = wSpace.prettyFloors
  val prettyFloorPlan : String get() = wSpace.prettyFloorplan
  val prettyFloorPlans : String get() = wSpace.prettyFloorplans

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

  fun loadFromCache() : Bitmap? {
    val method = ::loadFromCache.name
    LOG.E(tag, "$method: ${obj.number} ${obj.name} ${obj.buid}")
    return cache.readLevelplan(obj)
  }

  fun clearCacheFloorplan() { cache.deleteFloorplan(obj) }
  // fun clearCacheCvMap() { cache.deleteFloorCvMap(floor) }
  /** Deletes the cvmap folder that might contain several CvMaps
   * created with different [DetectionModel]s */
  fun clearCache() {
    clearCacheFloorplan()
    // Other cache?
  }
  fun cacheFloorplan(bitmap: Bitmap?) { bitmap.let { cache.saveFloorplan(obj, bitmap) } }

  /**
   * Request and cache a [Bitmap]
   */
  suspend fun requestRemoteFloorplan() : Bitmap? {
    LOG.D3(TAG,"$METHOD: ${obj.buid}: ${obj.number}")
    val response = wSpace.repo.remote.getFloorplanBase64(obj.buid, obj.number)
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