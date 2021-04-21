package cy.ac.ucy.cs.anyplace.lib.android.cache

import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.Preferences
import org.json.JSONObject
import java.io.File

class FileCache(private val prefs: Preferences) {
  companion object {
    val TAG = "ap_fileCache"
    const val JS_BUILDINGS_ALL = "buildings.all.json"
    const val JS_BUILDING_FLOORS = "building.floors.json"
  }

  fun initDirs() {
    File(prefs.cacheJsonDir).mkdirs()
  }

  fun deleteJsonCache() {
    val dir = prefs.cacheJsonDir
    LOG.W(TAG, "Deleting the whole json cache: $dir")
    File(dir).deleteRecursively()
  }

  private fun getJsonFilename(name: String) = "${prefs.cacheJsonDir}/$name"

  private fun hasJsonFile(name: String) = File(getJsonFilename(name)).exists()
  private fun hasJsonBuildingFile(buid: String, name: String) = File(getJsonFilename("$buid/$name")).exists()

  private fun deleteJsonFile(name: String): Boolean {
    val filename = getJsonFilename(name)
    LOG.D2(TAG, "deleting json: $filename")
    if (File(filename).exists()) return File(filename).delete()
    return true
  }

  private fun deleteJsonBuildingFile(buid: String, name: String)
          = deleteJsonFile("$buid/$name")

  private fun readJsonStr(name: String): String {
    val filename = getJsonFilename(name)
    LOG.D3(TAG, "reading file-cache: $filename")
    return File(filename).readText()
  }
  private fun readJson(name: String) = JSONObject(readJsonStr(name))
  private fun readJsonBuilding(buid: String, name: String) = readJson("$buid/$name")

  // all buildings
  fun hasBuildingsAll() = hasJsonFile(JS_BUILDINGS_ALL)
  fun deleteBuildingsAll() = deleteJsonFile(JS_BUILDINGS_ALL)
  fun storeBuildingsAll(jsString: String) = storeJson(jsString, JS_BUILDINGS_ALL)
  fun readBuildingsAll(): JSONObject = readJson((JS_BUILDINGS_ALL))

  // building/floors
  fun hasBuildingsFloors(buid: String) = hasJsonBuildingFile(buid, JS_BUILDING_FLOORS)
  fun deleteBuildingFloors(buid: String) = deleteJsonBuildingFile(buid, JS_BUILDING_FLOORS)
  fun storeBuildingFloors(buid: String, jsString: String) = storeJsonBuilding(jsString, buid, JS_BUILDING_FLOORS)
  fun readBuildingFloors(buid: String): JSONObject = readJsonBuilding(buid, JS_BUILDING_FLOORS)

  fun storeJsonBuilding(jsString: String, buid: String, name: String): Boolean {
    File(getJsonFilename(buid)).mkdirs() // create subfolder
    return storeJson(jsString, "$buid/$name")
  }

  fun storeJson(jsString: String, name: String): Boolean {
    return try {
      File(getJsonFilename(name)).printWriter().use { out ->
        out.println(jsString)
        true
      }
    } catch (e: Exception) {
      LOG.E(TAG, "storeJson", e)
      false
    }
  }
}