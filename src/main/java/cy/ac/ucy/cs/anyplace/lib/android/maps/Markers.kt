package cy.ac.ucy.cs.anyplace.lib.android.maps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.extensions.iconFromShape
import cy.ac.ucy.cs.anyplace.lib.android.maps.camera.CameraAndViewport.Companion.LATLNG_UCY
import java.io.IOException
import java.io.InputStream

class Markers(private val ctx: Context,
              private val map: GoogleMap) {

  /** GMap markers used in detections */
  var active: MutableList<Marker> = mutableListOf()

  /** Computer Vision marker */
  private fun cvMarker(latLng: LatLng, msg: String) : MarkerOptions  {
    return MarkerOptions().position(latLng).title(msg)
        .iconFromShape(ctx, R.drawable.marker_objects)
  }
  /** Computer Vision stored marker */
  fun cvMarkerStored(latLng: LatLng, msg: String) : MarkerOptions  {
    return MarkerOptions().position(latLng).title(msg)
        .iconFromShape(ctx, R.drawable.marker_objects)
  }

  fun addCvMarker(latLng: LatLng, msg: String) {
    active.add(map.addMarker(cvMarker(latLng, msg)))
  }

  fun hideActiveMakers() {
    active.forEach {
      it.remove()
    }
  }
}