package cy.ac.ucy.cs.anyplace.lib.android.maps

import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.cache.anyplace.Cache
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.GmapWrapper
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilColor
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Connection
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.POI
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Space
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Polylines placed on the map
 * (and everything related to Connections and POIs..)
 *
 * POIs are not rendered at the moment
 * So do Polylines (Connections)
 *
 * Instead [PolylineOptions] are created which
 * can be passed to the MapMatching/SticktoGrid algo
 * (something like a binary search, for finding closest point in the grid)
 *
 */
class MapLines(private val app: AnyplaceApp,
                private val scope: CoroutineScope,
                private val VM: CvViewModel,
                private val map: GmapWrapper) {

  private val ctx = app.applicationContext

  private val cache by lazy { Cache(ctx) }
  private val utlColor by lazy { UtilColor(ctx) }

  /** Polylines visible on the map [NOT USED] */
  private val polylines : MutableList<Polyline> = mutableListOf()

  /** key: floor, val: list of Connections */
  private val floorConn : HashMap<Int, MutableList<Connection>> = HashMap ()
  /** key: floor, val: list of PolylineOptions */
  private val floorPolyopt : HashMap<Int, MutableList<PolylineOptions>> = HashMap ()
  /** key: floor, val: list of pois */
  private val floorPOIs: HashMap<Int, MutableList<POI>> = HashMap ()

  /** K: poi id val: poi coordinates */
  private val poiCoords: HashMap<String, LatLng> = HashMap ()

  fun hasConnectionsAndPoisCached(space: Space) : Boolean {
    return cache.hasSpaceConnections(space) && cache.hasSpacePOIs(space)
  }

  /**
   * Initialize maps of POIs and connections
   */
  suspend fun loadFromCache() {
    LOG.W(TAG, "$METHOD")
    if(app.space == null) {
      LOG.E(TAG, "$METHOD: empty space!")
      return
    }
    LOG.W(TAG, "$METHOD: space not null")

    val space = app.space!!
    if(!hasConnectionsAndPoisCached(space)) {
      LOG.E(TAG, "$METHOD: empty Connections or POIS!")
      return
    }

    LOG.W(TAG, "$METHOD: reading POIs..")

    // add in two maps
    val pois= cache.readSpacePOIs(space)!!
    pois.objs.forEach { poi->

      poiCoords[poi.puid]=LatLng(poi.coordinatesLat.toDouble(), poi.coordinatesLon.toDouble())

      val level = poi.floorNumber.toInt()
      if (floorPOIs[level]==null) floorPOIs[level]= mutableListOf()
      floorPOIs[level]?.add(poi)
    }

    LOG.W(TAG, "$METHOD: reading connections..")
    val connections = cache.readSpaceConnections(space)!!
    connections.objs.forEach { connection ->
      val level = connection.floorA.toInt()

      if (floorConn[level]==null) floorConn[level]= mutableListOf()
      floorConn[level]?.add(connection)

      val from = poiCoords[connection.poisA]
      val to = poiCoords[connection.poisB]

      val polyopt=PolylineOptions().add(from).add(to)
                      .color(utlColor.ColorPrimaryDark50())

      if (floorPolyopt[level]==null) floorPolyopt[level]= mutableListOf()
      floorPolyopt[level]?.add(polyopt)
    }
  }

  fun isInited() : Boolean {
    return floorConn.isNotEmpty() && floorPOIs.isNotEmpty()
  }

  suspend fun loadPolylines(floor: Int) {

    LOG.D2(TAG, "rendering polylines of floor $floor")

    if (!isInited())  {
      LOG.W(TAG, "$METHOD: initing POIs/connections")
      loadFromCache()
    }

    if (!DBG.uim) return

    val space = app.space!!
    if(!hasConnectionsAndPoisCached(space)) {
      LOG.E(TAG, "$METHOD: Must download POIs/Connections first. restart app..")
    }

    // renderPolylines(floor) BUGGY
  }

  /**
   * Not rendering the polylines...
   */
  private fun renderPolylines(floor: Int) {
    clearPolylines()

    val polyOpts = floorPolyopt[floor]
    if (polyOpts.isNullOrEmpty())  {
      LOG.W(TAG, "$METHOD: no connections in current floor.")
      return
    }

    VM.viewModelScope.launch(Dispatchers.Main) {
      polyOpts.forEach { polyOpt ->
        val polyline=map.obj.addPolyline(polyOpt)
        polylines.add(polyline)
      }
    }
  }

  fun clearPolylines() {
    LOG.W(TAG, "clearing polylines")
    VM.viewModelScope.launch(Dispatchers.Main) {
      polylines.forEach { it.remove() }
    }
    polylines.clear()
  }

  fun getPolyopts(floor: Int): MutableList<PolylineOptions>?{
    return floorPolyopt[floor]
  }
}