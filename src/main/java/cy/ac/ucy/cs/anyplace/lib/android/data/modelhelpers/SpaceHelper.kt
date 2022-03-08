package cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.cache.Cache
import cy.ac.ucy.cs.anyplace.lib.android.data.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.resizeTo
import cy.ac.ucy.cs.anyplace.lib.android.extensions.setColor
import cy.ac.ucy.cs.anyplace.lib.models.LastValSpaces
import cy.ac.ucy.cs.anyplace.lib.models.Space

/**
 * Extra functionality on top of the [Space] data class.
 */
class SpaceHelper(val ctx: Context,
                  val repo: RepoAP,
                  val space: Space) {

  override fun toString(): String = Gson().toJson(space, Space::class.java)

  companion object {
    const val TP_BUILDING = "building"
    const val TP_VESSEL = "vessel"

    fun parse(str: String): Space  = Gson().fromJson(str, Space::class.java)
  }

  private val cache by lazy { Cache(ctx) }

  val prettyType: String
    get() {
      return space.type
    }

  val prettyTypeCapitalize: String
    get() {
      return prettyType.replaceFirstChar(Char::uppercase)
    }

  val prettyFloor : String
    get() {
      return when (space.type) {
        TP_BUILDING -> "floor"
        TP_VESSEL -> "deck"
        else -> "floor"
      }
    }


  /** Returns an icon according to the [Space] typ */
  fun getIcon(ctx: Context): Drawable? = getIcon(ctx, null, null)
  fun getIcon(ctx: Context, @ColorRes colorRes: Int): Drawable? = getIcon(ctx, colorRes, null)

  /** Returns an icon resized according to the [Space] typ */
  fun getIcon(ctx: Context, @ColorRes colorRes: Int?, size: Int?): Drawable? {
    val icon = when (space.type) {
      TP_VESSEL -> ContextCompat.getDrawable(ctx, R.drawable.ic_vessel)
      else -> ContextCompat.getDrawable(ctx, R.drawable.ic_building)  // TP_BUILDING case
    } ?: return null

    if (colorRes != null) { icon.setColor(ctx, colorRes) }

    return if (size != null) icon.resizeTo(ctx, size) else icon
  }

  /** Capitalize first letter */
  val prettyFloorCapitalize : String
    get() {
      return prettyFloor.replaceFirstChar(Char::uppercase)
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
      LOG.D2(TAG_METHOD, "${prettyFloor}: ${lastVal.lastFloor}")
      lastVal
    } else {
      LastValSpaces()
    }
  }
}