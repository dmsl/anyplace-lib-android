package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map

import android.content.Context
import com.google.android.gms.maps.GoogleMap
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.maps.Overlays
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Level
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Space
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages the [Overlays] of a level (floor or deck).
 * Overlays are: whatever images are drawn on the map
 * - levelplan: floorplan or deckplan
 * - heatmaps
 *
 * TODO:PMX load levelplan here
 * - LevelPlanLoader: put it in here
 * - Overlays.drawFloorplan
 *
 * also [Overlays] ??
 *
 * NOTE: [MapMarkers] and [MapLines] are not images
 */
open class LevelOverlaysWrapper(
        protected val VM: CvViewModel,
        protected val scope: CoroutineScope,
        protected val ctx: Context,
        private val UI: CvUI,
        /** [GoogleMap] overlays */
        protected val overlays: Overlays
) {

  private val app = VM.app

  val tag = "wr-lvl-overlays"

  var collectingLevelplanChanges = false
  /**
   * Observes when a floorplan changes ([VMB.floorplanFlow]) and loads
   * the new image, and the relevant heatmap
   *
   * This has to be separate.
   */
  fun observeLevelplanImage(gmap: GoogleMap) {
    val method = ::observeLevelplanImage.name

    if (collectingLevelplanChanges) return
    LOG.E(tag, "$method: setup")
    collectingLevelplanChanges=true

    scope.launch(Dispatchers.IO) {
      VM.nwLevelPlan.bitmap.collect { response ->

        LOG.E(tag, "$method: levelplan updated..")
        when (response) {
          is NetworkResult.Loading -> {
            LOG.W(tag, "$method: will load ${app.wSpace.prettyLevelplan}..")
          }
          is NetworkResult.Error -> {
            val msg = ": Failed to fetch ${app.wSpace.prettyType}: ${app.space?.name}: [${response.message}]"
            LOG.E(tag, "Error: $method: $msg")
            app.snackbarShort(scope, msg)
          }
          is NetworkResult.Success -> {
            if (app.wLevel == null) {
              val msg = "No floor/deck selected."
              LOG.W(msg)
              app.snackbarShort(scope, msg)
            } else {
              LOG.E(tag, "$method: success: rendering img")
              VM.nwLevelPlan.render(response.data, app.wLevel!!)
              loadHeatmap(gmap)
            }
          }
          // ignore Unset
          else -> {}
        }
      }
    }
  }

  // fun showToast(msg: String, len: Int) {
  //   VM.viewModelScope.launch(Dispatchers.Main) {
  //     Toast.makeText(ctx, msg, len).show()
  //   }
  // }


  // CLR:PM
  /** loading the very first floor */
  // var isFirstFloor = true
  /**
   * Observe when [VMB.floor] changes and react accordingly:
   * - update [floorSelector] UI (the up/down buttons)
   * - store the last floor selection (for the relevant [Space])
   * - loads the floor
   */
  // fun observeFloorChangesDELETE(map: GmapWrapper) {
  //   LOG.D3()
  //   scope.launch{
  //     app.floor.collect { selectedFloor ->
  //
  //       // update FloorHelper & FloorSelector
  //       // val tmpFloor = FloorWrapper(selectedFloor, app.wSpace)
  //       // UI.floorSelector.updateFloorSelector(selectedFloor, app.wFloors)
  //
  //       // if (isFirstFloor) {
  //       //   isFirstFloor = false
  //       //
  //       //   map.onInitialFloorLoaded()
  //       //
  //       //   LOG.E(tag, "MOVING TO CENTER")
  //       //   map.moveToLocation(app.wFloor!!.bounds().center)
  //       // }
  //
  //       // LOG.V3(tag, "$METHOD: -> floor: ${selectedFloor.floorNumber}")
  //       // LOG.V2(tag, "$METHOD: -> updating cache: floor: ${app.floor.value?.floorNumber}")
  //       // updateAndCacheLastFloor(app.floor.value)
  //       // LOG.V2(tag, "$METHOD: -> loadFloor: ${selectedFloor.floorNumber}")
  //       // UI.floorSelector.lazilyChangeFloor(VM, scope)
  //     }
  //   }
  // }

  /**
   * Stores in cache the last selected floor in [VMB.lastValSpaces] (for the relevant [Space])
   */
  fun cacheLastLevel(level: Level?) {
    val method = ::cacheLastLevel.name
    LOG.V2(tag, "$method: ${level?.number.toString()}")
    if (level != null) {
      VM.lastValSpaces.lastFloor=level.number
      app.wSpace.cacheLastValues(VM.lastValSpaces)
    }
  }


  /**
   * TODO: must be done for FINGERPRINT (SMAS type..)
   *
   * This was done for the [CvMapRM] / [CvMapFastRM] optimized structures
   * that were deleted by this commit.
   *
   * How to implement:
   * - if not prep:
   * - prep: store all points for all floors in mem
   * - load heatmap of cur floor
   */
  suspend fun loadHeatmap(gmap: GoogleMap) {
    LOG.D2()

    // TODO: make this in VM
    // BUGFIX: artificial delay workaround; could implement this better)
    VM.waitForDetector()

    // TODO: load fingerprint points..
    val model = VM.model
    if (app.wLevel==null) return
    val FW = app.wLevel!!
    UI.removeHeatmap()
    when {
      false -> { LOG.V3(tag, "No local CvMap") } // case that has no fingerprints for floor..
      else -> { // all is good, render.
        UI.renderHeatmap(gmap, null)
      }
    }
  }
}