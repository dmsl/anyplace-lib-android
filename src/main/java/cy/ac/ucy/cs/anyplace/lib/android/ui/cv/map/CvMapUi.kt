package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map

import android.app.Activity
import android.content.Context
import androidx.fragment.app.FragmentManager
import com.google.android.gms.maps.GoogleMap
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.CvMapHelper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.maps.Overlays
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.FloorSelector
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
 * - floors
 * - floorplans
 */
open class CvMapUi(
        protected val VM: CvViewModel,
        protected val scope: CoroutineScope,
        protected val activity: Activity,
        protected val fragmentManager: FragmentManager,
        /** [GoogleMap] overlays */
        protected val overlays: Overlays,
        val floorSelector: FloorSelector) {

  protected val ctx: Context = activity.applicationContext

  fun setupOnFloorSelectionClick(){
    floorSelector.onFloorDown { VM.wFloors.tryGoDown(VM) }
    floorSelector.onFloorUp { VM.wFloors.tryGoUp(VM) }
  }

  fun removeHeatmap() {
    scope.launch(Dispatchers.Main) {
      overlays.uiRemoveHeatmap()
    }
  }

  fun renderHeatmap(map: GoogleMap, cvMapH: CvMapHelper?) {
    if (cvMapH == null) {
      LOG.W(TAG, "renderHeatmap: floorHelper or cvMap are null.")
      return
    }

    LOG.D2(TAG, "renderHeatmap: locations ${cvMapH.cvMap.locations.size}")
    overlays.createHeatmap(map, cvMapH.getWeightedLocationList())
  }

}