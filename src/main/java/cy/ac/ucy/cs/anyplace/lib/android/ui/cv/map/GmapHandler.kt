package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.models.helpers.*
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.maps.Markers
import cy.ac.ucy.cs.anyplace.lib.android.maps.Overlays
import cy.ac.ucy.cs.anyplace.lib.android.maps.camera.CameraAndViewport
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.CvMapActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.demo.AssetReader
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.CvMapViewModel
import cy.ac.ucy.cs.anyplace.lib.models.Floor
import cy.ac.ucy.cs.anyplace.lib.models.Floors
import cy.ac.ucy.cs.anyplace.lib.models.Space
import cy.ac.ucy.cs.anyplace.lib.models.UserLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GmapHandler(private val ctx: Context,
                  private val scope: CoroutineScope,
                  private val UI: CvMapUi) {

  lateinit var obj: GoogleMap
  lateinit var VM: CvMapViewModel

  private val assetReader by lazy { AssetReader(ctx) }
  private val overlays by lazy { Overlays(ctx) }
  private val fHandler by lazy { FloorHandler(VM, scope, ctx, UI, overlays) }

  /** Dynamically attach a [GoogleMap] */
  fun attach(VM: CvMapViewModel, act: CvMapActivity, layout_id: Int) {
    this.VM=VM
    val mapFragment = SupportMapFragment.newInstance()
    act.supportFragmentManager
            .beginTransaction()
            .add(layout_id, mapFragment)
            .commit()
    scope.launch(Dispatchers.Main) {
      mapFragment.getMapAsync(act)
    }
  }

  fun setup(googleMap: GoogleMap) {
    LOG.D()

    obj = googleMap
    VM.markers = Markers(ctx, obj)

    // ON FLOOR LOADED....
    obj.uiSettings.apply {
      isZoomControlsEnabled = false
      isMapToolbarEnabled = false
      isTiltGesturesEnabled = false
      isCompassEnabled = false
      isIndoorLevelPickerEnabled = false
    }

    onMapRreadySpecialize()

    // TODO Space must be sent here using some SelectSpaceActivity (w/ SafeArgs?)
    // (maybe using Bundle is easier/better)
    loadSpaceAndFloor()

    // async continues by [onFloorLoaded]
  }

  /**
   * when the [Floor] is loaded, it continues the setup by:
   * - bringing the floor plans (downloading or from cache)
   * - setting up zoom preferences
   */
  fun onFloorLoaded() {
    // TODO:PM this must be moved to earlier activity
    // along with Space/Floors loading (that also needs implementation).
    scope.launch(Dispatchers.IO) { VM.floorsH.fetchAllFloorplans() }

    val maxZoomLevel = obj.maxZoomLevel // may be different from device to device

    obj.setMinZoomPreference(maxZoomLevel-4)
    // place some restrictions on the map
    LOG.D2(TAG, "MAX ZOOM: $maxZoomLevel (restriction)")

    // restrict screen to current bounds.
    scope.launch {
      delay(500) // CHECK is ths a bugfix?

      if (VM.floorH == null) {
        LOG.E(TAG_METHOD, "Floor is null. Cannot update google map location")
        return@launch
      }

      // zoom ins to include the floorplan
      // gmap.moveCamera(CameraUpdateFactory.newLatLngBounds(VM.floorH?.bounds()!!, 0))

      // zooms to the center of the floorplan
      obj.moveCamera(CameraUpdateFactory.newCameraPosition(
              CameraAndViewport.loggerCamera(VM.floorH?.bounds()!!.center, maxZoomLevel-2)))

      val floorOnScreenBounds = obj.projection.visibleRegion.latLngBounds
      LOG.D2("bounds: ${floorOnScreenBounds.center}")
      obj.setLatLngBoundsForCameraTarget(VM.floorH?.bounds())
    }
  }

  protected fun onMapRreadySpecialize() {
  }

  /**
   * Loads from assets the Space and the Space's Floors
   * Then it loads the floorplan for [selectedFloorPlan].
   *
   * TODO Implement this from network (@earlier), and pass it w/[SafeArgs] / [Bundle]
   */
  fun loadSpaceAndFloor() {
    LOG.E()

    if(!loadSpaceAndFloorFromAssets()) return

    VM.selectInitialFloor(ctx)

    fHandler.observeFloorChanges(this)
    fHandler.observeFloorplanChanges(obj)
  }

  private fun loadSpaceAndFloorFromAssets() : Boolean {
    LOG.V2()
    VM.space = assetReader.getSpace()
    VM.floors = assetReader.getFloors()

    if (VM.space == null || VM.floors == null) {
      showError(VM.space, VM.floors)
      return false
    }

    VM.spaceH = SpaceHelper(ctx, VM.repo, VM.space!!)
    VM.floorsH = FloorsHelper(VM.floors!!, VM.spaceH)
    val prettySpace = VM.spaceH.prettyTypeCapitalize
    val prettyFloors= VM.spaceH.prettyFloors

    LOG.D3(TAG_METHOD, "$prettySpace: ${VM.space!!.name} " +
            "(has ${VM.floors!!.floors.size} $prettyFloors)")

    return true
  }

  private fun showError(space: Space?, floors: Floors?, floor: Floor? = null, floorNum: Int = 0) {
    var msg = ""
    when {
      space == null -> msg = "No space selected."
      floors == null -> msg = "Failed to get ${VM.spaceH.prettyFloors}."
      floor == null -> msg = "Failed to get ${VM.spaceH.prettyFloor} $floorNum."
    }
    LOG.E(msg)
    Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
  }

  fun removeUserLocations() {
    scope.launch(Dispatchers.Main) {
      VM.hideUserMarkers()
    }
  }

  fun renderUserLocations(userLocation: List<UserLocation>) {
    removeUserLocations() // TODO: update instead of hiding and rendering again..
    // TODO:OPT add all markers at once
      VM.addUserMarkers(userLocation, scope)
  }
}