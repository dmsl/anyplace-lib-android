package cy.ac.ucy.cs.anyplace.lib.android.maps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.maps.android.heatmaps.Gradient
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Overlays on a map
 * - an overlay is an image that is placed on top of the map
 * - the levelplans (floorplans/deckplans) are an overlay that we are using..
 * - heatmaps is another:
 *   - there is some demo code for heatmaps..
 */
class MapOverlays(private val ctx: Context,
                  private val scope: CoroutineScope) {


}