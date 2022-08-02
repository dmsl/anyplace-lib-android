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
  private val TG = "wr-lvl-overlays"
  private val app = VM.app
  private val notify = app.notify


  var collectingLevelplanChanges = false
  /**
   * Observes when a floorplan changes ([VMB.floorplanFlow]) and loads
   * the new image, and the relevant heatmap
   *
   * This has to be separate.
   */
  fun observeLevelplanImage(gmap: GoogleMap) {
    val MT = ::observeLevelplanImage.name

    if (collectingLevelplanChanges) return
    collectingLevelplanChanges=true
    LOG.E(TG, "$MT: setup")

    scope.launch(Dispatchers.IO) {
      VM.nwLevelPlan.bitmap.collect { response ->

        LOG.W(TG, "$MT: levelplan updated..")
        when (response) {
          is NetworkResult.Loading -> {
            LOG.W(TG, "$MT: will load ${app.wSpace.prettyLevelplan}..")
          }
          is NetworkResult.Error -> {
            val msg = ": Failed to fetch ${app.wSpace.prettyType}: ${app.space?.name}: [${response.message}]"
            LOG.E(TG, "Error: $MT: $msg")
            notify.short(scope, msg)
          }
          is NetworkResult.Success -> {
            if (app.wLevel == null) {
              val msg = "No floor/deck selected."
              LOG.W(msg)
              notify.short(scope, msg)
            } else {
              LOG.E(TG, "$MT: success: rendering img")
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

  /**
   * Stores in cache the last selected floor in [VMB.lastValSpaces] (for the relevant [Space])
   */
  fun cacheLastLevel(level: Level?) {
    val MT = ::cacheLastLevel.name
    LOG.W(TG, "$MT: ${level?.number.toString()}")
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
    val MT = ::loadHeatmap.name
    LOG.D2(TG, MT)

    VM.waitForDetector()  // workaround

    // TODO: load fingerprint points..
    val model = VM.model
    if (app.wLevel==null) return
    UI.removeHeatmap()
    // all is good, render.
    UI.renderHeatmap(gmap, null)
  }
}