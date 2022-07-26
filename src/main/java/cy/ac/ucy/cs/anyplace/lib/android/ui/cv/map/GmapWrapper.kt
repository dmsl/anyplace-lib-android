package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.FloorsWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.SpaceWrapper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.maps.MapLines
import cy.ac.ucy.cs.anyplace.lib.android.maps.MapMarkers
import cy.ac.ucy.cs.anyplace.lib.android.maps.Overlays
import cy.ac.ucy.cs.anyplace.lib.android.maps.camera.CameraAndViewport
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.CvMapActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.demo.AssetReader
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GmapWrapper(
                  private val app: AnyplaceApp,
                  private val scope: CoroutineScope,
                  private val UI: CvUI) {

  val tag = "wr-gmap"
  private val ctx: Context = app.applicationContext
  lateinit var obj: GoogleMap
  lateinit var VM: CvViewModel

  private val assetReader by lazy { AssetReader(ctx) }
  /** Overlays on top of the map, like Heatmaps */
  val overlays by lazy { Overlays(ctx, scope) }
  private val fHandler by lazy { FloorHandler(VM, scope, ctx, UI, overlays) }

  lateinit var mapView : MapView

  /** Dynamically attach a [GoogleMap] */
  fun attach(VM: CvViewModel, act: CvMapActivity, layout_id: Int) {
    this.VM=VM

    mapView = act.findViewById(layout_id)
    val mapFragment = SupportMapFragment.newInstance()
    act.supportFragmentManager
            .beginTransaction()
            .add(layout_id, mapFragment)
            .commit()
    scope.launch(Dispatchers.Main) {
      mapFragment.getMapAsync(act)
    }
  }

  /** Initialized onMapReady */
  lateinit var markers : MapMarkers
  lateinit var lines : MapLines

  var gmapWrLoaded = false
  fun setup(googleMap: GoogleMap) {
    LOG.D()
    LOG.E(TAG, "$tag: setup")

    obj = googleMap
    // TODO:PMX FR10
    // obj.setInfoWindowAdapter(UserInfoWindowAdapter(ctx))
    markers = MapMarkers(app, scope, VM, obj)
    lines = MapLines(app, scope, VM, obj)

    // ON FLOOR LOADED....
    obj.uiSettings.apply {
      isZoomControlsEnabled = false
      isMapToolbarEnabled = false
      isTiltGesturesEnabled = false
      isCompassEnabled = false
      isIndoorLevelPickerEnabled = false
    }


    // TODO Space must be sent here using some SelectSpaceActivity (w/ SafeArgs?)
    // (maybe using Bundle is easier/better)
    loadSpaceAndFloor()

    // async continues by [onFloorLoaded]

    // onMapReadySpecialize()
    gmapWrLoaded = true
  }

  /**
   * when the [Floor] is loaded, it continues the setup by:
   * - bringing the floor plans (downloading or from cache)
   * - setting up zoom preferences
   */
  fun onFloorLoaded() {
    // TODO:PM this must be moved to earlier activity
    // along with Space/Floors loading (that also needs implementation).
    scope.launch(Dispatchers.IO) { app.wFloors.fetchAllFloorplans(VM) }

    val maxZoomLevel = obj.maxZoomLevel // may be different from device to device

    obj.setMinZoomPreference(maxZoomLevel-4)
    // place some restrictions on the map
    LOG.D2(TAG, "MAX ZOOM: $maxZoomLevel (restriction)")

    // restrict screen to current bounds.
    scope.launch {
      delay(500) // CHECK is ths a bugfix?

      if (app.wFloor == null) {
        LOG.E(TAG_METHOD, "Floor is null. Cannot update google map location")
        return@launch
      }

      // zoom ins to include the floorplan
      // gmap.moveCamera(CameraUpdateFactory.newLatLngBounds(VM.floorH?.bounds()!!, 0))

      // zooms to the center of the floorplan
      obj.moveCamera(CameraUpdateFactory.newCameraPosition(
              CameraAndViewport.loggerCamera(app.wFloor?.bounds()!!.center, maxZoomLevel-2)))

      val floorOnScreenBounds = obj.projection.visibleRegion.latLngBounds
      LOG.D2("bounds: ${floorOnScreenBounds.center}")
      // LEFTHERE: we get OUT OF BOUNDS RESULTS!
      // some crashes, and some weird results.
      // invastigating..
      // QR CODES?! those NOT part of the model.
      LOG.W(TAG, "Setting camera bounds: users will be restricted to viewing just the particular space")
      obj.setLatLngBoundsForCameraTarget(app.wFloor?.bounds())
    }
  }

  // protected fun onMapReadySpecialize() {
  // }

  /**
   * Loads from assets the Space and the Space's Floors
   * Then it loads the floorplan for [selectedFloorPlan].
   *
   * TODO Implement this from network (@earlier), and pass it w/[SafeArgs] / [Bundle]
   */
  fun loadSpaceAndFloor() {
    LOG.V2()

    if(!loadSpaceAndFloorFromAssets()) return

    VM.selectInitialFloor(ctx)

    fHandler.observeFloorChanges(this)
    fHandler.observeFloorplanChanges(obj)
  }

  private fun loadSpaceAndFloorFromAssets() : Boolean {
    LOG.W(TAG, "$METHOD: loading space from assets")
    app.space = assetReader.getSpace()
    app.floors = assetReader.getFloors()

    if (app.space == null || app.floors == null) {
      showError(app.space, app.floors)
      return false
    }

    app.wSpace = SpaceWrapper(ctx, VM.repo, app.space!!)
    app.wFloors = FloorsWrapper(app.floors!!, app.wSpace)
    val prettySpace = app.wSpace.prettyTypeCapitalize
    val prettyFloors= app.wSpace.prettyFloors

    LOG.W(TAG, "$METHOD: loaded: $prettySpace: ${app.space!!.name} " +
            "(has ${app.floors!!.floors.size} $prettyFloors)")

    LOG.W(TAG, "$METHOD: pretty: ${app.wSpace.prettyType} ${app.wSpace.prettyFloor}")

    return true
  }

  private fun showError(space: Space?, floors: Floors?, floor: Floor? = null, floorNum: Int = 0) {
    var msg = ""
    when {
      space == null -> msg = "No space selected."
      floors == null -> msg = "Failed to get ${app.wSpace.prettyFloors}."
      floor == null -> msg = "Failed to get ${app.wSpace.prettyFloor} $floorNum."
    }
    LOG.E(msg)
    Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
  }

  fun removeUserLocations() {
    scope.launch(Dispatchers.Main) {
      hideUserMarkers()
    }
  }

  fun renderUserLocations(userLocation: List<UserLocation>) {
    removeUserLocations() // TODO: update instead of hiding and rendering again..
    // TODO:OPT add all markers at once
    addUserMarkers(userLocation, scope)
  }

  //// GOOGLE MAPS
  // fun addCvMarker(latLng: LatLng, msg: String) { // CLR:PM ?
  //   markers.addCvMarker(latLng, msg)
  // }

  // fun hideCvMarkers() {
  //   markers.hideCvObjMarkers()
  // }

  fun addUserMarkers(userLocation: List<UserLocation>, scope: CoroutineScope) {
    userLocation.forEach {
      scope.launch(Dispatchers.Main) {
        markers.addUserMarker(LatLng(it.x, it.y), it.uid, it.alert, it.time)
      }
    }
  }

  fun hideUserMarkers() {
    markers.hideUserMarkers()
  }

  /**
   * Sets a new marker location on the map.
   *
   * - [userSet]: whether the location was manually added by user
   */
  fun setUserLocation(coord: Coord, manuallySet: Boolean) {
    LOG.D(TAG, "$METHOD")
    markers.setLocationMarker(coord, manuallySet)
  }

  fun recenterCamera(location: LatLng) {
    scope.launch(Dispatchers.Main) {
      obj.animateCamera(
              CameraUpdateFactory.newCameraPosition(
                      CameraPosition(
                              location, obj.cameraPosition.zoom,
                              // don't alter tilt/bearing
                              obj.cameraPosition.tilt,
                              obj.cameraPosition.bearing)))
    }
  }


}