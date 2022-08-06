package cy.ac.ucy.cs.anyplace.lib.android.maps

import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.cache.anyplace.Cache
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilColor
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Connection
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.POI
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Space
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Polylines on the map. there are the graph that connections different POIs between them
 * (and everything related to Connections and POIs..)
 *
 * - neither POIs or polylines are renderd on the map
 * - they are just fetched.
 * - Instead [PolylineOptions] are created which,
 *    can be passed to the MapMatching/SticktoGrid algo
 *   (something like a binary search, for finding closest point in the grid)
 *
 * - there is some implementation below that puts polylines on map, but it's buggy.
 *   - they do not get removed when the floor changes
 */
class MapLines(private val app: AnyplaceApp,
               private val scope: CoroutineScope,
               private val VM: CvViewModel,
               private val map: GmapWrapper) {
  private val TG = "ui-map-lines"
  private val notify = app.notify

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
    return !VM.downloadingPoisAndConnections && cache.hasSpaceConnections(space) && cache.hasSpacePOIs(space)
  }

  /**
   * Initialize maps of POIs and connections
   */
  fun loadFromCache() {
    val MT = ::loadFromCache.name
    LOG.W(TG, MT)

    scope.launch(Dispatchers.IO) {
      while(app.space==null || !hasConnectionsAndPoisCached(app.space!!)) {
        LOG.D(TG, "$MT: waiting for space+pois")
        delay(100)
      }

      val space = app.space!!
      // add in two map data structures
      val pois=cache.readSpacePOIs(space)
      if (pois == null) {
        notify.DEV(scope, "Empty POIs ($TG/$MT)")
        return@launch
      }

      pois.objs.forEach { poi->

        poiCoords[poi.puid]=LatLng(poi.coordinatesLat.toDouble(), poi.coordinatesLon.toDouble())
        val level = poi.levelNumber.toInt()
        if (floorPOIs[level]==null) floorPOIs[level]= mutableListOf()
        floorPOIs[level]?.add(poi)
      }

      LOG.D2(TG, "$MT: reading connections..")
      val connections = cache.readSpaceConnections(space)
      if (connections == null) {
        notify.DEV(scope, "Empty space connections ($TG/$MT)")
        return@launch
      }

      connections.objs.forEach { connection ->
        val level = connection.floorA.toInt()

        if (floorConn[level]==null) floorConn[level]= mutableListOf()
        floorConn[level]?.add(connection)

        val from = poiCoords[connection.poisA]
        val to = poiCoords[connection.poisB]

        val polyopt=PolylineOptions().add(from).add(to).color(utlColor.PrimaryDark50())

        if (floorPolyopt[level]==null) floorPolyopt[level]= mutableListOf()
        floorPolyopt[level]?.add(polyopt)
      }
    }
  }

  fun isInited() : Boolean {
    return floorConn.isNotEmpty() && floorPOIs.isNotEmpty()
  }

  fun loadPolylines(floor: Int) {
    val MT = ::loadPolylines.name
    LOG.D2(TG, "$MT: rendering polylines of floor $floor")

    if (!isInited())  {
      LOG.D(TG, "$MT: initing POIs/connections")
      loadFromCache()
    }
    val space = app.space!!
    if(!hasConnectionsAndPoisCached(space)) {
      LOG.E(TG, "$MT: Must download POIs/Connections first. restart app..")
    }

    // renderPolylines(floor) // BUGGY
  }

  /**
   * Not rendering the polylines...
   */
  private fun renderPolylines(floor: Int) {
    val MT = ::renderPolylines.name
    clearPolylines()

    val polyOpts = floorPolyopt[floor]
    if (polyOpts.isNullOrEmpty())  {
      LOG.W(TG, "$MT: no connections in current floor.")
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
    LOG.W(TG, "clearing polylines")
    VM.viewModelScope.launch(Dispatchers.Main) {
      polylines.forEach { it.remove() }
    }
    polylines.clear()
  }

  fun getPolyopts(floor: Int): MutableList<PolylineOptions>?{
    return floorPolyopt[floor]
  }
}