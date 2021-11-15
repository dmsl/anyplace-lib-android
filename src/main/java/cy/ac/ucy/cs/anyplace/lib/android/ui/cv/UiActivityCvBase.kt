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
  protected val overlays: Overlays,
) {

  private fun getCvHeatmapGradient() : Gradient {
    val colors = intArrayOf(
      // Color.rgb(127, 114, 170), // light purple
      // Color.rgb(6, 135, 142), // green/blue0
      // Color.rgb(255, 203, 91), // yellow
      Color.rgb(255, 255, 255), // white
      Color.rgb(7, 145, 84), // green/blue0
    )
    val startPoints = floatArrayOf(0.5f, 1f)
    // val startPoints = floatArrayOf(0.2f, 0.4f, 1f)
    return Gradient(colors, startPoints)
  }


  fun renderHeatmap(gmap: GoogleMap, floorH: FloorHelper?, cvMap: CvMap?) {
    if (floorH == null || cvMap == null) {
      LOG.W(TAG, "renderHeatmap: floorHelper or cvMap are null.")
      return
    }
    val cvMapH = CvMapHelper(cvMap, floorH)
    LOG.D(TAG, "renderHeatmap")
    LOG.E(TAG, "CV MAP:")
    LOG.E(TAG, cvMap.toString())

    overlays.addHeatmap(gmap, cvMapH.getWeightedLocationList(), getCvHeatmapGradient())
  }

}