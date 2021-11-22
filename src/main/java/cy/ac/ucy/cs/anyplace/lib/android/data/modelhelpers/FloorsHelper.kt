package cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers

import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.models.Floor
import cy.ac.ucy.cs.anyplace.lib.models.Floors

/**
 * Extra functionality on top of the [Floors] data class.
 */
class FloorsHelper(val floors: Floors,
                   val spaceH: SpaceHelper) {

  fun hasFloors()  = floors.floors.isNotEmpty()
  fun getFirstFloor() = floors.floors[0]

  fun getFloor(num: Int) = getFloor(num.toString())
  fun getFloor(str: String) : Floor? {
    floors.floors.forEach { floor ->
      if (floor.floorNumber == str) return floor
    }
    LOG.E(TAG, "${spaceH.prettyFloor} not found: $str")
    return null
  }

  /** Deletes all cached floorplans */
  fun clearCacheFloorplans() = clearCache("floorplans") { clearCacheFloorplan() }

  /** Deletes all cached [CvMap]s */
  fun clearCacheCvMaps() = clearCache("CvMaps") { clearCacheCvMap() }

  /** Deletes all the cache related to a floor */
  fun clearCaches() = clearCache("all") { clearCache() }

  private fun clearCache(msg: String, method: FloorHelper.() -> Unit) {
    floors.floors.forEach { floor ->
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
    floors.floors.forEach { floor ->
      val FH = FloorHelper(floor, spaceH)
      if (!FH.hasFloorplanCached()) {
        val bitmap = FH.requestRemoteFloorplan()
        if (bitmap != null) {
          FH.cacheFloorplan(bitmap)
          LOG.D("Downloaded: ${FH.prettyFloorplanNumber()}.")
        }
      } else {
        alreadyCached+="${FH.floor.floorNumber}, "
      }
    }

    if (alreadyCached.isNotEmpty()) {
      LOG.D2(TAG_METHOD, "already cached ${spaceH.prettyFloorplans}: ${alreadyCached.dropLast(2)}")
    }
  }
}