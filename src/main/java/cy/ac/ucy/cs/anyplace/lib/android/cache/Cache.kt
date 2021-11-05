package cy.ac.ucy.cs.anyplace.lib.android.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.renderscript.ScriptGroup
import com.google.gson.Gson
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.models.Floor
import cy.ac.ucy.cs.anyplace.lib.models.LastValSpaces
import cy.ac.ucy.cs.anyplace.lib.models.Space
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter

/**
 * File cache using [ctx.cacheDir] as a base directory.
 *
 * Structure:
 * - TODO:PM
 */
class Cache(val ctx: Context) {

  companion object {
    // FILES
    val JS_SPACE = "s.json"
    val JS_SPACE_LASTVAL = "s.lastval.json" // Cached settings, like last floor
    val JS_FLOORS = "f.json"
    val PNG_FLOORPLAN = "f.png"
  }

  // SPACES
  fun dirSpace(space: Space) : String {  return "${ctx.cacheDir}/${space.id}"  }
  fun dirSpace(floor: Floor) : String {  return "${ctx.cacheDir}/${floor.buid}" }
  fun jsonSpace(space: Space) : String {  return "${dirSpace(space)}/$JS_SPACE" }
  //// Last Values cache
  fun jsonSpaceLastValues(space: Space) : String {  return "${dirSpace(space)}/$JS_SPACE_LASTVAL" }
  fun hasSpaceLastValues(space: Space): Boolean { return File(jsonSpaceLastValues(space)).exists() }
  fun deleteSpaceLastValues(space: Space) {  File(jsonSpaceLastValues(space)).delete()  }
  fun saveSpaceLastValues(space: Space, lastVal: LastValSpaces): Boolean {
    val filename = jsonSpaceLastValues(space)
    return try {
      File(dirSpace(space)).mkdirs()
      val fw= FileWriter(File(filename))
      Gson().toJson(lastVal, fw)
      fw.close()
      LOG.D2(TAG, "saveSpaceLastValues: $lastVal")
      LOG.D2(TAG, "saveSpaceLastValues: $filename")
      true
    } catch (e: Exception) {
      LOG.E(TAG, "saveSpaceLastValues: $filename: ${e.message}")
      false
    }
  }

  fun readSpaceLastValues(space: Space): LastValSpaces? {
    val filename = jsonSpaceLastValues(space)
    LOG.V4(TAG, "readSpaceLastValues: file: $filename")
    try {
      val json = File(filename).readText()
      val lv = Gson().fromJson(json, LastValSpaces::class.java)
      return lv
    } catch (e: Exception) {
     LOG.E(TAG, "readSpaceLastValues: $filename: ${e.message}")
    }

    return null
  }

  // FLOORS TODO:PM download and cache it here!
  fun jsonFloors(space: Space) : String {  return "${dirSpace(space)}/$JS_FLOORS"  }
  //// FLOOR
  fun dirFloor(floor: Floor) : String { return "${dirSpace(floor)}/${floor.floorNumber}" }
  fun floorplan(floor: Floor) : String { return "${dirFloor(floor)}/$PNG_FLOORPLAN" }

  fun hasFloorplan(floor: Floor): Boolean { return File(floorplan(floor)).exists() }
  fun clearFloorplan(floor: Floor) {  File(floorplan(floor)).delete()  }

  // VERIFY:PM
  // 1. update the json and restore it later
  // 2. cache the image and check that it is cached.
  // 3. if cache exists: load from cache.
  // 4. delete cache option.
  fun saveFloorplan(floor: Floor, bitmap: Bitmap?): Boolean {
    if (bitmap == null) return false

    return try {
      File(dirFloor(floor)).mkdirs()
      val file = File(floorplan(floor))
      val ostream = FileOutputStream(file)
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, ostream)
      ostream.flush()
      ostream.close()
      true
    } catch (e: Exception) {
      LOG.E(TAG, "Failed: ${floorplan(floor)}: ${e.message}")
      false
    }
  }

  fun loadFloorplan(floor: Floor): Bitmap? {
    return BitmapFactory.decodeFile(floorplan(floor))
  }

}