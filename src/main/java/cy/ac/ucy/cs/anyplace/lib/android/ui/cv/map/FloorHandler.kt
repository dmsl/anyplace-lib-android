package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.GoogleMap
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.FloorWrapper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.maps.Overlays
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Floor
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Space
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

open class FloorHandler(
        protected val VM: CvViewModel,
        protected val scope: CoroutineScope,
        protected val ctx: Context,
        private val UI: CvUI,
        /** [GoogleMap] overlays */
        protected val overlays: Overlays
) {

  private val fpLoader by lazy { FloorplanLoader() }
  private val app = VM.app

  val tag = "handler-floor"

  /**
   * Observes when a floorplan changes ([VMB.floorplanFlow]) and loads
   * the new image, and the relevant heatmap
   *
   * This has to be separate
   */
  fun observeFloorplanChanges(gmap: GoogleMap) {
    scope.launch(Dispatchers.IO) {
      VM.floorplanFlow.collect { response ->
        when (response) {
          is NetworkResult.Loading -> {
            LOG.W("Loading ${app.wSpace.prettyFloorplan}..")
          }
          is NetworkResult.Error -> {
            val msg = ": Failed to fetch ${app.wSpace.prettyType}: ${app.space?.name}: [${response.message}]"
            LOG.E(TAG, "Error: observeFloorplanChanges: $msg")
            showToast(msg, Toast.LENGTH_SHORT)
          }
          is NetworkResult.Success -> {
            if (app.wFloor == null) {
              val msg = "No floor/deck selected."
              LOG.W(msg)
              showToast(msg, Toast.LENGTH_SHORT)
            } else {
              LOG.D(TAG, "$METHOD: observeFloorplanChanges: success: loading floorplan")
              fpLoader.render(overlays, gmap, response.data, app.wFloor!!)
              loadHeatmap(gmap)
            }
          }
          // ignore Unset
          else -> {}
        }
      }
    }
  }

  fun showToast(msg: String, len: Int) {
    VM.viewModelScope.launch(Dispatchers.Main) {
      Toast.makeText(ctx, msg, len).show()
    }
  }


  /** loading the very first floor */
  var initialFloor = true
  /**
   * Observe when [VMB.floor] changes and react accordingly:
   * - update [floorSelector] UI (the up/down buttons)
   * - store the last floor selection (for the relevant [Space])
   * - loads the floor
   */
  fun observeFloorChanges(gmapH: GmapWrapper) {
    LOG.D3()
    scope.launch{
      app.floor.collect { selectedFloor ->

        if (initialFloor) {
          gmapH.onFloorLoaded()
          initialFloor = false
        }

        // update FloorHelper & FloorSelector
        app.wFloor = if (selectedFloor != null) FloorWrapper(selectedFloor, app.wSpace) else null
        UI.floorSelector.updateFloorSelector(selectedFloor, app.wFloors)
        LOG.V3(TAG, "observeFloorChanges: -> floor: ${selectedFloor?.floorNumber}")
        if (selectedFloor != null) {
          LOG.V2(TAG,
                  "observeFloorChanges: -> updating cache: floor: ${app.floor.value?.floorNumber}")
          updateAndCacheLastFloor(app.floor.value)
          LOG.V2(TAG, "observeFloorChanges: -> loadFloor: ${selectedFloor.floorNumber}")
          UI.floorSelector.lazilyChangeFloor(VM, scope)
        }
      }
    }
  }

  /**
   * Stores in cache the last selected floor in [VMB.lastValSpaces] (for the relevant [Space])
   */
  private fun updateAndCacheLastFloor(floor: Floor?) {
    LOG.V2(TAG, "$METHOD: ${floor?.floorNumber.toString()}")
    if (floor != null) {
      VM.lastValSpaces.lastFloor=floor.floorNumber
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
    LOG.V3()

    // BUGFIX: artificial delay workaround; could implement this better)
    while (!VM.detectorLoaded) {
      LOG.W(TAG, "$METHOD: waiting for model to be loaded..")
      delay(200)
    }

    // TODO: load fingerprint points..
    val model = VM.model
    if (app.wFloor==null) return
    val FW = app.wFloor!!
    UI.removeHeatmap()
    when {
      false -> { LOG.V3(TAG, "No local CvMap") } // case that has no fingerprints for floor..
      else -> { // all is good, render.
        // VM.cvMapH = CvMapHelperRM(cvMap, VM.detector.labels, FH)
        // VM.cvMapH?.generateCvMapFast()
        UI.renderHeatmap(gmap, null)
      }
    }
  }
}