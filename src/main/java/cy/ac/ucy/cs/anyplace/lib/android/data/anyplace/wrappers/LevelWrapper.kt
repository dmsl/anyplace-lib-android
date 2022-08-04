package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.wrappers

import android.graphics.Bitmap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Level
import com.google.gson.Gson
import cy.ac.ucy.cs.anyplace.lib.android.cache.anyplace.Cache
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.DetectionModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Levels

/**
 * Extra functionality on top of the [Level] data class.
 * [Levels] is at: clients/core/lib/src/main/java/cy/ac/ucy/cs/anyplace/lib/anyplace/models/Level.kt
 */
class LevelWrapper(val obj: Level,
                   val wSpace: SpaceWrapper) {
  val TG = "lvl-wrapper"

  override fun toString(): String = Gson().toJson(obj, Level::class.java)

  companion object {
    fun parse(str: String): Level = Gson().fromJson(str, Level::class.java)
  }

  private val cache by lazy { Cache(wSpace.ctx) }

  fun levelNumber() : Int = obj.number.toInt()
  fun prettyLevelplanNumber() = "${wSpace.prettyLevelplan}${obj.number}"
  fun prettyLevelNumber() = "${wSpace.prettyLevel}${obj.name}"
  fun prettyLevelName() = "${wSpace.prettyLevel} ${obj.name}"

  val prettyFloorCapitalize : String get() = prettyFloor.replaceFirstChar(Char::uppercase)
  val prettyFloor : String get() = wSpace.prettyLevel
  val prettyFloors : String get() = wSpace.prettyFloors
  val prettyFloorPlan : String get() = wSpace.prettyLevelplan
  val prettyFloorPlans : String get() = wSpace.prettyLevelplans

  fun northEast() : LatLng {
    val latNE = obj.topRightLat.toDouble()
    val lonNE = obj.topRightLng.toDouble()
    return LatLng(latNE, lonNE)
  }

  fun southWest() : LatLng {
    val latSW = obj.bottomLeftLat.toDouble()
    val lonSW = obj.bottomLeftLng.toDouble()
    return LatLng(latSW, lonSW)
  }

  fun bounds() : LatLngBounds {
    return LatLngBounds(southWest(), northEast())
  }

  fun hasLevelplanCached(): Boolean { return cache.hasFloorplan(obj) }

  fun loadLevelplanFromCache() : Bitmap? {
    val MT = ::loadLevelplanFromCache.name
    LOG.V2(TG, "$MT: ${obj.number} ${obj.name} ${obj.buid}")
    return cache.readLevelplan(obj)
  }

  fun clearCacheLevelplan() { cache.deleteFloorplan(obj) }
  // fun clearCacheCvMap() { cache.deleteFloorCvMap(floor) }
  /** Deletes the cvmap folder that might contain several CvMaps
   * created with different [DetectionModel]s */
  fun clearCache() {
    clearCacheLevelplan()
    // Other cache?
  }
  fun cacheLevelplan(bitmap: Bitmap?) { bitmap.let { cache.saveFloorplan(obj, bitmap) } }


}