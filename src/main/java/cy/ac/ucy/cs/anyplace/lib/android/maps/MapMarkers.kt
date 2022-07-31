package cy.ac.ucy.cs.anyplace.lib.android.maps

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.userIcon
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.GmapWrapper
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.toLatLng
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlLoc.toLatLng
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationMethod
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Coord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
                 private val map: GmapWrapper) {
  private val ctx = app.applicationContext

  companion object {
    private const val MAX_SECONDS_INACTIVE = 120 // up to 2 minutes is considered active
    private const val SHARED_CHAT_LOC = "SHARED-CHAT-LOC"
    fun isUserConsideredInactive(seconds: Long) = seconds > MAX_SECONDS_INACTIVE
  }

  /** Markers of scanned objects (from logger) */
  var scannedMarkers: MutableList<Marker> = mutableListOf()

  // TODO:PM show storedMarkers with green color
  var stored: MutableList<Marker> = mutableListOf()

  /** Marker of the last location calculated remotely using SMAS */
  var lastOwnLocation: Marker? = null
  /** Marker of the last location that was selected from the chat
   * i.e., when a user clicks on a shared location in the chat,
   * it is redirected back in the main activity, and this marker is drawn
   */
  var lastChatShareLocation: Marker? = null

  /** Active users on the map */
  var userLocations: MutableList<Marker> = mutableListOf()

  // private fun toString(ll: LatLng) : String {
  //   // ll.toString()
  //   // TODO:PMX FR10
  //   // return ""
  //   return ll.latitude.toString() + ",\n" + ll.longitude.toString()
  // }

  // private fun toString(coord: Coord) : String {
  //   return toString(toLatLng(coord))
  // }

  private fun ownLocationDrawable(setMethod: LocalizationMethod, alerting: Boolean): Int {
    return when {
      alerting -> R.drawable.marker_location_own_alert
      setMethod == LocalizationMethod.manualByUser -> R.drawable.marker_location_own_manual
      setMethod == LocalizationMethod.autoMostRecent -> R.drawable.marker_location_own_recent
      else -> R.drawable.marker_location_own_smas
    }
  }



  /** Last Location marker */
  private fun ownLocationMarker(coord: Coord, locMethod: LocalizationMethod, alerting: Boolean) : MarkerOptions  {
    val latLng = toLatLng(coord)
    val title = "Own Location"

    // val snippet = getSnippetOwnLocation(locMethod)

    return MarkerOptions().position(latLng)
            .userIcon(ctx, ownLocationDrawable(locMethod, alerting))
            .zIndex(100f)
            .title(title)
    // .snippet(snippet)
  }

  /** Computer Vision marker */
  private fun scanMarker(latLng: LatLng, title: String, snippet: String) : MarkerOptions  {
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

  fun addScanMarker(coord: Coord, title: String, snippet: String) {
    scope.launch(Dispatchers.Main) {
      map.obj.addMarker(scanMarker(coord.toLatLng(), title, snippet))?.let {
        scannedMarkers.add(it)

        it.tag = UserInfoMetadata(UserInfoType.LoggerScan,
                LocalizationMethod.NA,
                "",
                coord,
                0,
                false)
      }
    }
  }

  /** User marker in active mode */
  private fun userMarker(latLng: LatLng, title: String, snippet: String) : MarkerOptions {
    return MarkerOptions().position(latLng)
            .userIcon(ctx, R.drawable.marker_location_other_user_active)
            .zIndex(10f)
            .title(title)
            .snippet(snippet)
  }

  /** User marker in alerting mode */
  private fun userAlertMarker(latLng: LatLng, title: String, snippet: String) : MarkerOptions  {
    return MarkerOptions().position(latLng)
            .userIcon(ctx, R.drawable.marker_location_other_user_alert)
            .zIndex(101f)
            .title(title)
            .snippet(snippet)
  }

  /** User marker in inactive state */
  private fun userInactiveMarker(latLng: LatLng, title: String, snippet: String) : MarkerOptions {
    return MarkerOptions().position(latLng)
            .userIcon(ctx, R.drawable.marker_location_other_user_inactive)
            .zIndex(8f)
            .title(title)
            .snippet(snippet)
  }

  /** User marker in alert mode */
  private fun sharedChatLocationMarker(latLng: LatLng) : MarkerOptions  {
    return MarkerOptions().position(latLng)
            .userIcon(ctx, R.drawable.marker_location_share_from_chat)
            .title("shared location")
            .snippet("(click to hide)")
  }

  /**
   * return a pretty string of the last activity of the user
   */
  fun getActiveInfo(secondsElapsed: Long) : String {
    // TODO:PMX: FR10
    val prettySecs = utlTime.getSecondsPretty(secondsElapsed)
    return "Active: $prettySecs ago"
  }

  fun addUserMarker(latLng: LatLng, uid: String, alert: Int, time: Long) {
    scope.launch(Dispatchers.IO) {

      val secondsElapsed = utlTime.secondsElapsed(time)
      val detailedMsg = getActiveInfo(secondsElapsed)

      val title = uid
      val marker = when {  // TODO:FR10
        alert == 1 -> userAlertMarker(latLng, title, detailedMsg)
        isUserConsideredInactive(secondsElapsed) -> userInactiveMarker(latLng, title, detailedMsg)
        else -> userMarker(latLng, title, detailedMsg)
      }

      // add a marker and then store it
      // val marker = userMarker(latLng, title, detailedMsg) // TODO:PMX: FR10
      scope.launch(Dispatchers.Main) {
        map.obj.addMarker(marker)?.let {
          userLocations.add(it)

          val currentFloor = app.wFloor?.floorNumber()!!
          val otherUserCoords = Coord(latLng.latitude, latLng.longitude, currentFloor)
          it.tag = UserInfoMetadata(UserInfoType.OtherUser,
                  LocalizationMethod.NA,
                  uid,
                  otherUserCoords,
                  secondsElapsed,
                  (alert==1))

          // reopen the last selected user
          if (userWasLastSelected(uid)) {
            LOG.E(TAG,"FOUND LAST USER: $uid (opening)")
            it.showInfoWindow()
            // also follow the user on map
            if (app.dsCvMap.read.first().followSelectedUser) {
              LOG.D3(TAG, "following user on map..")
              animateOnOutOfBounds(latLng)
            }
          }
        }
      }
    }
  }

  /**
   * This other user was previously selected in the UI by the current user.
   * Because we re-draw users at some intervals (update their locations),
   * we remove all previous user location markers and then add their updated locations.
   *
   * By storing the last selection, we can re-open the info window of the last selected user
   */
  fun userWasLastSelected(uid: String): Boolean {
    return lastSelectedUser != null && lastSelectedUser == uid
  }

  fun hideScanMarkers() {
    scope.launch(Dispatchers.Main) {
      scannedMarkers.forEach { it.remove() }
    }
  }


  /** Re-open last selected user on refresh
   *
   * Current user (own user) is excluded from this.
   */
  var lastSelectedUser : String?=null
  fun storeLastSelectedUser(marker: Marker) {
    if (marker.isInfoWindowShown) {
      val metadata = marker.tag as UserInfoMetadata?
      if (metadata!=null) {
        lastSelectedUser = metadata.uid
        LOG.E(TAG, "STORE LAST: ${metadata.uid}")
      }
    }
  }

  /**
   * IMPORTANT: it must run on the Main thread
   */
  fun clearAllInfoWindow() {
    LOG.I(TAG, "$METHOD: clearing all markers")
    // clear last selected user
    userLocations.forEach {
      it.hideInfoWindow()
      val metadata = it.tag as UserInfoMetadata?
      if (metadata != null) {
        if (!lastSelectedUser.isNullOrEmpty() && lastSelectedUser == metadata.uid) {
          lastSelectedUser=null
        }
      }
    }

    if (lastOwnLocation != null) {
      lastOwnLocation?.hideInfoWindow()
    }
  }


  fun hideUserMarkers() {
    userLocations.forEach {
      scope.launch(Dispatchers.Main) {
        storeLastSelectedUser(it) // store selected user before removing
        it.remove()
      }
    }
  }

  // private fun getSnippetOwnLocation(coord: Coord) = toString(coord)

  /**
   * If the location marker is on a different floor it will become transparent,
   * and show relevant info in the snippet
   */
  fun updateLocationMarkerBasedOnFloor(floorNum: Int) {
    if (lastOwnLocation == null || lastCoord == null) return

    LOG.D(TAG, "$METHOD: floor: $floorNum. last one: ${lastCoord!!.level}")

    var alpha = 1f
    var snippet = ""
    if (floorNum != lastCoord!!.level)  {  // on a different floor
      alpha=0.7f
      snippet += "On ${app.wFloor?.prettyFloor}: ${lastCoord?.level}"
    }

    scope.launch(Dispatchers.Main) {
      lastOwnLocation?.alpha=alpha
      lastOwnLocation?.snippet=snippet
      if (lastOwnLocation?.isInfoWindowShown == true) {
        lastOwnLocation?.hideInfoWindow()
        lastOwnLocation?.showInfoWindow()
      }
    }
  }


  private fun removeLastOwnLocation() {
    if (lastOwnLocation != null) { // hide previous location
      lastOwnLocation!!.remove()
      lastOwnLocation=null
    }
  }

  var lastCoord : Coord?=null
  fun setOwnLocationMarker(coord: Coord, locMethod: LocalizationMethod, alerting: Boolean) {
    val latLng = coord.toLatLng()
    LOG.D2()

    lastCoord=coord
    map.animateToLocation(latLng)

    scope.launch(Dispatchers.Main) {
      removeLastOwnLocation()

      val uid = app.dsSmasUser.read.first().uid
      // app.dsUser.readUser.first().id
      lastOwnLocation = map.obj.addMarker(ownLocationMarker(coord, locMethod, alerting))
      lastOwnLocation!!.tag = UserInfoMetadata(
              UserInfoType.OwnUser,
              locMethod,
              uid,
              coord,
              utlTime.epoch(),
              alerting)
    }

    updateLocationMarkerBasedOnFloor(coord.level)
  }


  fun animateOnOutOfBounds(latLng: LatLng) {
    if (map.outOfMapBounds(latLng)) {
      map.animateToLocation(latLng)
    }
  }

  fun clearChatLocationMarker() {
    LOG.D2()
    scope.launch(Dispatchers.Main) {
      if (lastChatShareLocation != null) { // hide previous location
        lastChatShareLocation!!.remove()
        lastChatShareLocation = null
      }
    }
  }

  /**
   * Opens the Info Window of an existing marker, if their [latLng] overlaps exactly.
   * It might be our own user location, or any other user location.
   *
   * If not match is found: returns false
   */
  @Deprecated("Does not work as expected..")
  private fun tryToOpenExistingMarker(coord: Coord) : Boolean {
    val latLng = coord.toLatLng()
    // pointing to our current location
    if (lastOwnLocation != null &&
            latLng == lastOwnLocation!!.position
            && coord.level == lastCoord!!.level) {
      lastOwnLocation!!.showInfoWindow()

      LOG.E(TAG, "found own user")
      return true
    }

    userLocations.forEach {  // these users are on the current floor anyway (so no need to check the floor)
      if (latLng == it.position) {
        LOG.E(TAG, "found existing other user")
        // extract uid from tag
        val metadata = it.tag as UserInfoMetadata?
        if (metadata != null) {
          lastSelectedUser = metadata.uid
          LOG.E(TAG, "USER FOUND: $metadata.uid")
        }

        // very ugly workaround: open InfoWindow in a few moments
        // (so the method will return, and afterwards we'll open it)
        scope.launch(Dispatchers.IO) {
          delay(250)
          scope.launch(Dispatchers.Main) { // switch to main thread

            LOG.E(TAG, "SHOWING INFO WIN")
            it.showInfoWindow()
          }
        }
        return true
      }
    }
    return false  // an additional marker will have to be drawn
  }

  /**
   * Draw a location marker that was returned from the [SmasChatActivity].
   */
  fun addSharedLocationMarker(coord: Coord) {
    LOG.E(TAG, "addChatLocationMarker")

    scope.launch(Dispatchers.Main) {
      val latLng = coord.toLatLng()
      map.animateToLocation(latLng)

      // DONT DO THIS...
      // there was an existing marker already (either own user marker, or other user marker)
      // In that case, avoid drawing another ovelapping marker on top of that.
      // if (tryToOpenExistingMarker(coord)) return@launch

      // otherwise, if there is no perfect overlap: draw a [sharedChatLocationMarker]
      lastChatShareLocation = map.obj.addMarker(sharedChatLocationMarker(latLng)
              .zIndex(102f))
      lastChatShareLocation!!.tag = UserInfoMetadata(
              UserInfoType.SharedLocation,
              LocalizationMethod.NA,
              SHARED_CHAT_LOC,
              coord,
              utlTime.epoch(),
              false)
      lastChatShareLocation!!.showInfoWindow()
    }

  }

}