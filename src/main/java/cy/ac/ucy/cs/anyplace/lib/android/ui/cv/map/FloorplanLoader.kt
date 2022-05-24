package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map

import android.graphics.Bitmap
import com.google.android.gms.maps.GoogleMap
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.FloorHelper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.maps.Overlays
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvMapViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult

class FloorplanLoader {
  /**
   * Reads a floorplan image form the devices cache
   */
  fun readFromCache(VM: CvMapViewModel, FH: FloorHelper) {
    LOG.V3(TAG_METHOD, FH.prettyFloorName())
    val localResult =
            when (val bitmap = FH.loadFromCache()) {
              null -> NetworkResult.Error("Failed to load from local cache")
              else -> NetworkResult.Success(bitmap)
            }
    VM.floorplanFlow.value = localResult
  }

  fun render(overlays: Overlays, gmap: GoogleMap, bitmap: Bitmap?, FH: FloorHelper) {
    LOG.V3()
    overlays.drawFloorplan(bitmap, gmap, FH.bounds())
  }
}