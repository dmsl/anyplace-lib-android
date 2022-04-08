package cy.ac.ucy.cs.anyplace.lib.android.maps

import android.content.Context
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.userIcon
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Markers(private val ctx: Context,
              private val map: GoogleMap) {
  companion object {
    private val TAG = Markers::class.java.simpleName
  }

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
            .userIcon(ctx, R.drawable.marker_objects_stored)
  }
  /** Computer Vision marker */
  private fun cvMarker(latLng: LatLng, msg: String) : MarkerOptions  {
    return MarkerOptions().position(latLng).title(msg)
        .userIcon(ctx, R.drawable.marker_objects)
  }

  /** Computer Vision stored marker */
  fun cvMarkerStored(latLng: LatLng, msg: String) : MarkerOptions  {
    return MarkerOptions().position(latLng).title(msg)
        .userIcon(ctx, R.drawable.marker_objects)
  }

  fun addCvMarker(latLng: LatLng, msg: String) {
    map.addMarker(cvMarker(latLng, msg))?.let {
      cvObjects.add(it)
    }
  }

  /** User marker */
  private fun userMarker(latLng: LatLng, msg: String) : MarkerOptions {
    return MarkerOptions().position(latLng).title(msg)
            .userIcon(ctx, R.drawable.marker_user)
  }

  /** User marker in alert mode */
  private fun userAlertMarker(latLng: LatLng, msg: String) : MarkerOptions  {
    return MarkerOptions().position(latLng).title(msg)
            .userIcon(ctx, R.drawable.marker_user_alert)
  }

  /** User marker in alert mode */
  private fun userInactiveMarker(latLng: LatLng, msg: String) : MarkerOptions {
    return MarkerOptions().position(latLng).title(msg)
            .userIcon(ctx, R.drawable.marker_user_inactive)
  }

  fun addUserMarker(latLng: LatLng, msg: String, scope: CoroutineScope, alert: Int, time: Long) {
    scope.launch(Dispatchers.Main) {
      val MAX_SECONDS = 10
      val secondsElapsed = utlTime.secondsElapsed(time)
      // val detailedMsg= "$msg ${secondsElapsed}s"
      val detailedMsg= "$msg" // TODO:PMX
      val marker = when {
        alert == 1 -> userAlertMarker(latLng, detailedMsg)
        secondsElapsed > MAX_SECONDS -> userInactiveMarker(latLng, detailedMsg)
        else -> userMarker(latLng, detailedMsg)
      }
      // TODO:PMX
      val marker2 = userMarker(latLng, detailedMsg)
      map.addMarker(marker2)?.let { users.add(it) }
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
      LOG.D2(TAG, "setLocationMarker: initial marker")
      lastLocationMarker = map.addMarker(locationMarker(latLng))
    } else {
      LOG.D2(TAG, "setLocationMarker: updated marker")
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
                            map.cameraPosition.bearing)))
  }
}