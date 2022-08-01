package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map

import android.graphics.Bitmap
import com.google.android.gms.maps.GoogleMap
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.LevelWrapper
import cy.ac.ucy.cs.anyplace.lib.android.maps.Overlays
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import kotlinx.coroutines.flow.update

// TODO:PMX MERGE
class LevelPlanLoader {
  private val TG = "LevelPlanLoader"

  /**
   * Reads a floorplan image form the devices cache
   */
  fun readFromCache(VM: CvViewModel, FW: LevelWrapper) {
    val MT = ::readFromCache.name
    LOG.V(TG, "$MT: ${FW.prettyFloorName()}")

    val localResult =
            when (val bitmap = FW.loadFromCache()) {
              null -> {
                val msg ="Failed to load from local cache"
                LOG.W(TG, "$MT: msg")
                NetworkResult.Error(msg)
              }
              else -> {
                LOG.D2(TG, "$MT: success.")
                NetworkResult.Success(bitmap)
              }
            }
    VM.levelplanImg.update { localResult }
  }

  fun render(overlays: Overlays, gmap: GoogleMap, bitmap: Bitmap?, LW: LevelWrapper) {
    val method = ::render.name
    LOG.E(TG, method)
    LOG.E(TG, "$method: ${LW.wSpace.obj.name}: ${LW.wSpace.obj.buid}")
    overlays.drawFloorplan(bitmap, gmap, LW.bounds())
  }
}