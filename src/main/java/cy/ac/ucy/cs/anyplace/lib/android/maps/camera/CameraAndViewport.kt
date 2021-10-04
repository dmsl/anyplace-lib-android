package cy.ac.ucy.cs.anyplace.lib.android.maps.camera

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng

class CameraAndViewport {
  companion object {
    var LATLNG_UCY = LatLng(35.14425742563485, 33.41051356485263)
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