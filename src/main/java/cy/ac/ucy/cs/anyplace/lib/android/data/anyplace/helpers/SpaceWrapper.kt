package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.cache.anyplace.Cache
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.ConnectionsResp
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.LastValSpaces
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.POIsResp
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Space

/**
 * Extra functionality on top of the [Space] data class.
 */
class SpaceWrapper(val ctx: Context,
                   val repo: RepoAP,
                   val obj: Space) {

  override fun toString(): String = Gson().toJson(obj, Space::class.java)

  companion object {
    const val TP_BUILDING = "building"
    const val TP_VESSEL = "vessel"

    const val BUID_UCY_CS_BUILDING = "username_1373876832005"
    const val BUID_UCY_FST02 = "building_3ae47293-69d1-45ec-96a3-f59f95a70705_1423000957534"

    const val BUID_STENA_FLAVIA= "vessel_2a2cf77c-91e0-41e2-971b-e80f5570d616_1635154314048"

    const val BUID_HARDCODED = BUID_STENA_FLAVIA

    fun parse(str: String): Space  = Gson().fromJson(str, Space::class.java)
  }

  private val cache by lazy { Cache(ctx) }

  val prettyType: String
    get() = obj.type

  val prettyTypeCapitalize: String
    get() = prettyType.replaceFirstChar(Char::uppercase)

  val prettyTypeAllCaps: String
    get() = prettyType.uppercase()

  val prettyLevel : String
    get() {
      return when (obj.type) {
        TP_BUILDING -> "floor"
        TP_VESSEL -> "deck"
        else -> "level"
      }
    }

  val prettyFloorAllCaps: String
    get() = prettyLevel.uppercase()

  /** Returns an icon according to the [Space] typ */
  fun getIcon(ctx: Context): Drawable? = getIcon(ctx, null, null)
  fun getIcon(ctx: Context, @ColorRes colorRes: Int): Drawable? = getIcon(ctx, colorRes, null)

  /** Returns an icon resized according to the [Space] typ */
  fun getIcon(ctx: Context, @ColorRes colorRes: Int?, size: Int?): Drawable? {
    val icon = when (obj.type) {
      TP_VESSEL -> ContextCompat.getDrawable(ctx, R.drawable.ic_vessel)
      else -> ContextCompat.getDrawable(ctx, R.drawable.ic_building)  // TP_BUILDING case
    } ?: return null

    if (colorRes != null) { icon.setColor(ctx, colorRes) }

    return if (size != null) icon.resizeTo(ctx, size) else icon
  }

  /** Capitalize first letter */
  val prettyFloorCapitalize : String
    get() {
      return prettyLevel.replaceFirstChar(Char::uppercase)
    }

  val prettyFloors : String get() = "${prettyLevel}s"
  val prettyLevelplan : String get() = "${prettyLevel}plan"
  val prettyLevelplans : String get() = "${prettyLevelplan}s"

  fun isBuilding() : Boolean = obj.type == TP_BUILDING
  fun isVessel() : Boolean = obj.type == TP_VESSEL

  fun latLng() : LatLng {
    val lat = obj.coordinatesLat.toDouble()
    val lon = obj.coordinatesLon.toDouble()
    return LatLng(lat, lon)
  }

  fun cacheLastValues(lastValSpaces: LastValSpaces) {
    LOG.E(TAG, "$METHOD: ${lastValSpaces.lastFloor}")
    cache.saveSpaceLastValues(obj, lastValSpaces)
  }

  fun hasLastValuesCached() = cache.hasSpaceLastValues(obj)
  fun loadLastValues() : LastValSpaces {
    val lastVal = cache.readSpaceLastValues(obj)
    return if (lastVal != null) {
      LOG.D2(TAG_METHOD, "${prettyLevel}: ${lastVal.lastFloor}")
      lastVal
    } else {
      LastValSpaces()
    }
  }

  fun cacheConnections(connections: ConnectionsResp) {
    cache.saveSpaceConnections(obj, connections)
  }

  fun cachePois(pois: POIsResp) {
    cache.saveSpacePois(obj, pois)
  }
}