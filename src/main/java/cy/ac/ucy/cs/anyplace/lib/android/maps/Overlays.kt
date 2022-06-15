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

class Overlays(private val ctx: Context,
               private val scope: CoroutineScope) {

  /** Showing a heatmap */
  var heatmapOverlay: TileOverlay? = null
  var heatmapTileProvider: HeatmapTileProvider? = null
  /** Showing a floorplan */
  var floorplanOverlay: GroundOverlay? = null

  /**
   * Removes the previous floorplan before drawing a new one
   */
  fun drawFloorplan(bitmap: Bitmap?, map: GoogleMap, bounds: LatLngBounds) {
    if (bitmap != null) {
      if (floorplanOverlay != null) {
        uiRemoveFloorplanOverlay()
      }
      uiAddGroundOverlay(map, bitmap, bounds)
    }
    // floorplanOverlay = null
  }

  fun uiRemoveFloorplanOverlay() {
    scope.launch(Dispatchers.Main) {
      floorplanOverlay!!.remove()
    }
  }

  fun uiAddGroundOverlay(map: GoogleMap, bitmap: Bitmap, bounds: LatLngBounds) {
    val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
    scope.launch(Dispatchers.Main) {
      // make the caller method normal, and this on main thread....
      floorplanOverlay = map.addGroundOverlay(
              GroundOverlayOptions().apply {
                positionFromBounds(bounds)
                image(bitmapDescriptor)
              })
    }
  }

  fun refreshHeatmap(map: GoogleMap, locations: List<WeightedLatLng>) {
    LOG.E(TAG, "refreshHeatmap")
    if (heatmapTileProvider != null) {
      LOG.W(TAG, "$METHOD: updating")
      uiRefreshHeatmap(locations)
    } else {
      LOG.W(TAG, "$METHOD: heatmapTileProvider was null.") // CLR:PM
      createHeatmap(map, locations)
    }
  }



  private fun CvHeatmapGradient() : Gradient? {
    val useGradient = true
    return if (useGradient) {
      val lightPurple = Color.rgb(127, 114, 170)
      val yellow = Color.rgb(255, 203, 91)
      val white= Color.rgb(255, 255, 255)
      val orange = Color.rgb(255, 128, 0)
      val greenish = Color.rgb(7, 145, 84)

      // val colors = intArrayOf(lightPurple, greenish, yellow)
      // val colors = intArrayOf(white, greenish)

      val colors = intArrayOf(orange, greenish)
      val startPoints = floatArrayOf(0.5f, 1f)
      Gradient(colors, startPoints)
    } else null
  }

  fun createHeatmap(map: GoogleMap, locations: List<WeightedLatLng>) {
    val gradient = CvHeatmapGradient()
    if (heatmapOverlay != null) {
      LOG.D(TAG, "$METHOD: removing previous heatmap")
      uiRemoveHeatmap()
    }

    heatmapTileProvider = getTileProvider(gradient, locations)
    uiAddTileOverlay(map)
  }

  fun getTileProvider(gradient: Gradient?, locations: List<WeightedLatLng>):
          HeatmapTileProvider =
          if (gradient != null) {
            HeatmapTileProvider.Builder()
                    .weightedData(locations)
                    .opacity(0.9)
                    .gradient(gradient)
                    .build()
          } else {
            HeatmapTileProvider.Builder()
                    .weightedData(locations)
                    .build()
          }

  private fun bitmapFromVectorKt(vectorResID: Int) : BitmapDescriptor {
    val vectorDrawable=ContextCompat.getDrawable(ctx ,vectorResID)
    vectorDrawable!!.setBounds(0,0,vectorDrawable.intrinsicWidth,vectorDrawable.intrinsicHeight)
    val bitmap=Bitmap.createBitmap(vectorDrawable.intrinsicWidth,vectorDrawable.intrinsicHeight,Bitmap.Config.ARGB_8888)
    val canvas=Canvas(bitmap)
    vectorDrawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
  }

  fun uiAddTileOverlay(map: GoogleMap) {
    scope.launch(Dispatchers.Main) {
      val options = TileOverlayOptions().tileProvider(heatmapTileProvider!!)
      heatmapOverlay = map.addTileOverlay(options)
    }
  }

  fun uiRemoveHeatmap() {
    scope.launch(Dispatchers.Main) {
      heatmapOverlay?.remove()
    }
  }

  fun uiRefreshHeatmap(locations: List<WeightedLatLng>) {
    scope.launch(Dispatchers.Main) {
      heatmapTileProvider?.setWeightedData(locations)
      heatmapOverlay?.clearTileCache()
    }
  }

}