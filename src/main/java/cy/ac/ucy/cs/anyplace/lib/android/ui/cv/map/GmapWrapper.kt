package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.MapBounds
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.copyToClipboard
import cy.ac.ucy.cs.anyplace.lib.android.maps.*
import cy.ac.ucy.cs.anyplace.lib.android.maps.camera.CameraAndViewport
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.CvMapActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
import cy.ac.ucy.cs.anyplace.lib.android.utils.demo.AssetReader
import cy.ac.ucy.cs.anyplace.lib.android.utils.toLatLng
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasChatViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationMethod
import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationResult
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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
  @SuppressLint("PotentialBehaviorOverride")
  fun setup(googleMap: GoogleMap, act: CvMapActivity) {
    LOG.D()
    LOG.E(TAG, "$tag: setup OF GMAP-WRAPPER")

    obj = googleMap
    obj.setInfoWindowAdapter(UserInfoWindowAdapter(ctx))   // TODO:PMX FR10
    markers = MapMarkers(app, scope, VM, this)
    lines = MapLines(app, scope, VM, this)

    // ON FLOOR LOADED....
    obj.uiSettings.apply {
      isZoomControlsEnabled = false
      isMapToolbarEnabled = false
      isTiltGesturesEnabled = false
      isCompassEnabled = false
      isIndoorLevelPickerEnabled = false
    }

    // (maybe using Bundle is easier/better)
    scope.launch(Dispatchers.IO) {
      // wait for prefs to be loaded: the user might not have selected a space
      // in that case the activity will close (once prefs are ready)
      // otherwise, we'll continue loading the space / level
      while(!act.cvMapPrefsLoaded) delay(100)

      loadSpaceAndLevel()

      setupOnMapAndMarkerClick()
      setupInfoWindowClick()

      // async continues by [onFloorLoaded] CLR?
      // onMapReadySpecialize()
      setupObserverUserBounds()
      gmapWrLoaded = true
    }
  }

  @SuppressLint("PotentialBehaviorOverride")
  private fun setupOnMapAndMarkerClick() {
    scope.launch(Dispatchers.Main) {
      obj.setOnMapClickListener {
        markers.clearAllInfoWindow()
      }

      obj.setOnMarkerClickListener {  // PotentialBehaviorOverride
        markers.clearAllInfoWindow()
        return@setOnMarkerClickListener false
      }
    }
  }

  var infoWindowClickSetup=false
  @SuppressLint("PotentialBehaviorOverride")
  private fun setupInfoWindowClick() {
    if (infoWindowClickSetup) return
    infoWindowClickSetup=true

    scope.launch(Dispatchers.Main) {
      obj.setOnInfoWindowClickListener { // PotentialBehaviorOverride
        markers.clearAllInfoWindow() // clear any previous InfoWindows
        val metadata = it.tag as UserInfoMetadata?
        if (metadata != null) {
          if(metadata.type == UserInfoType.SharedLocation) {
            LOG.W(TAG, "clearing chat location marker")
            markers.clearChatLocationMarker()
            return@setOnInfoWindowClickListener
          }

          LOG.E(TAG, "USER: ${metadata.uid}")
          LOG.E(TAG, "LOCATION: ${metadata.coord}")

          val uid = metadata.uid
          val deck = metadata.coord.level
          val lat = metadata.coord.lat
          val lon = metadata.coord.lon

          if (UserInfoWindowAdapter.isUserLocation(metadata.type)) {

            val clipboardLocation = SmasChatViewModel.ClipboardLocation(uid, deck, lat, lon)
            clipboardLocation.toString().copyToClipboard(ctx)

            scope.launch(Dispatchers.IO) {
              val ownUid = app.dsSmasUser.read.first().uid
              if (UserInfoWindowAdapter.isUserLocation(metadata.type)) {
                if (ownUid == uid) {
                  app.snackbarShort(scope, "Copied own location to clipboard")
                } else {
                  app.snackbarShort(scope, "Copied ${metadata.uid}'s location to clipboard")
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   * Whether the [latLng] is out of the projected region of the map
   */
  fun outOfMapBounds(latLng: LatLng) : Boolean {
    val bounds = obj.projection.visibleRegion.latLngBounds
    return !bounds.contains(latLng)
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

      LOG.W(TAG, "Setting camera bounds: users will be restricted to viewing just the particular space")
      LOG.E(TAG, "SP: ${app.wSpace.obj.name}")
      LOG.E(TAG, "FLOOR: ${app.wFloor?.prettyFloorNumber()}")

      obj.setLatLngBoundsForCameraTarget(app.wFloor?.bounds())
      animateToLocation(app.wFloor?.bounds()!!.center)

      val floorOnScreenBounds = obj.projection.visibleRegion.latLngBounds
      LOG.D2("bounds: ${floorOnScreenBounds.center}")
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
  fun loadSpaceAndLevel() {
    LOG.V2()

    scope.launch(Dispatchers.IO) {
      if (!DBG.SLR) {
        if(!loadSpaceAndFloorFromAssets()) return@launch
      } else {
        val prefs = VM.dsCvMap.read.first()
        LOG.W(TAG, "$METHOD: loading dynamically: ${prefs.selectedSpace}")
        loadSpaceAndFloorFromCache(prefs.selectedSpace)
      }

      VM.selectInitialFloor(ctx)
      fHandler.observeFloorChanges(this@GmapWrapper)
      fHandler.observeFloorplanChanges(obj)
    }
  }

  /**
   * Cache is prepared by [SpaceSelector]
   * TODO put other act here
   */
  private fun loadSpaceAndFloorFromCache(selectedSpace: String): Boolean {
    LOG.E(TAG, "$METHOD: space: $selectedSpace (DYNAMICALLY)")
    return app.initSpaceAndFloors(scope,
            VM.cache.readJsonSpace(selectedSpace),
            VM.cache.readJsonFloors(selectedSpace))
  }


  @Deprecated("for testing")
  private fun loadSpaceAndFloorFromAssets() : Boolean {
    LOG.W()
    return app.initSpaceAndFloors(scope,
            assetReader.getSpace(),
            assetReader.getFloors())
  }


  var setUserPannedOutOfBounds = false
  /**
   * Observe user bounds to update WAI functionality
   */
  fun setupObserverUserBounds() {
    if (setUserPannedOutOfBounds) return
    setUserPannedOutOfBounds=true

    scope.launch(Dispatchers.Main) {
      obj.setOnCameraIdleListener {
        var boundsState = MapBounds.notLocalizedYet
        LOG.W(TAG, "Map idle (movement ended)") // CLR:PM D3
        val lr = app.locationSmas.value
        if (lr is LocalizationResult.Success) {  // there was a user location
          // NOTE: on floor changing the [app.userOutOfBounds] might also get updated,
          // because the user might change the floor, without any panning on the map

          val latLng = lr.coord!!.toLatLng()
          boundsState =
                  if (app.userOnOtherFloor() || outOfMapBounds(latLng)) MapBounds.outOfBounds
                  else MapBounds.inBounds
        }
        app.userOutOfBounds.update { boundsState }
      }
    }
  }

  /**
   * Pan camera to new location
   */
  fun animateToLocation(latLng: LatLng) {
    // nice animation but it causes issues..
    // map.obj.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, map.obj.maxZoomLevel))

    scope.launch(Dispatchers.Main) {
      val cameraPosition = CameraPosition(
              latLng, obj.cameraPosition.zoom,
              // don't alter tilt/bearing
              obj.cameraPosition.tilt,
              obj.cameraPosition.bearing)

      // val cancellableCallback= object : GoogleMap.CancelableCallback {
      //   override fun onCancel() {}
      //   override fun onFinish() {}
      // }

      val newCameraPosition = CameraUpdateFactory.newCameraPosition(cameraPosition)
      obj.moveCamera(newCameraPosition)
    }
  }

  private fun showError(space: Space?, floors: Floors?, floor: Floor? = null, floorNum: Int = 0) {
    var msg = ""
    when {
      space == null -> msg = "No space selected."
      floors == null -> msg = "Failed to get ${app.wSpace.prettyFloors}."
      floor == null -> msg = "Failed to get ${app.wSpace.prettyFloor} $floorNum."
    }
    LOG.E(msg)
    app.snackbarShort(scope, msg)
  }

  fun removeUserLocations() {
    scope.launch(Dispatchers.Main) {
      hideUserMarkers()
    }
  }

  fun renderUserLocations(userLocation: List<UserLocation>) {
    removeUserLocations()
    addUserMarkers(userLocation, scope)
  }

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
  fun setUserLocation(coord: Coord, locMethod: LocalizationMethod) {
    LOG.D(TAG, "$METHOD")
    markers.setOwnLocationMarker(coord, locMethod, app.alerting)
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