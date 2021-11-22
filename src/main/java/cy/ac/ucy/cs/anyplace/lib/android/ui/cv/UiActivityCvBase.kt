package cy.ac.ucy.cs.anyplace.lib.android.ui.cv

import android.content.Context
import android.graphics.Color
import androidx.camera.core.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.maps.android.heatmaps.Gradient
import com.google.maps.android.heatmaps.HeatmapTileProvider
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers.CvMapHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers.FloorHelper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.maps.Overlays
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.StatusUpdater
import cy.ac.ucy.cs.anyplace.lib.models.CvMap
import kotlinx.coroutines.CoroutineScope

/**
 * CLR:PM either CLR or use it...
 *
 * Encapsulating UI operations for the CV Activities.
 * This allwos sharing code between:
 * - [CvLoggerActivity]
 * - [CvNavigatorActivity] TODO:PM
 */
open class UiActivityCvBase(
  protected val ctx: Context,
  protected val scope: CoroutineScope,
  // private val viewModel: CvLoggerViewModel,
  // private val binding: ActivityCvLoggerBinding,
  protected val statusUpdater: StatusUpdater,
  protected val overlays: Overlays) {


  fun renderHeatmap(gmap: GoogleMap, cvMapH: CvMapHelper?) {
    if (cvMapH == null) {
      LOG.W(TAG, "renderHeatmap: floorHelper or cvMap are null.")
      return
    }

    LOG.D2(TAG, "renderHeatmap: locations ${cvMapH.cvMap.locations.size}")
    overlays.addHeatmap(gmap, cvMapH.getWeightedLocationList())
  }

}