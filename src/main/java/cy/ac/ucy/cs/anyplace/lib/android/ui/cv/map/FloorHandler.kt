package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map

import android.content.Context
import android.widget.Toast
import com.google.android.gms.maps.GoogleMap
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.models.helpers.CvMapFast
import cy.ac.ucy.cs.anyplace.lib.android.data.models.helpers.CvMapHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.models.helpers.FloorHelper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.maps.Overlays
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.CvMapViewModel
import cy.ac.ucy.cs.anyplace.lib.models.CvMap
import cy.ac.ucy.cs.anyplace.lib.models.Floor
import cy.ac.ucy.cs.anyplace.lib.models.Space
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

open class FloorHandler(
        protected val VM: CvMapViewModel,
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
    scope.launch {
      VM.floorplanFlow.collect { response ->
        when (response) {
          is NetworkResult.Loading -> {
            LOG.W("Loading ${VM.spaceH.prettyFloorplan}")
          }
          is NetworkResult.Error -> {
            val msg = "Failed to fetch ${VM.spaceH.prettyType}: ${VM.space?.name}"
            LOG.E(msg)
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
          }
          is NetworkResult.Success -> {
            if (VM.floorH == null) {
              val msg = "No floor/deck selected."
              LOG.W(msg)
              Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
            } else {
              fpLoader.render(overlays, gmap, response.data, VM.floorH!!)
              loadCvMapAndHeatmap(gmap)
            }
          }
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
  fun observeFloorChanges(gmapH: GmapHandler) {
    LOG.W()
    scope.launch{
      VM.floor.collect { selectedFloor ->

        if (initialFloor) {
          gmapH.onFloorLoaded()
          initialFloor = false
        }

        // update FloorHelper & FloorSelector
        VM.floorH = if (selectedFloor != null) FloorHelper(selectedFloor, VM.spaceH) else null
        UI.floorSelector.updateFloorSelector(selectedFloor, VM.floorsH)
        LOG.E(TAG, "observeFloorChanges: -> floor: ${selectedFloor?.floorNumber}")
        if (selectedFloor != null) {
          LOG.W(TAG,
                  "observeFloorChanges: -> updating cache: floor: ${VM.floor.value?.floorNumber}")
          updateAndCacheLastFloor(VM.floor.value)
          LOG.W(TAG, "observeFloorChanges: -> loadFloor: ${selectedFloor.floorNumber}")
          UI.floorSelector.lazilyChangeFloor(VM, scope)
        }
      }
    }
  }

  /**
   * Stores in cache the last selected floor in [VMB.lastValSpaces] (for the relevant [Space])
   */
  private fun updateAndCacheLastFloor(floor: Floor?) {
    LOG.W(TAG_METHOD, floor?.floorNumber.toString())
    if (floor != null) {
      VM.lastValSpaces.lastFloor=floor.floorNumber
      VM.spaceH.cacheLastValues(VM.lastValSpaces)
    }
  }



  /**
   * Reads the [CvMap] from cache and if it exists it:
   * - parses it into the optimized [CvMapFast] structure
   * - it renders a heatmap of the detections
   */
  fun loadCvMapAndHeatmap(gmap: GoogleMap) {
    LOG.E()
    val model = VM.model // TODO
    if (VM.floorH==null) return
    val FH = VM.floorH!!
    val cvMap = if (FH.hasFloorCvMap(model)) VM.floorH?.loadCvMapFromCache(model) else null
    UI.removeHeatmap()
    when {
      !FH.hasFloorCvMap(model) -> { LOG.W(TAG, "No local CvMap") }
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