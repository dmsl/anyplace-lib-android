package cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers

import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
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

  fun clearCachedFloorplans() {
    floors.floors.forEach { floor ->
      val FH = FloorHelper(floor, spaceH)
      FH.clearCache()
      LOG.D("Deleted cache: ${FH.prettyFloorplanNumber()}.")
    }
  }

  /**
   * TODO: this must be called from a "Select Space" activity
   * (not on the logging/nav. in a earlier activity).
   */
  suspend fun fetchAllFloorplans() {
    floors.floors.forEach { floor ->
      val FH = FloorHelper(floor, spaceH)
      if (!FH.hasFloorplanCached()) {
        val bitmap = FH.requestRemoteFloorplan()
        if (bitmap != null) {
          FH.cacheFloorplan(bitmap)
          LOG.D("Downloaded: ${FH.prettyFloorplanNumber()}.")
        }
      } else {
        LOG.D2("Already in cache: ${FH.prettyFloorplanNumber()}.")
      }
    }
  }
}