package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map

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
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.maps.MapOverlays
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Manages the [MapOverlays] of a level (floor or deck).
 * Overlays are: whatever images are drawn on the map
 * - levelplan: floorplan or deckplan
 * - heatmaps
 *
 * NOTE: [MapMarkers] and [MapLines] are not images
 *
 * // TODO make this overlays..
 */
open class LevelOverlaysWrapper(
        protected val VM: CvViewModel,
        protected val scope: CoroutineScope,
        protected val ctx: Context,
        private val ui: CvUI,
        /** [GoogleMap] overlays */
        // protected val overlays: MapOverlays
) {
  val TG = "ui-map-overlays"

  // val overlays by lazy { MapOverlays(ctx, scope) }

  /** Showing a heatmap */
  var heatmapOverlay: TileOverlay? = null
  var heatmapTileProvider: HeatmapTileProvider? = null
  /** Showing a floorplan */
  var floorplanOverlay: GroundOverlay? = null

  // private val TG = "wr-lvl-overlays"
  private val app = VM.app
  private val notify = app.notify


  var collectingLevelplanChanges = false
  /**
   * Observes when a floorplan changes ([VMB.floorplanFlow]) and loads
   * the new image, and the relevant heatmap
   *
   * This has to be separate.
   */
  fun observeLevelplanImage(gmap: GoogleMap) {
    val MT = ::observeLevelplanImage.name

    if (collectingLevelplanChanges) return
    collectingLevelplanChanges=true
    LOG.V2(TG, "$MT: setup")

    scope.launch(Dispatchers.IO) {
      VM.nwLevelPlan.bitmap.collect { response ->

        LOG.V4(TG, "$MT: levelplan updated..")
        when (response) {
          is NetworkResult.Loading -> {
            LOG.V5(TG, "$MT: will load ${app.wSpace.prettyLevelplan}..")
          }
          is NetworkResult.Error -> {
            val msg = ": Failed to fetch ${app.wSpace.prettyType}: ${app.space?.name}: [${response.message}]"
            LOG.E(TG, "Error: $MT: $msg")
            notify.short(scope, msg)
          }
          is NetworkResult.Success -> {
            if (app.wLevel == null) {
              val msg = "No floor/deck selected."
              LOG.D2(msg)
              notify.short(scope, msg)
            } else {
              LOG.D2(TG, "$MT: success: rendering img")
              VM.nwLevelPlan.render(response.data, app.wLevel!!) // TODO: move this method in this file?
              loadHeatmap(gmap)
            }
          }
          // ignore Unset
          else -> {}
        }
      }
    }
  }

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
  }

  fun uiRemoveFloorplanOverlay() {
    scope.launch(Dispatchers.Main) { floorplanOverlay!!.remove() }
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

  // /**
  //  * TODO:PMX: refresh it in here..
  //  */
  // fun refreshHeatmap(map: GoogleMap, points: List<WeightedLatLng>) {
  //   val MT = ::refreshHeatmap.name
  //   LOG.E(TG,  MT)
  //   scope.launch(Dispatchers.IO) {
  //     // if (!app.isLogger()) return TODO:PMX
  //     if (heatmapTileProvider != null) {
  //       LOG.W(TG,  "$MT: updating")
  //       uiRefreshHeatmap(points)
  //     } else {
  //       LOG.D2(TG,  "$MT: heatmapTileProvider was null.")
  //       createHeatmap(map, points)
  //     }
  //   }
  // }

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

  private fun createHeatmap(map: GoogleMap, points: List<WeightedLatLng>) {
    val MT = ::createHeatmap.name
    if (points.isEmpty()) {
      LOG.W(TG, "$MT: empty input points")
      return
    }
    val gradient = CvHeatmapGradient()
    if (heatmapOverlay != null) {
      LOG.D(TG,  "$MT: removing previous heatmap")
      removeHeatmap()
    }

    heatmapTileProvider = getTileProvider(gradient, points)
    uiAddTileOverlay(map)
  }

  private val HEATMAP_OPACITY =0.7
  fun getTileProvider(gradient: Gradient?, locations: List<WeightedLatLng>): HeatmapTileProvider {
    return if (gradient != null) {
      HeatmapTileProvider.Builder()
              .weightedData(locations)
              .opacity(HEATMAP_OPACITY)
              .gradient(gradient)
              .build()
    } else {
      HeatmapTileProvider.Builder()
              .weightedData(locations)
              .build()
    }
  }

  /**
   * This was for drawing a vector instead of an image on the maps. not used..
   */
  private fun bitmapFromVectorKt(vectorResID: Int) : BitmapDescriptor {
    val vectorDrawable= ContextCompat.getDrawable(ctx ,vectorResID)
    vectorDrawable!!.setBounds(0,0,vectorDrawable.intrinsicWidth,vectorDrawable.intrinsicHeight)
    val bitmap= Bitmap.createBitmap(vectorDrawable.intrinsicWidth,vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas= Canvas(bitmap)
    vectorDrawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
  }

  fun uiAddTileOverlay(map: GoogleMap) {
    scope.launch(Dispatchers.Main) {
      val options = TileOverlayOptions().tileProvider(heatmapTileProvider!!)
      heatmapOverlay = map.addTileOverlay(options)
    }
  }

  fun removeHeatmap() {
    scope.launch(Dispatchers.Main) {
      heatmapOverlay?.remove()
    }
  }

  // fun uiRefreshHeatmap(points: List<WeightedLatLng>) {
  //   val MT = ::uiRefreshHeatmap.name
  //   if (points.isEmpty()) {
  //     LOG.W(TG, "$MT: empty input points")
  //     return
  //   }
  //   scope.launch(Dispatchers.Main) {
  //     heatmapTileProvider?.setWeightedData(points)
  //     heatmapOverlay?.clearTileCache()
  //   }
  // }


  /**
   * Loads
   */
  suspend fun loadHeatmap(gmap: GoogleMap) {
    val MT = ::loadHeatmap.name
    LOG.E(TG, MT)

    VM.waitForDetector()  // workaround
    if (app.wLevel==null)  {
      LOG.E(TG, "$MT: level was null (skipping heatmap)")
      return
    }
    removeHeatmap()
    // all is good, render.
    renderHeatmap(gmap)
  }


  /**
   * Gets the weighted locations and renders the heatmap
   */
  suspend fun renderHeatmap(map: GoogleMap) {
    val MT = ::renderHeatmap.name
    // if (!app.isLogger()) return TODO:PMX

    val hasAnyCvFingerprints = app.repoSmas.local.hasCvFingerprints()
    if (app.wLevel != null && hasAnyCvFingerprints) {
      val points = getWeightedLocationList()
      LOG.E(TG, "$MT: points: ${points.size}")
      createHeatmap(map, points)

      // val MT = ::refreshHeatmap.name
      // LOG.E(TG,  MT)
      // scope.launch(Dispatchers.IO) {
      // if (heatmapTileProvider != null) {
      //   LOG.W(TG,  "$MT: updating")
      //   uiRefreshHeatmap(points)
      // } else {
      //   LOG.D2(TG,  "$MT: creating..")
      //   createHeatmap(map, points)
      // }
    }
    // }
  }

  /**
   * Doing a query to fetch the weights necessary for the heatmap
   * - it's a weighted heatmap
   * - essentially it's the count of objects (weight) per location (latLng)
   * - it is done per floor, per model
   */
  suspend fun getWeightedLocationList() : List<WeightedLatLng> {
    val MT = ::getWeightedLocationList.name
    LOG.E(TG, MT)
    val locations : MutableList<WeightedLatLng> = mutableListOf()
    val level = app.wLevel!!.levelNumber()
    val points = app.repoSmas.local.getCvFingerprintsHeatmapWeights(VM.model.idSmas, level)
    LOG.E(TG, "$MT: points in ${points.size} locations")
    points.forEach {
      try {
        locations.add(WeightedLatLng(LatLng(it.x, it.y), it.weight.toDouble()))
      } catch (e: Exception) {}
    }
    return locations
  }
}

/** Data class for returning the weights */
data class HeatmapWeights(val weight:
                          Int, val x: Double,
                          val y: Double)