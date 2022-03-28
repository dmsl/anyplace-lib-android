package cy.ac.ucy.cs.anyplace.lib.android.data.models.helpers

import com.google.gson.Gson
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.models.Floor
import cy.ac.ucy.cs.anyplace.lib.models.Floors
import java.lang.Exception

/**
 * Extra functionality on top of the [Floors] data class.
 */
class FloorsHelper(val unsortedObj: Floors, val spaceH: SpaceHelper) {

  /** Parses this sorted BUGFIX: wrapping on a new object */
  override fun toString(): String = Gson().toJson(Floors(obj), Floors::class.java)
  companion object {
    fun parse(str: String): Floors = Gson().fromJson(str, Floors::class.java)
  }

  private val obj: List<Floor> = (unsortedObj.floors.sortedBy { floor ->
    floor.floorNumber.toInt()
  })

  val size : Int get() = obj.size
  fun hasFloors()  = obj.isNotEmpty()
  fun getFirstFloor() = obj[0]
  fun getLastFloor() = obj[obj.size-1]

  fun getFloor(num: Int) = getFloor(num.toString())
  fun getFloor(str: String) : Floor? {
    obj.forEach { floor ->
      if (floor.floorNumber == str) return floor
    }
    LOG.E(TAG, "${spaceH.prettyFloor} not found: $str")
    return null
  }

  fun getFloorIdx(str: String) : Int {
    for (i in obj.indices) {
      if (obj[i].floorNumber == str) return i
    }
    return -10
  }

  /** Deletes all cached floorplans */
  fun clearCacheFloorplans() = clearCache("floorplans") { clearCacheFloorplan() }

  /** Deletes all cached [CvMap]s */
  fun clearCacheCvMaps() = clearCache("CvMaps") { clearCacheCvMaps() }

  /** Deletes all the cache related to a floor */
  fun clearCaches() = clearCache("all") { clearCache() }

  private fun clearCache(msg: String, method: FloorHelper.() -> Unit) {
    obj.forEach { floor ->
      val FH = FloorHelper(floor, spaceH)
      FH.method()
      LOG.D5(TAG, "clearCache:$msg: ${FH.prettyFloorplanNumber()}.")
    }
  }

  /**
   * TODO: this must be called from a "Select Space" activity
   * (not on the logging/nav. in a earlier activity).
   */
  suspend fun fetchAllFloorplans() {
    var alreadyCached=""
    obj.forEach { floor ->
      val FH = FloorHelper(floor, spaceH)
      if (!FH.hasFloorplanCached()) {
        val bitmap = FH.requestRemoteFloorplan()
        if (bitmap != null) {
          FH.cacheFloorplan(bitmap)
          LOG.D("Downloaded: ${FH.prettyFloorplanNumber()}.")
        }
      } else {
        alreadyCached+="${FH.obj.floorNumber}, "
      }
    }

    if (alreadyCached.isNotEmpty()) {
      LOG.D2(TAG_METHOD, "already cached ${spaceH.prettyFloorplans}: ${alreadyCached.dropLast(2)}")
    }
  }

  /**
   * Answers whether there is a floor higher than [floorNumStr]
   */
  fun canGoUp(floorNumStr: String): Boolean {
    try {
      if (floorNumStr.toInt() < getLastFloor().floorNumber.toInt()) return true
    } catch (e: Exception) { }
    return false
  }

  /**
   * Answers whether there is a floor lower than [floorNumStr]
   */
  fun canGoDown(floorNumStr: String): Boolean {
    try {
      if (floorNumStr.toInt() > getFirstFloor().floorNumber.toInt()) return true
    } catch (e: Exception) { }
    return false
  }

  fun getFloorAbove(curFloorStr: String): Floor? {
    val idx = getFloorIdx(curFloorStr) +1
    LOG.D5(TAG_METHOD, "IDX: $idx")
    return if (idx>=0 && idx<obj.size) obj[idx] else null
  }

  private fun printFloors() {
    for (i in obj.indices) {
      LOG.D(TAG_METHOD, "floor: $i, ${obj[i].floorNumber}")
    }
  }

  fun getFloorBelow(curFloorStr: String): Floor? {
    val idx = getFloorIdx(curFloorStr) + -1
    LOG.D5(TAG_METHOD, "IDX: $idx")
    return if (idx>=0 && idx<obj.size) obj[idx] else null
  }

}