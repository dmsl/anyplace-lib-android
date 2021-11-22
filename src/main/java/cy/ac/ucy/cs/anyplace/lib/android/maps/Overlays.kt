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
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import java.io.IOException
import java.io.InputStream

class Overlays(private val ctx: Context) {

  /** Showing a heatmap */
  var heatmapOverlay: TileOverlay? = null
  var heatmapTileProvider: HeatmapTileProvider? = null
  /** Showing a floorplan */
  var floorplanOverlay: GroundOverlay? = null

  fun getImgTestAsset(): InputStream? {
    val floorplanFilename = "t1.png"
    try {
      with(ctx.assets.open(floorplanFilename)){  return this  }
    } catch (e: IOException) {
      // log error
    }
    return null
  }

  fun addFloorplan(bitmap: Bitmap?, map: GoogleMap, bounds: LatLngBounds) {
    if (floorplanOverlay != null) {
      LOG.E(TAG, "addFloorplanOverlay: TODO: remove previous floorplan")
    }
    if (bitmap != null) {
      val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
      floorplanOverlay = map.addGroundOverlay(
        GroundOverlayOptions().apply {
          positionFromBounds(bounds)
          image (bitmapDescriptor)
        }
      )
    }
    floorplanOverlay = null
  }

  fun refreshHeatmap(locations: List<WeightedLatLng>) {
    LOG.E(TAG, "refreshHeatmap")
    if (heatmapTileProvider != null) {
      LOG.E(TAG, "refreshHeatmap: updating")
      heatmapTileProvider?.setWeightedData(locations)
      heatmapOverlay?.clearTileCache()
    }
  }

  // fun updateHeatmap(gmap: GoogleMap, locations: List<WeightedLatLng>, gradient: Gradient?) {
  //   if (heatmapTileProvider == null) {
  //     addHeatmap(gmap, locations, gradient)
  //   } else {
  //     heatmapTileProvider?.setWeightedData(locations)
  //   }
  // }

  fun removeHeatmap() = heatmapOverlay?.remove()

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

  fun addHeatmap(map: GoogleMap, locations: List<WeightedLatLng>) {
    val gradient = CvHeatmapGradient()
    if (heatmapOverlay != null) {
      LOG.D(TAG, "addHeatmap: removing previous heatmap")
      heatmapOverlay?.remove()
    }
    heatmapTileProvider = if (gradient != null) {
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

    heatmapOverlay =
      map.addTileOverlay(TileOverlayOptions().tileProvider(heatmapTileProvider!!))
  }


  private fun bitmapFromVectorKt(vectorResID: Int) : BitmapDescriptor {
    val vectorDrawable=ContextCompat.getDrawable(ctx ,vectorResID)
    vectorDrawable!!.setBounds(0,0,vectorDrawable.intrinsicWidth,vectorDrawable.intrinsicHeight)
    val bitmap=Bitmap.createBitmap(vectorDrawable.intrinsicWidth,vectorDrawable.intrinsicHeight,Bitmap.Config.ARGB_8888)
    val canvas=Canvas(bitmap)
    vectorDrawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
  }
}