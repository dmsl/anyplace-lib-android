package cy.ac.ucy.cs.anyplace.lib.android.maps

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.userIcon
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.toLatLng
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlLoc.toLatLng
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationMethod
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Coord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Marker Management on the [GoogleMap]
 *
 * NOTE: any UI updating operations must be executed on the Main [CoroutineScope] (UI Thread)
 */
class MapMarkers(private val app: AnyplaceApp,
                 private val scope: CoroutineScope,
                 private val VM: CvViewModel,
                 private val map: GoogleMap) {
  private val ctx = app.applicationContext

  companion object {
    private const val MAX_SECONDS_INACTIVE = 120 // up to 2 minutes is considered active
    private const val SHARED_CHAT_LOC = "SHARED-CHAT-LOC"
    fun isUserConsideredInactive(seconds: Long) = seconds > MAX_SECONDS_INACTIVE
  }

  /** GMap markers used in detections */
  var cvObjects: MutableList<Marker> = mutableListOf()
  // TODO:PM show storedMarkers with green color
  var stored: MutableList<Marker> = mutableListOf()

  /** Marker of the last location calculated remotely using SMAS */
  var lastLocation: Marker? = null
  /** Marker of the last location that was selected from the chat
   * i.e., when a user clicks on a shared location in the chat,
   * it is redirected back in the main activity, and this marker is drawn
   */
  var lastChatLocation: Marker? = null

  /** Active users on the map */
  var users: MutableList<Marker> = mutableListOf()

  // private fun toString(ll: LatLng) : String {
  //   // ll.toString()
  //   // TODO:PMX FR10
  //   // return ""
  //   return ll.latitude.toString() + ",\n" + ll.longitude.toString()
  // }

  // private fun toString(coord: Coord) : String {
  //   return toString(toLatLng(coord))
  // }

  private fun getLocationDrawable(setMethod: LocalizationMethod): Int {
    return when (setMethod) {
      LocalizationMethod.manualByUser -> R.drawable.marker_location_manually
      LocalizationMethod.autoMostRecent -> R.drawable.marker_location_autorecent
      else -> R.drawable.marker_location_smas
    }
  }

  private fun getSnippetOwnLocation (locMethod: LocalizationMethod) : String {
    return when (locMethod) {
      LocalizationMethod.manualByUser -> " (manually set)"
      LocalizationMethod.autoMostRecent -> " (last recent location)"
      else -> "(set through Vision)"
    }
  }

  /** Last Location marker */
  private fun myLocationMarker(coord: Coord, locMethod: LocalizationMethod) : MarkerOptions  {
    val latLng = toLatLng(coord)
    val title = "My Location"

    val snippet = getSnippetOwnLocation(locMethod)

    return MarkerOptions().position(latLng)
            .userIcon(ctx, getLocationDrawable(locMethod))
            .zIndex(100f)
            .title(title)
            .snippet(snippet)
  }

  /** Computer Vision marker */
  private fun cvMarker(latLng: LatLng, title: String, snippet: String) : MarkerOptions  {
    return MarkerOptions()
            .position(latLng)
            .title(title)
            .zIndex(10f)
            .userIcon(ctx, R.drawable.marker_objects)
            .snippet(snippet)
  }

  /** Computer Vision stored marker */
  @Deprecated("Stored fingerprints are now uploaded on backend and not shown..")
  fun cvMarkerStored(latLng: LatLng, msg: String) : MarkerOptions  {
    return MarkerOptions().position(latLng).title(msg)
            .zIndex(10f)
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
  private fun userMarker(latLng: LatLng, title: String, snippet: String) : MarkerOptions {
    return MarkerOptions().position(latLng)
            .userIcon(ctx, R.drawable.marker_user)
            .zIndex(10f)
            .title(title)
            .snippet(snippet)
  }

  /** User marker in alert mode */
  private fun userAlertMarker(latLng: LatLng, title: String, snippet: String) : MarkerOptions  {
    return MarkerOptions().position(latLng)
            .userIcon(ctx, R.drawable.marker_user_alert)
            .zIndex(101f)
            .title(title)
  }

  /** User marker in alert mode */
  private fun userInactiveMarker(latLng: LatLng, title: String, snippet: String) : MarkerOptions {
    return MarkerOptions().position(latLng)
            .userIcon(ctx, R.drawable.marker_user_inactive)
            .zIndex(8f)
            .title(title)
            .snippet(snippet)
  }

  /** User marker in alert mode */
  private fun sharedChatLocationMarker(latLng: LatLng) : MarkerOptions  {
    return MarkerOptions().position(latLng)
            .title("shared location")
  }

  fun addUserMarker(latLng: LatLng, uid: String, alert: Int, time: Long) {
    scope.launch(Dispatchers.IO) {
      val secondsElapsed = utlTime.secondsElapsed(time)
      val prettySecs = utlTime.getSecondsPretty(secondsElapsed)
      // TODO:PMX: FR10
      val detailedMsg= "Active: $prettySecs ago"

      val title = uid
      val marker = when {  // TODO:FR10
        alert == 1 -> userAlertMarker(latLng, title, detailedMsg)
        isUserConsideredInactive(secondsElapsed) -> userInactiveMarker(latLng, title, detailedMsg)
        else -> userMarker(latLng, title, detailedMsg)
      }
      // add a marker and then store it
      // val marker = userMarker(latLng, title, detailedMsg) // TODO:PMX: FR10
      scope.launch(Dispatchers.Main) {
        map.addMarker(marker)?.let {
          users.add(it)

          val currentFloor = app.wFloor?.floorNumber()!!
          val otherUserCoords = Coord(latLng.latitude, latLng.longitude, currentFloor)
          val userType = if (alert == 1) UserInfoType.OtherUserAlerting else UserInfoType.OtherUser
          it.tag = UserInfoMetadata(userType, uid, otherUserCoords, secondsElapsed)

          // reopen the last opened user
          if (isLastOpenedUser(uid)) { it.showInfoWindow() }
        }
      }
    }
  }

  fun isLastOpenedUser(uid: String): Boolean {
    return lastOpenedUser != null && lastOpenedUser == uid
  }

  // TODO:PM hide LoggerMarkers?
  fun hideCvObjMarkers() {
    cvObjects.forEach {
      scope.launch(Dispatchers.Main) { it.remove() }
    }
  }


  /** reopen last opened user on refresh */
  var lastOpenedUser : String?=null
  fun storeLastOpenedUser(marker: Marker) {
    if (marker.isInfoWindowShown) {
      val metadata = marker.tag as UserInfoMetadata?
      if (metadata?.type != UserInfoType.OwnUser) {
        lastOpenedUser = metadata?.uid
      }
    }
  }

  fun hideUserMarkers() {
    users.forEach {
      scope.launch(Dispatchers.Main) {
        storeLastOpenedUser(it) // before removing
        it.remove() 
      }
    }
  }

  // private fun getSnippetOwnLocation(coord: Coord) = toString(coord)

  /**
   * If the location marker is on a different floor it will become transparent,
   * and show relevant info in the snippet
   */
  fun updateLocationMarkerBasedOnFloor(floorNum: Int, locMethod: LocalizationMethod) {
    if (lastLocation == null || lastCoord == null) return

    LOG.D(TAG, "$METHOD: floor: $floorNum. last one: ${lastCoord!!.level}")

    var alpha = 1f
    var snippet = ""
    if (lastCoord!= null) snippet = getSnippetOwnLocation(locMethod)
    if (floorNum != lastCoord!!.level)  {  // on a different floor
      alpha=0.8f
      snippet += "\n\n${app.wFloor?.prettyFloorCapitalize}: ${lastCoord?.level}"
    } else {
      if (lastCoord!= null) snippet = getSnippetOwnLocation(locMethod)
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
  fun setLocationMarker(coord: Coord, locMethod: LocalizationMethod) {
    val latLng = coord.toLatLng()
    LOG.D2()

    lastCoord=coord
    animateToLocation(latLng)

    scope.launch(Dispatchers.Main) {
      if (lastLocation != null) { // hide previous location
        lastLocation!!.remove()
        lastLocation=null
      }

      val uid = app.dsSmasUser.read.first().uid
      // app.dsUser.readUser.first().id
      lastLocation = map.addMarker(myLocationMarker(coord, locMethod))
      lastLocation!!.tag = UserInfoMetadata(UserInfoType.OwnUser, uid, coord, utlTime.epoch())
    }

    updateLocationMarkerBasedOnFloor(coord.level, locMethod)
  }


  /**
   * Pan camera to new location
   */
  fun animateToLocation(latLng: LatLng) {
    // nice animation but it causes issues..
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

  fun clearChatLocationMarker() {
    scope.launch(Dispatchers.Main) {
      LOG.E(TAG,"clearChatLocationMarker")
      if (lastChatLocation != null) { // hide previous location
        lastChatLocation!!.remove()
        lastChatLocation = null
      }
    }
  }

  /**
   * Draw a location marker that was returned from the [SmasChatActivity].
   */
  fun addChatLocationMarker(coord: Coord) {
    LOG.E(TAG, "addChatLocationMarker")

    scope.launch(Dispatchers.Main) {
      // clearChatLocationMarker()
      val latLng = coord.toLatLng()
      animateToLocation(latLng)

      lastChatLocation = map.addMarker(sharedChatLocationMarker(latLng).zIndex(9f))
      lastChatLocation!!.tag = UserInfoMetadata(UserInfoType.SharedLocation, SHARED_CHAT_LOC, coord, utlTime.epoch())
    }

  }

}