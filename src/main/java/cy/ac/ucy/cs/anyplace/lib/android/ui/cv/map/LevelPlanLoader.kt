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

  val tag = "LevelPlanLoader"

  /**
   * Reads a floorplan image form the devices cache
   */
  fun readFromCache(VM: CvViewModel, FW: LevelWrapper) {
    val method = ::readFromCache.name
    LOG.E(tag, "$method: BUG")
    LOG.E(tag, "$method: BUG")
    LOG.E(tag, "$method: ${FW.prettyFloorName()}")

    LOG.E(tag, "$method: space: ${FW.wSpace.obj.name}")
    LOG.E(tag, "$method: space: ${FW.wSpace.obj.buid}")
    LOG.E(tag, "$method: floornum: ${FW.obj.number}")
    LOG.E(tag, "$method: level: buid: ${FW.obj.buid}")
    LOG.E(tag, "$method: level: name: ${FW.obj.name}")

    val localResult =
            when (val bitmap = FW.loadFromCache()) {
              null -> {
                val msg ="Failed to load from local cache"
                LOG.W(tag, "$method: msg")
                NetworkResult.Error(msg)
              }
              else -> {
                LOG.D2(tag, "$method: success.")
                NetworkResult.Success(bitmap)
              }
            }
    VM.levelplanImg.update { localResult }
  }

  fun render(overlays: Overlays, gmap: GoogleMap, bitmap: Bitmap?, LW: LevelWrapper) {
    val method = ::render.name
    LOG.E(tag, method)
    LOG.E(tag, "$method: ${LW.wSpace.obj.name}: ${LW.wSpace.obj.buid}")
    overlays.drawFloorplan(bitmap, gmap, LW.bounds())
  }
}