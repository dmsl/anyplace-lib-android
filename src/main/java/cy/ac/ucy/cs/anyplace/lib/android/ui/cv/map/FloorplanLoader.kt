package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map

import android.graphics.Bitmap
import com.google.android.gms.maps.GoogleMap
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.FloorWrapper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.maps.Overlays
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult

class FloorplanLoader {
  /**
   * Reads a floorplan image form the devices cache
   */
  fun readFromCache(VM: CvViewModel, FH: FloorWrapper) {
    LOG.W(TAG_METHOD, FH.prettyFloorName())
    val localResult =
            when (val bitmap = FH.loadFromCache()) {
              null -> {
                val msg ="Failed to load from local cache"
                LOG.W(TAG, "$METHOD: msg")
                NetworkResult.Error(msg)
              }
              else -> {
                LOG.D2(TAG, "$METHOD: Successfully read from cache.")
                NetworkResult.Success(bitmap)
              }
            }
    VM.floorplanFlow.value = localResult
  }

  fun render(overlays: Overlays, gmap: GoogleMap, bitmap: Bitmap?, FH: FloorWrapper) {
    LOG.V3()
    overlays.drawFloorplan(bitmap, gmap, FH.bounds())
  }
}