package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers

import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Floor
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Floors
import kotlinx.coroutines.flow.update
import java.lang.Exception

/**
 * Extra functionality on top of the [Floors] data class.
 */
class FloorsWrapper(val unsortedObj: Floors, val spaceH: SpaceWrapper) {

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

  /** Deletes all the cache related to a floor */
  fun clearCaches() = clearCache("all") { clearCache() }

  private fun clearCache(msg: String, method: FloorWrapper.() -> Unit) {
    obj.forEach { floor ->
      val FW = FloorWrapper(floor, spaceH)
      FW.method()
      LOG.D5(TAG, "clearCache:$msg: ${FW.prettyFloorplanNumber()}.")
    }
  }

  /**
   * TODO: this must be called from a "Select Space" activity
   * (not on the logging/nav. in a earlier activity).
   */
  var showedMsgDownloading=false
  var showedMsgDone=true
  suspend fun fetchAllFloorplans(VM: CvViewModel) {
    var alreadyCached=""
    val app = VM.app
    obj.forEach { floor ->
      val FW = FloorWrapper(floor, spaceH)
      if (!FW.hasFloorplanCached()) {
        // at least one floor needs to be downloaded:
        // show notification now (and when done [showedMsgDone]
        if (!showedMsgDownloading) {
          app.snackbarInf(VM.viewModelScope, "Downloading all ${FW.prettyFloors} ..\n(keep app open)")
          showedMsgDownloading=true
          showedMsgDone=false // show another msg at the end
        }
        val bitmap = FW.requestRemoteFloorplan()
        if (bitmap != null) {
          FW.cacheFloorplan(bitmap)
          LOG.V2("Downloaded: ${FW.prettyFloorplanNumber()}.")
        }
      } else {
        alreadyCached+="${FW.obj.floorNumber}, "
      }
    }

    if (!showedMsgDone) {
      showedMsgDone=true
      app.snackbarShort(VM.viewModelScope, "All ${app.wFloors.size} ${app.wSpace.prettyFloors} downloaded!")
    }

    if (alreadyCached.isNotEmpty()) {
      LOG.V3(TAG_METHOD, "already cached ${spaceH.prettyFloorplans}: ${alreadyCached.dropLast(2)}")
    }
  }


  /** Go one floor up */
  fun tryGoUp(VM: CvViewModel) {
    LOG.V3()

    try {
      val app = VM.app
      val floorNumStr = app.floor.value?.floorNumber.toString()
      if (canGoUp(floorNumStr)) {
        val floorDest = getFloorAbove(floorNumStr)
        moveToFloor(VM, floorDest!!)
      }
    } catch (e: Exception) {
    }
    LOG.W(TAG_METHOD, "Cannot go further up.")
  }

  fun tryGoDown(VM: CvViewModel) {
    LOG.V3()
    try {
      val app = VM.app
      val floorNumStr = app.floor.value?.floorNumber.toString()
      if (app.wFloors.canGoDown(floorNumStr)) {
        val floorDest = app.wFloors.getFloorBelow(floorNumStr)
        moveToFloor(VM, floorDest!!)
      }
    } catch(e: Exception) {
    }
    LOG.W(TAG_METHOD, "Cannot go further down.")
  }

  fun moveToFloor(VM: CvViewModel, floor: Floor) {
    LOG.D2(TAG, "$METHOD: ${floor.floorNumber}")
    val app = VM.app
    // VM.floor.value = floor
    app.floor.update { floor }
  }

  fun moveToFloor(VM: CvViewModel, floorNum: Int) {
    LOG.D2(TAG, "$METHOD: to: $floorNum")
    val app = VM.app
    val floor = app.wFloors.getFloor(floorNum)!!
    moveToFloor(VM, floor)
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