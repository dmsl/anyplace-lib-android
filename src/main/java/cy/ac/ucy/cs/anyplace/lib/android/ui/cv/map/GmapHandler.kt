package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers.*
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GmapHandler(private val ctx: Context,
                  private val scope: CoroutineScope,
                  private val UI: CvMapUi) {

  lateinit var gmap: GoogleMap
  lateinit var VM: CvMapViewModel

  private val assetReader by lazy { AssetReader(ctx) }
  protected val overlays by lazy { Overlays(ctx) }

  protected val fHandler by lazy { FloorHandler(VM, scope, ctx, UI, overlays) }

  /** Dynamically attach a [GoogleMap] */
  fun attach(VM: CvMapViewModel, act: CvMapActivity, layout_id: Int) {
    this.VM=VM
    val mapFragment = SupportMapFragment.newInstance()
    act.supportFragmentManager
            .beginTransaction()
            .add(layout_id, mapFragment)
            .commit()
    mapFragment.getMapAsync(act)
  }

  fun setup(googleMap: GoogleMap) {
    LOG.D()

    gmap = googleMap
    VM.markers = Markers(ctx, gmap)

    val maxZoomLevel = gmap.maxZoomLevel // may be different from device to device

    // TODO Space must be sent here using some SelectSpaceActivity (w/ SafeArgs?)
    // (maybe using Bundle is easier/better)
    loadSpaceAndFloor()

    // TODO:PM this must be moved to earlier activity
    // along with Space/Floors loading (that also needs implementation).
    scope.launch(Dispatchers.IO) { VM.floorsH.fetchAllFloorplans() }

    // place some restrictions on the map
    gmap.moveCamera(CameraUpdateFactory.newCameraPosition(
            CameraAndViewport.loggerCamera(VM.spaceH.latLng(), maxZoomLevel)))
    gmap.setMinZoomPreference(maxZoomLevel-3)

    // restrict screen to current bounds.
    scope.launch {
      delay(500) // CHECK is ths a bugfix?

      if (VM.floorH == null) {
        LOG.E(TAG_METHOD, "Floor is null. Cannot update google map location")
        return@launch
      }

      gmap.moveCamera(CameraUpdateFactory.newLatLngBounds(VM.floorH?.bounds()!!, 0))
      val floorOnScreenBounds = gmap.projection.visibleRegion.latLngBounds
      LOG.D2("bounds: ${floorOnScreenBounds.center}")
      gmap.setLatLngBoundsForCameraTarget(VM.floorH?.bounds())
    }

    gmap.uiSettings.apply {
      isZoomControlsEnabled = false
      isMapToolbarEnabled = false
      isTiltGesturesEnabled = false
      isCompassEnabled = false
      isIndoorLevelPickerEnabled = true
    }

    onMapRreadySpecialize()
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

    fHandler.observeFloorChanges()
    fHandler.observeFloorplanChanges(gmap)
  }

  private fun loadSpaceAndFloorFromAssets() : Boolean {
    LOG.V2()
    VM.space = assetReader.getSpace()
    VM.floors = assetReader.getFloors()

    if (VM.space == null || VM.floors == null) {
      showError(VM.space, VM.floors)
      return false
    }

    VM.spaceH = SpaceHelper(ctx, VM.repoAP, VM.space!!)
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
}