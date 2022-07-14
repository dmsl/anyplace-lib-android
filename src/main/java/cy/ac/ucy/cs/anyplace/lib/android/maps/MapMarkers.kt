package cy.ac.ucy.cs.anyplace.lib.android.maps

import android.content.Context
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.userIcon
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlLoc.toLatLng
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Coord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Marker Management on the [GoogleMap]
 *
 * NOTE: any UI updating operations must be executed on the Main [CoroutineScope] (UI Thread)
 */
class MapMarkers(private val ctx: Context,
                 private val scope: CoroutineScope,
                 private val VM: CvViewModel,
                 private val map: GoogleMap) {
  companion object {
    private val TAG = MapMarkers::class.java.simpleName
  }

  /** GMap markers used in detections */
  var cvObjects: MutableList<Marker> = mutableListOf()
  // TODO:PM show storedMarkers with green color
  var stored: MutableList<Marker> = mutableListOf()
  /** Marker of the last location calculated remotely using SMAS */
  var lastLocation: Marker? = null

  /** Active users on the map */
  var users: MutableList<Marker> = mutableListOf()

  private fun toString(ll: LatLng) : String {
    return ""
    // TODO:PMX FR10
    // return ll.latitude.toString() + ",\n" + ll.longitude.toString()
  }

  private fun toString(coord: Coord) : String {
    return toString(toLatLng(coord))
  }

  private fun getLocationDrawable(manuallySet: Boolean): Int {
    return if (manuallySet) R.drawable.marker_location_manually
      else R.drawable.marker_location_smas
  }


  /** Last Location marker REMOTE */
  private fun locationMarker(coord: Coord, manuallySet: Boolean) : MarkerOptions  {
    val latLng = toLatLng(coord)
    var title = "Location"
    if (manuallySet) title+=" (manual)"

    return MarkerOptions().position(latLng)
            // TODO:PMX FR10: CHECK with above?
            // .userIcon(ctx, R.drawable.marker_location_smas)
            .userIcon(ctx, getLocationDrawable(manuallySet))
            .zIndex(100f)
            .title(title)
            .snippet(getSnippetOwnLocation(coord))
  }

  /** Computer Vision marker */
  private fun cvMarker(latLng: LatLng, title: String, snippet: String) : MarkerOptions  {
    return MarkerOptions()
            .position(latLng)
            .title(title)
            .userIcon(ctx, R.drawable.marker_objects)
            .snippet(snippet)
  }

  /** Computer Vision stored marker */
  @Deprecated("Stored fingerprints are now uploaded on backend and not shown..")
  fun cvMarkerStored(latLng: LatLng, msg: String) : MarkerOptions  {
    return MarkerOptions().position(latLng).title(msg)
        .userIcon(ctx, R.drawable.marker_objects_stored)
  }

  fun addCvMarker(latLng: LatLng, title: String, snippet: String) {
    scope.launch(Dispatchers.Main) {
      map.addMarker(cvMarker(latLng, title, snippet))?.let {
        cvObjects.add(it)
      }
    }
  }

  /** User marker */
  private fun userMarker(latLng: LatLng, title: String) : MarkerOptions {
    // TODO:PMX FR10
    // val details = "Last Active: <>\n\n" + toString(latLng)
    val details = toString(latLng)
    return MarkerOptions().position(latLng).title(title)
            .userIcon(ctx, R.drawable.marker_user)
            .snippet(details)
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
        map.addMarker(marker2)?.let {
          users.add(it)
          it.tag = UserInfoMetadata(UserInfoType.OtherUser)
        }
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

  private fun getSnippetOwnLocation(coord: Coord) = toString(coord)

  /**
   * If the location marker is on a different floor it will become transparent,
   * and show relevant info in the snippet
   */
  fun updateLocationMarkerBasedOnFloor(floorNum: Int) {
    if (lastLocation == null || lastCoord == null) return

    LOG.D(TAG, "$METHOD: floor: $floorNum. last one: ${lastCoord!!.level}")

    var alpha = 1f
    var snippet = ""
    if (lastCoord!= null) snippet = getSnippetOwnLocation(lastCoord!!)
    if (floorNum != lastCoord!!.level)  {  // on a different floor
      alpha=0.5f
      snippet += "\n\nlast ${VM.wFloor?.prettyFloor}: ${lastCoord?.level}"
    } else {
      if (lastCoord!= null) snippet = getSnippetOwnLocation(lastCoord!!)
    }

    scope.launch(Dispatchers.Main) {
      lastLocation?.alpha=alpha
      lastLocation?.snippet=snippet
      if (lastLocation?.isInfoWindowShown == true) {
        lastLocation?.hideInfoWindow()
        lastLocation?.showInfoWindow()
      }
    }
  }

  var lastCoord : Coord?=null
  fun setLocationMarker(coord: Coord, manuallySet: Boolean) {
    val latLng = toLatLng(coord)
    LOG.D2()

    lastCoord=coord
    animateToLocation(latLng)

    scope.launch(Dispatchers.Main) {
      if (lastLocation != null) { // hide previous location
        lastLocation!!.remove()
        lastLocation=null
      }

      lastLocation = map.addMarker(locationMarker(coord, manuallySet))
      lastLocation!!.tag = UserInfoMetadata(UserInfoType.OwnUser)
    }

    updateLocationMarkerBasedOnFloor(coord.level)
  }


  /**
   * Pan camera to new location
   */
  fun animateToLocation(latLng: LatLng) {
    // map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, map.maxZoomLevel))

    scope.launch(Dispatchers.Main) {
      val cameraPosition = CameraPosition(
              latLng, map.cameraPosition.zoom,
              // don't alter tilt/bearing
              map.cameraPosition.tilt,
              map.cameraPosition.bearing)

      // val cancellableCallback= object : GoogleMap.CancelableCallback {
      //   override fun onCancel() {}
      //   override fun onFinish() {}
      // }

      val newCameraPosition = CameraUpdateFactory.newCameraPosition(cameraPosition)
      map.moveCamera(newCameraPosition)
    }
  }

}