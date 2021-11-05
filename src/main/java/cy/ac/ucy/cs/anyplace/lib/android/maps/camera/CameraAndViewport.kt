package cy.ac.ucy.cs.anyplace.lib.android.maps.camera

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

class CameraAndViewport {
  companion object {
    var LATLNG_UCY = LatLng(35.144487632230664, 33.41121698404017)
    var LATLNG_UCYsw = LatLng(35.14411016675971, 33.41133987082109)
    var LATLNG_UCYne = LatLng(35.14487441002181, 33.411137631157615)
    var sw2 = LatLng(35.144077493386284, 33.41101850057425)
    var ne2 = LatLng(35.14486925689344, 33.41159785772652)
    // var BOUNDS_UCY = LatLngBounds(sw2, ne2)
    var BOUNDS_UCY = LatLngBounds(LATLNG_UCYsw, LATLNG_UCYne)

    val latLng = LATLNG_UCY
    fun loggerCamera(latLng: LatLng, maxZoomLevel: Float) : CameraPosition {
      return CameraPosition.builder()
          .target(latLng)
          .zoom(maxZoomLevel-2)
          .bearing(0f)
          .tilt(0f)
          .build()
    }
  }
}