package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.MapBounds
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.maps.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.CvMapActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.selector.space.SelectSpaceActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
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

/**
 * Has all [GoogleMap] related functionality
 * - manages markers, overlays polylines
 *   - these are further broken down into separate classes
 */
class GmapWrapper(
        private val app: AnyplaceApp,
        private val scope: CoroutineScope,
        private val UI: CvUI) {

  companion object {
    private val TG = "wr-map"
    const val ZOOM_LEVEL_DEF = 19f
  }

  private val ctx: Context = app.applicationContext
  private val notify = app.notify
  lateinit var obj: GoogleMap
  lateinit var VM: CvViewModel

  private val assetReader by lazy { AssetReader(ctx) }
  /** Overlays on top of the map, like Heatmaps */
  val overlays by lazy { MapOverlays(ctx, scope) }
  val wrOverlays by lazy { LevelOverlaysWrapper(VM, scope, ctx, UI, overlays) }

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

  var initedGmap = false
  @SuppressLint("PotentialBehaviorOverride")
  fun setup(googleMap: GoogleMap, act: CvMapActivity) {
    val MT = ::setup.name
    LOG.E(TG, MT)

    obj = googleMap
    obj.setInfoWindowAdapter(UserInfoWindowAdapter(ctx))
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

      if (!loadSpace()) {
        var msg = "Cannot load space."
        if (!app.hasInternet()) msg+="\nChoose a differrent one or connect to the internet."
        app.showToastLong(scope, msg)
        app.dsCvMap.clearSelectedSpace() // unselect it
        act.startActivity(Intent(act, SelectSpaceActivity::class.java))
        act.finish()
      }

      setupOnMapAndMarkerClick()
      setupInfoWindowClick()

      // async continues by [onFloorLoaded] CLR?
      // onMapReadySpecialize()
      setupObserverUserBounds()
      initedGmap = true
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
    val method = ::setupInfoWindowClick.name
    if (infoWindowClickSetup) return
    infoWindowClickSetup=true

    scope.launch(Dispatchers.Main) {
      obj.setOnInfoWindowClickListener { // PotentialBehaviorOverride
        markers.clearAllInfoWindow() // clear any previous InfoWindows
        val metadata = it.tag as UserInfoMetadata?
        if (metadata != null) {
          if(metadata.type == UserInfoType.SharedLocation) {
            LOG.W(TG, "$method: clearing chat location marker")
            markers.clearChatLocationMarker()
            return@setOnInfoWindowClickListener
          }

          LOG.W(TG, "$method: USER: ${metadata.uid}")
          LOG.W(TG, "$method: LOCATION: ${metadata.coord}")

          val uid = metadata.uid
          val deck = metadata.coord.level
          val lat = metadata.coord.lat
          val lon = metadata.coord.lon

          if (UserInfoWindowAdapter.isUserLocation(metadata.type)) {
            val clipboardLocation = SmasChatViewModel.ClipboardLocation(uid, deck, lat, lon)
            clipboardLocation.toString().copyToClipboard(ctx)

            scope.launch(Dispatchers.IO) {
              val ownUid = app.dsUserSmas.read.first().uid
              if (UserInfoWindowAdapter.isUserLocation(metadata.type)) {
                if (ownUid == uid) {
                  notify.short(scope, "Copied own location to clipboard")
                } else {
                  notify.short(scope, "Copied ${metadata.uid}'s location to clipboard")
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
   * when the first [Level] is loaded, it continues the setup by:
   * - bringing the floor plans (downloading or from cache)
   * - setting up zoom preferences
   */
  fun onFirstLevelLoaded() {
    val MT = ::onFirstLevelLoaded.name
    LOG.I(TG, MT)

    scope.launch(Dispatchers.IO) { VM.nwLevelPlan.downloadAll() }

    scope.launch(Dispatchers.Main) {
      val maxZoomLevel = obj.maxZoomLevel // may differ per device
      obj.setMinZoomPreference(maxZoomLevel-4)
      LOG.V2(TG, "MAX ZOOM: $maxZoomLevel (restriction)")

      if (app.wLevel == null) {
        val msg = "Cannot locate floor ($TG)"
        notify.longDEV(scope, msg)
        LOG.E(TG, msg)
        return@launch
      }

      LOG.D3(TG, "$MT: SPACE: ${app.wSpace.obj.name} LVL: ${app.wLevel?.prettyLevelNumber()}")
      VM.ui.map.moveToBounds(app.wLevel!!.bounds())
    }
  }

  /**
   * Loads some necessary json objects for:
   * - the space itself
   * - the levels (floors/decks) of the building
   * 
   * It can load:
   * - dynamically (from cache, given they were already downloaded)
   * - of from assets (used for development/debugging)
   */
  suspend fun loadSpace() : Boolean {
    LOG.W()
    try {
      if (!DBG.USE_SPACE_SELECTOR) {
        if(!loadSpaceFromAssets()) return false
      } else {
        val prefs = VM.dsCvMap.read.first()
        if(!loadSpaceFromCache(prefs.selectedSpace)) return false
      }

      VM.selectInitialLevel()
      wrOverlays.observeLevelplanImage(obj)
      return true
    } catch (e: Exception) {
      return false
    }
  }

  /**
   * Cache is prepared by [SpaceSelector]
   */
  private fun loadSpaceFromCache(selectedSpace: String): Boolean {
    val method = ::loadSpaceFromCache.name
    LOG.W(TG, "$method: $selectedSpace (DYNAMICALLY)")
    return app.loadSpace(scope,
            VM.cache.readJsonSpace(selectedSpace),
            VM.cache.readJsonFloors(selectedSpace))
  }


  @Deprecated("for testing")
  private fun loadSpaceFromAssets() : Boolean {
    val MT = ::loadSpaceFromAssets.name
    LOG.E(TG, MT)
    return app.loadSpace(scope,
            assetReader.getSpace(),
            assetReader.getFloors())
  }

  var setUserPannedOutOfBounds = false
  /**
   * Observe user bounds to update WAI functionality
   */
  fun setupObserverUserBounds() {
    val method = ::setupObserverUserBounds.name
    if (setUserPannedOutOfBounds) return
    setUserPannedOutOfBounds=true

    scope.launch(Dispatchers.Main) {
      obj.setOnCameraIdleListener {
        val zoomLevel = obj.cameraPosition.zoom
        var boundsState = MapBounds.notLocalizedYet
        LOG.V3(TG, "$method: map idle: zoom: $zoomLevel") // CLR:PM D3
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
   * moves camera when the [latLng] is out of bounds:
   * - not visible in current map's projection
   */
  fun moveIfOutOufBounds(latLng: LatLng) {
    scope.launch(Dispatchers.Main) {
      if (outOfMapBounds(latLng)) {
        moveToLocation(latLng)
      }
    }
  }

  /**
   * Set the camera to an area that the whole [bounds] are visible
   */
  fun moveToBounds(bounds: LatLngBounds, padding: Int = 10) {
    scope.launch(Dispatchers.Main) {
      obj.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
    }
  }

  /**
   * move camera to [latLong], optionally changing zoom level
   */
  fun moveToLocation(latLng: LatLng, changeZoomLevel: Boolean = false) {
    val method = ::moveToLocation.name
    scope.launch(Dispatchers.Main) {
      LOG.D2(TG, "$method: zoom: ${obj.cameraPosition.zoom}")

      val zoomLevel = if (changeZoomLevel) ZOOM_LEVEL_DEF else obj.cameraPosition.zoom

      // don't alter tilt, bearing
      val oldPos = obj.cameraPosition
      val newPos = CameraPosition(latLng, zoomLevel, oldPos.tilt, oldPos.bearing)
      obj.moveCamera(CameraUpdateFactory.newCameraPosition(newPos))
    }
  }

  fun animateToCenter(location: LatLng) {
    scope.launch(Dispatchers.Main) {

      val oldPos = obj.cameraPosition
      // don't alter tilt/bearing
      val newPos = CameraPosition(location, oldPos.zoom, oldPos.tilt, oldPos.bearing)
      obj.animateCamera(CameraUpdateFactory.newCameraPosition(newPos))
    }
  }

  fun removeUserLocations() {
    scope.launch(Dispatchers.Main) { hideUserMarkers() }
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
   * - [locMethod]: what localization method was used
   */
  fun setUserLocation(coord: Coord, locMethod: LocalizationMethod) {
    val method = ::setUserLocation.name
    LOG.D(TG, method)
    markers.setOwnLocationMarker(coord, locMethod, app.alerting)
  }
}