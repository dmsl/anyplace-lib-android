package cy.ac.ucy.cs.anyplace.lib.android.cache.legacy
// DEPRECATE

import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.Preferences
import org.json.JSONObject
import java.io.File

@kotlin.Deprecated("dont use")
class FileCache(private val prefs: Preferences) {
  companion object {
    const val JS_BUILDINGS_ALL = "buildings.json"
    const val JS_BUILDING_FLOORS = "floors.json"
    const val JS_BUILDING_POI = "poi.json"
  }

  fun initDirs() {
    File(prefs.cacheJsonDir).mkdirs()
  }

  fun deleteJsonCache() {
    val dir = prefs.cacheJsonDir
    LOG.W(TAG, "Deleting the whole json cache: $dir")
    File(dir).deleteRecursively()
  }

  fun radiomapsFolder() = File(prefs.radiomapsDir)
  fun floorplansFolder() = File(prefs.floorplansDir)
  fun jsonFolder() = File(prefs.cacheJsonDir)

  fun radiomapsFolder(buid: String, floorNum: String?) : File {
    // INFO:PM i think buid is NOT nullable (CLR:PM)
    val fl= floorNum ?: ""
    val path = "${prefs.radiomapsDir}/$buid/fl$fl"
    LOG.D2(TAG, "radiomapsFolder: $path")
    val file = File(path)
    file.mkdirs()
    return file
  }
  // public static File getRadioMapFolder(Context ctx, String buid, String floor) throws Exception { // CLR:PM
  // 	File root = getRadioMapsRootFolder(ctx);
  // 	File file = new File(root, (buid == null ? "-" : buid) + "fl_" + (floor == null ? "-" : floor));
  // 	file.mkdirs();
  //
  // 	// if (file.isDirectory() == false) {
  // 	// 	throw new Exception("Error: It seems we cannot write on the sdcard!");
  // 	// }
  // 	return file;
  // }

  // public static String getRadioMapFileName(String floor) { // CLR:PM
  //   return "fl_" + (floor == null ? "-" : floor) + "_indoor-radiomap.txt";
  // }
  fun radiomapFilename(floorNum: String?) : String {
    val fl= floorNum ?: ""
    return "${prefs.radiomapsDir}/fl$fl-indoor-radiomap.txt"
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

  // building/floor/pois (floor==null -> fetch all pois)
  fun hasBuildingsPOIs(buid: String, floorNum: String?) : Boolean {
    return if(floorNum!=null) {
      hasJsonBuildingFile("$buid/$floorNum", JS_BUILDING_POI)
    } else {
      hasJsonBuildingFile(buid, JS_BUILDING_POI)
    }
  }

  fun deleteBuildingPOIs(buid: String, floorNum: String?) : Boolean {
    return if(floorNum!=null) {
      deleteJsonBuildingFile("$buid/$floorNum", JS_BUILDING_POI)
    } else {
      deleteJsonBuildingFile(buid, JS_BUILDING_POI)
    }
  }

  fun storeBuildingPOIs(buid: String, floorNum: String?, jsString: String) : Boolean {
    return if(floorNum!=null) {
      storeJsonBuilding(jsString, "$buid/$floorNum", JS_BUILDING_POI)
    } else {
      storeJsonBuilding(jsString, buid, JS_BUILDING_POI)
    }
  }

  fun readJsonPOIs(buid: String, floorNum: String?) : JSONObject {
    return if(floorNum!=null) {
      readJsonBuilding("$buid/$floorNum", JS_BUILDING_POI)
    } else {
      readJsonBuilding(buid, JS_BUILDING_POI)
    }
  }

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