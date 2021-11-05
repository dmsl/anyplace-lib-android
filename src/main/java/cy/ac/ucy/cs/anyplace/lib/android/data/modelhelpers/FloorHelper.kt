package cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.Repository
import cy.ac.ucy.cs.anyplace.lib.models.Floor
import android.util.Base64
import cy.ac.ucy.cs.anyplace.lib.android.cache.Cache
import cy.ac.ucy.cs.anyplace.lib.models.Space
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import okhttp3.ResponseBody
import retrofit2.Response

/**
 * Extra functionality on top of the [Floor] data class.
 */
class FloorHelper(val floor: Floor,
                  val spaceH: SpaceHelper) {

  private val cache by lazy { Cache(spaceH.ctx) }

  fun prettyFloorplanNumber() = "${spaceH.prettyFloorplan}${floor.floorNumber}"
  fun prettyFloorNumber() = "${spaceH.prettyFloor}${floor.floorName}"
  fun prettyFloorName() = "${spaceH.prettyFloorplan} ${floor.floorName}"

  fun northEast() : LatLng {
    val latNE = floor.topRightLat.toDouble()
    val lonNE = floor.topRightLng.toDouble()
    return LatLng(latNE, lonNE)
  }

  fun southWest() : LatLng {
    val latSW = floor.bottomLeftLat.toDouble()
    val lonSW = floor.bottomLeftLng.toDouble()
    return LatLng(latSW, lonSW)
  }

  fun bounds() : LatLngBounds {
    return LatLngBounds(southWest(), northEast())
  }

  fun hasFloorplanCached(): Boolean { return cache.hasFloorplan(floor) }
  fun loadFromCache() : Bitmap? { return cache.loadFloorplan(floor) }
  fun clearCache() { cache.clearFloorplan(floor) }
  fun cacheFloorplan(bitmap: Bitmap?) { bitmap.let { cache.saveFloorplan(floor, bitmap) } }

  // CLR:PM
  // https://ap-dev.cs.ucy.ac.cy:9001/api/floorplans64/vessel_9bdb1052-ff23-4f9b-b9f9-aae5095af468_1634646807927/-2
  /**
   * Request and cache a [Bitmap]
   */
  suspend fun requestRemoteFloorplan() : Bitmap? {
    LOG.D2("requestRemoteFloorplan: ${floor.buid}: ${floor.floorNumber}")
    val response = spaceH.repo.remote.getFloorplanBase64(floor.buid, floor.floorNumber)
    return handleResponse(response)
  }

  private fun handleResponse(response: Response<ResponseBody>): Bitmap? {
    if (response.errorBody() != null) {
      LOG.E("Response: ErrorBody: ${response.errorBody().toString()}")
      return null
    }

    val base64 = response.body()?.string()
    val byteArray = Base64.decode(base64, Base64.DEFAULT)

    // TODO CACHE this..
    // TODO CHECK CACHE BEFORE downloading.
    return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    // CLR:PM
    // imageView.setImageBitmap(decodedImage)
    // val decodedByte = Base64.decode(base64, Base64.DEFAULT)
    // val bitmap = BitmapFactory.decodeByteArray(decodedData, 0, decodedData.length)
    // val decodedBytes = Base64.getDecoder().decode(base64)
    // val decodedString = String(decodedBytes)
    // val decodedString = Base64.decode(base64 ,Base64.DEFAULT);
    // final Bitmap decodedByte =
    //   BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
  }
}