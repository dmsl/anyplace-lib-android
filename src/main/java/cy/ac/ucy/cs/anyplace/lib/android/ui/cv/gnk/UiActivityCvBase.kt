package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.gnk

import android.app.Activity
import android.content.Context
import androidx.camera.core.*
import androidx.fragment.app.FragmentManager
import com.google.android.gms.maps.GoogleMap
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers.CvMapHelper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.maps.Overlays
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.FloorSelector
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.gnk.logger.CvLoggerActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.gnk.logger.StatusUpdater
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.CvViewModelBase
import kotlinx.coroutines.CoroutineScope

/**
 * Encapsulating UI operations for the CV Activities.
 * This allows sharing code between:
 * - [CvLoggerActivity]
 * - [CvNavigatorActivity] TODO:PM
 *
 *
 * CHECK: add also gmap markers here?
 */
open class UiActivityCvBase(
        protected val activity: Activity,
        protected val fragmentManager: FragmentManager,
        protected val VMb: CvViewModelBase,
        protected val scope: CoroutineScope,
        protected val statusUpdater: StatusUpdater,
        /** [GoogleMap] overlays */
        protected val overlays: Overlays,
        protected val floorSelector: FloorSelector) {

  protected val ctx: Context = activity.applicationContext

  fun setupOnFloorSelectionClick(){
    floorSelector.onFloorDown { VMb.floorGoDown() }
    floorSelector.onFloorUp { VMb.floorGoUp() }
  }

  fun removeHeatmap() {
    overlays.removeHeatmap()
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