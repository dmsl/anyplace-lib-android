package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map

import android.content.Context
import android.widget.Toast
import com.google.android.gms.maps.GoogleMap
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.CvMapFast
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.CvMapHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.FloorWrapper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.maps.Overlays
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.CvMap
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
  private val UI: CvMapUi,
  /** [GoogleMap] overlays */
        protected val overlays: Overlays
        ) {

  private val fpLoader by lazy { FloorplanLoader() }

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
            LOG.W("Loading ${VM.wSpace.prettyFloorplan}..")
          }
          is NetworkResult.Error -> {
            val msg = ": Failed to fetch ${VM.wSpace.prettyType}: ${VM.space?.name}: [${response.message}]"
            LOG.E(TAG, "Error: observeFloorplanChanges: $msg")
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
          }
          is NetworkResult.Success -> {
            if (VM.wFloor == null) {
              val msg = "No floor/deck selected."
              LOG.W(msg)
              Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
            } else {
              LOG.E(TAG, "observeFloorplanChanges: success: loading floorplan")
              fpLoader.render(overlays, gmap, response.data, VM.wFloor!!)
              loadCvMapAndHeatmap(gmap)
            }
          }
          // ignore Unset
          else -> {}
        }
      }
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
      VM.floor.collect { selectedFloor ->

        if (initialFloor) {
          gmapH.onFloorLoaded()
          initialFloor = false
        }

        // update FloorHelper & FloorSelector
        VM.wFloor = if (selectedFloor != null) FloorWrapper(selectedFloor, VM.wSpace) else null
        UI.floorSelector.updateFloorSelector(selectedFloor, VM.wFloors)
        LOG.V3(TAG, "observeFloorChanges: -> floor: ${selectedFloor?.floorNumber}")
        if (selectedFloor != null) {
          LOG.V2(TAG,
                  "observeFloorChanges: -> updating cache: floor: ${VM.floor.value?.floorNumber}")
          updateAndCacheLastFloor(VM.floor.value)
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
      VM.wSpace.cacheLastValues(VM.lastValSpaces)
    }
  }



  /**
   * Reads the [CvMap] from cache and if it exists it:
   * - parses it into the optimized [CvMapFast] structure
   * - it renders a heatmap of the detections
   */
  suspend fun loadCvMapAndHeatmap(gmap: GoogleMap) {
    LOG.V3()

    // BUGFIX: artificial delay workaround; could implement this better)
   while (!VM.modelLoaded) {
     LOG.W(TAG, "$METHOD: waiting for model to be loaded..")
     delay(200)
   }

    val model = VM.model // TODO
    if (VM.wFloor==null) return
    val FH = VM.wFloor!!
    val cvMap = if (FH.hasFloorCvMap(model)) VM.wFloor?.loadCvMapFromCache(model) else null
    UI.removeHeatmap()
    when {
      !FH.hasFloorCvMap(model) -> { LOG.V3(TAG, "No local CvMap") }
      cvMap == null -> { LOG.W(TAG, "Can't load CvMap") }
      cvMap.schema < CvMap.SCHEMA -> {
        LOG.W(TAG, "CvMap outdated: version: ${cvMap.schema} (current: ${CvMap.SCHEMA}")
        LOG.E(TAG, "outdated cv-map")
        FH.clearCacheCvMaps()
      }
      else -> { // all is good, render.
        VM.cvMapH = CvMapHelper(cvMap, VM.detector.labels, FH)
        VM.cvMapH?.generateCvMapFast()
        UI.renderHeatmap(gmap, VM.cvMapH)
      }
    }
  }
}