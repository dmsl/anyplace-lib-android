package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.wrappers

import com.google.gson.Gson
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Level
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Levels
import kotlinx.coroutines.flow.update
import java.lang.Exception

/**
 * Extra functionality on top of the [Levels] data class.
 * [Levels] is at: clients/core/lib/src/main/java/cy/ac/ucy/cs/anyplace/lib/anyplace/models/Level.kt
 */
class LevelsWrapper(val unsortedObj: Levels, val spaceH: SpaceWrapper) {
  private val TG = "wr-levels"

  /** Parses this sorted BUGFIX: wrapping on a new object */
  override fun toString(): String = Gson().toJson(Levels(obj), Levels::class.java)
  companion object {
    fun parse(str: String): Levels = Gson().fromJson(str, Levels::class.java)
  }

  val obj: List<Level> = (unsortedObj.levels.sortedBy { floor ->
    floor.number.toInt()
  })

  val size : Int get() = obj.size
  fun hasLevels()  = obj.isNotEmpty()
  fun getFirstLevel() = obj[0]
  fun getLastLevel() = obj[obj.size-1]

  fun getLevel(num: Int) = getLevel(num.toString())
  fun getLevel(str: String) : Level? {
    val MT = "getLevel"
    obj.forEach { floor ->
      if (floor.number == str) return floor
    }
    LOG.E(TG, "$MT: ${spaceH.prettyLevel} not found: $str")
    return null
  }

  fun getLevelIdx(str: String) : Int {
    for (i in obj.indices) {
      if (obj[i].number == str) return i
    }
    return -10
  }

  /** Deletes all cached floorplans */
  fun clearCacheFloorplans() = clearCache("floorplans") { clearCacheLevelplan() }

  /** Deletes all the cache related to a floor */
  fun clearCaches() = clearCache("all") { clearCache() }

  private fun clearCache(msg: String, method: LevelWrapper.() -> Unit) {
    val MT = ::clearCache.name
    obj.forEach { floor ->
      val LW = LevelWrapper(floor, spaceH)
      LW.method()
      LOG.D5(TG, "$MT: $msg: ${LW.prettyLevelplanNumber()}.")
    }
  }


  /** Go one floor up */
  fun tryGoUp(VM: CvViewModel) {
    val MT = ::tryGoUp.name
    LOG.V3(TG, MT)

    try {
      val app = VM.app
      val floorNumStr = app.level.value?.number.toString()
      if (canGoUp(floorNumStr)) {
        val floorDest = getFloorAbove(floorNumStr)
        moveToFloorLvl(VM, floorDest!!)
        return
      }
    } catch (e: Exception) {
    }

    LOG.W(TG, "$MT: Cannot go further up.")
  }

  fun tryGoDown(VM: CvViewModel) {
    val MT = ::tryGoDown.name
    LOG.V3(TG, MT)
    try {
      val app = VM.app
      val floorNumStr = app.level.value?.number.toString()
      if (app.wLevels.canGoDown(floorNumStr)) {
        val floorDest = app.wLevels.getFloorBelow(floorNumStr)
        moveToFloorLvl(VM, floorDest!!)
        return
      }
    } catch(e: Exception) {
    }

    LOG.W(TG, "$MT: Cannot go further down.")
  }

  fun moveToFloorLvl(VM: CvViewModel, level: Level) {
    val MT = ::moveToFloorLvl.name
    LOG.D3(TG, "$MT: ${level.number} ${level.buid}")
    val app = VM.app
    app.level.update { level }
  }

  fun moveToFloor(VM: CvViewModel, floorNum: Int) {
    val MT = ::moveToFloor.name
    LOG.D2(TG, "$MT: to: $floorNum")
    val app = VM.app
    val floor = app.wLevels.getLevel(floorNum)!!
    moveToFloorLvl(VM, floor)
  }

  /**
   * Answers whether there is a floor higher than [floorNumStr]
   */
  fun canGoUp(floorNumStr: String): Boolean {
    try {
      if (floorNumStr.toInt() < getLastLevel().number.toInt()) return true
    } catch (e: Exception) { }
    return false
  }

  /**
   * Answers whether there is a floor lower than [floorNumStr]
   */
  fun canGoDown(floorNumStr: String): Boolean {
    try {
      if (floorNumStr.toInt() > getFirstLevel().number.toInt()) return true
    } catch (e: Exception) { }
    return false
  }

  fun getFloorAbove(curFloorStr: String): Level? {
    val MT = ::getFloorAbove.name
    val idx = getLevelIdx(curFloorStr) +1
    LOG.D5(TG, "$MT: idx: $idx")
    return if (idx>=0 && idx<obj.size) obj[idx] else null
  }
  fun getFloorBelow(curFloorStr: String): Level? {
    val MT = ::getFloorBelow.name
    val idx = getLevelIdx(curFloorStr) + -1
    LOG.D5(TG, "$MT: idx: $idx")
    return if (idx>=0 && idx<obj.size) obj[idx] else null
  }
}