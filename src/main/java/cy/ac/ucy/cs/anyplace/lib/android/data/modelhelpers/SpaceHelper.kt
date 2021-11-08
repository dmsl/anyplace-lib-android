package cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.maps.model.LatLng
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.cache.Cache
import cy.ac.ucy.cs.anyplace.lib.android.data.Repository
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.models.LastValSpaces
import cy.ac.ucy.cs.anyplace.lib.models.Space

/**
 * Extra functionality on top of the [Space] data class.
 */
class SpaceHelper(val ctx: Context,
                  val repo: Repository,
                  val space: Space) {

  companion object {
    const val TP_BUILDING = "building"
    const val TP_VESSEL = "vessel"
  }

  private val cache by lazy { Cache(ctx) }

  val prettyType: String
    get() {
      // TODO check if valid space type?
      return space.type.replaceFirstChar(Char::uppercase)
    }

  val prettyFloor : String
    get() {
      return when (space.type) {
        TP_BUILDING -> "floor"
        TP_VESSEL -> "deck"
        else -> "floor"
      }
    }

  val prettyFloors : String get() = "${prettyFloor}s"
  val prettyFloorplan : String get() = "${prettyFloor}plan"
  val prettyFloorplans : String get() = "${prettyFloorplan}s"

  fun isBuilding() : Boolean = space.type == TP_BUILDING
  fun isVessel() : Boolean = space.type == TP_VESSEL

  fun latLng() : LatLng {
    val lat = space.coordinatesLat.toDouble()
    val lon = space.coordinatesLon.toDouble()
    return LatLng(lat, lon)
  }

  fun cacheLastValues(lastValSpaces: LastValSpaces) {
    cache.saveSpaceLastValues(space, lastValSpaces)
  }

  fun hasLastValuesCached() = cache.hasSpaceLastValues(space)
  fun loadLastValues() : LastValSpaces {
    val lastVal = cache.readSpaceLastValues(space)
    return if (lastVal != null) {
      LOG.D2("loadLastValues: ${prettyFloor}: ${lastVal.lastFloor}")
      lastVal
    } else {
      LastValSpaces()
    }
  }

}