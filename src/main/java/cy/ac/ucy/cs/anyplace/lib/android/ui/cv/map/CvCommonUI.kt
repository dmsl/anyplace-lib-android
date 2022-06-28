package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map

import android.app.Activity
import android.content.Context
import androidx.fragment.app.FragmentManager
import com.google.android.gms.maps.GoogleMap
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.CvMapHelper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.FloorSelector
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.UiLocalization
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Encapsulating operations for the CVMap UI
 *
 * TODO:
 * // More functionality from GmapHandler
 * // OR break up [GmapWrapper] to:
 * - GmapWrapper (that holds Overlays)
 * - floors
 * - floorplans
 * - floorSelector
 */
open class CvCommonUI(
        protected val VM: CvViewModel,
        protected val scope: CoroutineScope,
        protected val activity: Activity,
        protected val fragmentManager: FragmentManager,
        val floorSelector: FloorSelector
        ) {

  protected val ctx: Context = activity.applicationContext
  /** Google Maps Wrapper */
  val map by lazy { GmapWrapper(activity, scope, this) }

  /** Localization Button Wrapper */
  val localization by lazy { UiLocalization(activity, VM, scope, map, R.id.btn_localization) }

  /**
   * MERGE:PM once image is analyzed
   * Used to be inside analyzeImage I think
   */
  open fun onInferenceRan() {
    LOG.D2(TAG, "$METHOD: CvMapUi")
    // scope.launch(Dispatchers.Main) {
      // TODO: binding bottom sheet stats..
      // bottom.tvTimeInfo.text =  "<TODO>ms" // "${detectionTime}ms" // TODO:PM timer?
      // bottom.bindCvStats()
      // bindCvStatsImgDimensions(image) // and do this once. not on each analyze
    // }
  }

  fun setupOnFloorSelectionClick(){
    floorSelector.onFloorDown { VM.wFloors.tryGoDown(VM) }
    floorSelector.onFloorUp { VM.wFloors.tryGoUp(VM) }
  }

  fun removeHeatmap() {
    scope.launch(Dispatchers.Main) {
      map.overlays.uiRemoveHeatmap()
    }
  }

  fun renderHeatmap(map: GoogleMap, cvMapH: CvMapHelper?) {
    if (cvMapH == null) {
      LOG.W(TAG, "renderHeatmap: floorHelper or cvMap are null.")
      return
    }

    LOG.D2(TAG, "renderHeatmap: locations ${cvMapH.cvMap.locations.size}")
    this.map.overlays.createHeatmap(map, cvMapH.getWeightedLocationList())
  }

}