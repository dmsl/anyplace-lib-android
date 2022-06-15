package cy.ac.ucy.cs.anyplace.lib.android.maps

import android.content.Context
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.userIcon
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Marker Management on the [GoogleMap]
 *
 * NOTE: any UI updating operations must be executed on the Main [CoroutineScope] (UI Thread)
 */
class Markers(private val ctx: Context,
              private val scope: CoroutineScope,
              private val map: GoogleMap) {
  companion object {
    private val TAG = Markers::class.java.simpleName
  }

  /** GMap markers used in detections */
  var cvObjects: MutableList<Marker> = mutableListOf()
  // TODO:PM show storedMarkers with green color
  var stored: MutableList<Marker> = mutableListOf()
  /** Marker of the last location calculated locally using CvMaps */
  var lastLocationLOCAL: Marker? = null
  /** Marker of the last location calculated remotely using SMAS */
  var lastLocationREMOTE: Marker? = null

  /** Active users on the map */
  var users: MutableList<Marker> = mutableListOf()

  /** Last Location marker LOCAL */
  private fun locationMarkerLOCAL(latLng: LatLng) : MarkerOptions  {
    return MarkerOptions().position(latLng)
            .userIcon(ctx, R.drawable.marker_location_local, 255/4)
            .title("CvMap Location")
  }

  /** Last Location marker REMOTE */
  private fun locationMarkerREMOTE(latLng: LatLng) : MarkerOptions  {
    return MarkerOptions().position(latLng)
            .userIcon(ctx, R.drawable.marker_location_smas)
            .title("Smas Location")
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
    scope.launch(Dispatchers.Main) {
      map.addMarker(cvMarker(latLng, msg))?.let {
        cvObjects.add(it)
      }
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

  fun addUserMarker(latLng: LatLng, msg: String, alert: Int, time: Long) {
    scope.launch(Dispatchers.IO) {
      val MAX_SECONDS = 10
      val secondsElapsed = utlTime.secondsElapsed(time)
      // val detailedMsg= "$msg ${secondsElapsed}s"   // TODO:PMX
      val detailedMsg= "$msg"
      val marker = when {  // TODO:PMX
        alert == 1 -> userAlertMarker(latLng, detailedMsg)
        secondsElapsed > MAX_SECONDS -> userInactiveMarker(latLng, detailedMsg)
        else -> userMarker(latLng, detailedMsg)
      }
      // add a marker and then store it
      val marker2 = userMarker(latLng, detailedMsg)
      scope.launch(Dispatchers.Main) {
        map.addMarker(marker2)?.let { users.add(it) }
      }

    }
  }

  // TODO:PM hide LoggerMarkers?
  fun hideCvObjMarkers() {
    cvObjects.forEach {
      scope.launch(Dispatchers.Main) { it.remove() }
    }
  }

  fun hideUserMarkers() {
    users.forEach {
      scope.launch(Dispatchers.Main) { it.remove() }
    }
  }

  /**
   * Updates the user's location to position [latLng]
   */
  fun setLocationMarkerLOCAL(latLng: LatLng) {
    LOG.D2()
    if (lastLocationLOCAL == null) {
      LOG.D3(TAG, "$METHOD: initial marker")

      scope.launch(Dispatchers.Main) {
        lastLocationLOCAL = map.addMarker(locationMarkerLOCAL(latLng))
      }
    } else {
      LOG.D3(TAG, "$METHOD: updated marker")
      scope.launch(Dispatchers.Main) {
        lastLocationLOCAL?.position = latLng
      }
      // CLR:PM
      // lastLocationMarker?.remove()
      // lastLocationMarker = map.addMarker(locationMarker(latLng))
      // lastLocationMarker = map.addMarker(locationMarker(latLng))
      // MarkerAnimation.animateMarkerToICS(ourGlobalMarker, newLatLng, new LatLngInterpolator.Spherical());
      // https://gist.github.com/broady/6314689
      //https://www.youtube.com/watch?v=WKfZsCKSXVQ
    }

    // animateToMarker(latLng)
  }

  fun setLocationMarkerREMOTE(latLng: LatLng) {
    LOG.D2()
    if (lastLocationREMOTE == null) {
      LOG.D3(TAG, "$METHOD: initial marker")

      scope.launch(Dispatchers.Main) {
        lastLocationREMOTE = map.addMarker(locationMarkerREMOTE(latLng))
      }
    } else {
      LOG.D3(TAG, "$METHOD: updated marker")
      scope.launch(Dispatchers.Main) {
        lastLocationREMOTE?.position = latLng
      }
    }
    animateToMarker(latLng)
  }

  /**
   * Pan camera to new location
   */
  fun animateToMarker(latLng: LatLng) {
    // map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, map.maxZoomLevel))

    scope.launch(Dispatchers.Main) {
      val cameraPosition = CameraPosition(
              latLng, map.cameraPosition.zoom,
              // don't alter tilt/bearing
              map.cameraPosition.tilt,
              map.cameraPosition.bearing)

      val newCameraPosition = CameraUpdateFactory.newCameraPosition(cameraPosition)
      map.animateCamera(newCameraPosition)
    }
  }

}