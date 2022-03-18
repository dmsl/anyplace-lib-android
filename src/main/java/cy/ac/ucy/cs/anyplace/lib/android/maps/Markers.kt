package cy.ac.ucy.cs.anyplace.lib.android.maps

import android.content.Context
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.iconFromShape
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Markers(private val ctx: Context,
              private val map: GoogleMap) {

  /** GMap markers used in detections */
  var cvObjects: MutableList<Marker> = mutableListOf()
  // TODO:PM show storedMarkers with green color
  var stored: MutableList<Marker> = mutableListOf()
  var lastLocationMarker : Marker? = null

  /** Active users on the map */
  var users: MutableList<Marker> = mutableListOf()

  /** Last Location marker */
  private fun locationMarker(latLng: LatLng) : MarkerOptions  {
    return MarkerOptions().position(latLng)
            .iconFromShape(ctx, R.drawable.marker_objects_stored)
  }
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
    map.addMarker(cvMarker(latLng, msg))?.let {
      cvObjects.add(it)
    }
  }

  /** User marker */
  private fun userMarker(latLng: LatLng, msg: String) : MarkerOptions  {
    return MarkerOptions().position(latLng).title(msg)
            .iconFromShape(ctx, R.drawable.marker_user)
  }

  fun addUserMarker(latLng: LatLng, msg: String, scope: CoroutineScope) {
    scope.launch(Dispatchers.Main) {
      map.addMarker(userMarker(latLng, msg))?.let { users.add(it) }
    }
  }

  fun hideCvObjMarkers() { cvObjects.forEach { it.remove() } }

  fun hideUserMarkers() { users.forEach { it.remove() } }

  /**
   * Updates the user's location to position [latLng]
   */
  fun setLocationMarker(latLng: LatLng) {
    LOG.D2()
    if (lastLocationMarker == null) {
      LOG.D2(TAG_METHOD, "initial marker")
      lastLocationMarker = map.addMarker(locationMarker(latLng))
    } else {
      LOG.D2(TAG_METHOD, "updated marker")
      lastLocationMarker?.position = latLng
      // CLR:PM
      // lastLocationMarker?.remove()
      // lastLocationMarker = map.addMarker(locationMarker(latLng))
      // lastLocationMarker = map.addMarker(locationMarker(latLng))
      // MarkerAnimation.animateMarkerToICS(ourGlobalMarker, newLatLng, new LatLngInterpolator.Spherical());
      // https://gist.github.com/broady/6314689
      //https://www.youtube.com/watch?v=WKfZsCKSXVQ
    }

    // pan to new location
    // map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, map.maxZoomLevel))
    map.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                    CameraPosition(
                            latLng, map.cameraPosition.zoom,
                            // don't alter tilt/bearing
                            map.cameraPosition.tilt,
                            map.cameraPosition.bearing)
            )
    )
  }
}