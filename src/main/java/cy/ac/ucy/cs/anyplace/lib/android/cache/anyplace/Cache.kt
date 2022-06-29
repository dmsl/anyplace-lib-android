package cy.ac.ucy.cs.anyplace.lib.android.cache.anyplace

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.DetectionModel
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.*
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths

/**
 * File cache using [ctx.filesDir]/spaces as a base directory.
 *
 * A local CvMap is an uncommitted CvMap
 *
 * Structure:
 * - TODO:PM
 */
open class Cache(val ctx: Context) {

  companion object {
    // FILES
    //// Space
    const val JS_SPACE = "s.json"
    const val JS_SPACE_LASTVAL = "s.lastval.json" // Cached settings, like last floor
    const val JS_FLOORS = "f.json"
    //// Space/Floor
    const val PNG_FLOORPLAN = "f.png"
  }

  private val gson : Gson by lazy { GsonBuilder().create() }

  val baseDir get() = "${ctx.filesDir}"

  val spacesDir get() = "$baseDir/spaces"
  val fingerprintsFilename get() = "$baseDir/cv-fingerprints.jsonl"

  // SPACES

  fun dirSpace(space: Space) : String {  return "$spacesDir/${space.id}"  }
  fun dirSpace(floor: Floor) : String {  return "$spacesDir/${floor.buid}" }
  fun dirSpace(cvMap: CvMap) : String {  return "$spacesDir/${cvMap.buid}" }
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
      LOG.V3(TAG, "$METHOD: $lastVal")
      LOG.V3(TAG, "$METHOD: $filename")
      true
    } catch (e: Exception) {
      LOG.E(TAG, "$METHOD: $filename: ${e.message}")
      false
    }
  }
  fun readSpaceLastValues(space: Space): LastValSpaces? {
    val filename = jsonSpaceLastValues(space)
    LOG.V4(TAG, "readSpaceLastValues: file: $filename")
    try {
      val json = File(filename).readText()
      return Gson().fromJson(json, LastValSpaces::class.java)
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
  fun dirFloor(cvMap: CvMap) : String { return "${dirSpace(cvMap)}/${cvMap.floorNumber}" }

  fun hasFloorplan(floor: Floor): Boolean { return File(floorplan(floor)).exists() }
  fun deleteFloorplan(floor: Floor) {  File(floorplan(floor)).delete()  }
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

  fun readFloorplan(floor: Floor): Bitmap? {
    return BitmapFactory.decodeFile(floorplan(floor))
  }

  val foldernameCvMap get() = "cvmap.local"

  //// CV Detection Maps
  ////// Filenames for CvModels: its the model-name.json
  fun filenameCvMapModel(model: String) : String {  return "${model}.json" }
  fun filenameCvMapModel(model: DetectionModel) : String {  return "${model.modelName}.json" }
  fun filenameCvMapModel(cvMap: CvMap) : String {  return "${cvMap.detectionModel}.json" }

  /** Directory (per [Floor] of [CvMap]s. */
  fun dirFloorCvMapsLocal(floor: Floor) : String {  return "${dirFloor(floor)}/$foldernameCvMap" }
  fun dirFloorCvMapsLocal(cvMap: CvMap) : String {  return "${dirFloor(cvMap)}/$foldernameCvMap" }
  fun hasDirFloorCvMapsLocal(floor: Floor): Boolean { return File(dirFloorCvMapsLocal(floor)).exists() }
  fun hasDirFloorCvMapsLocal(cvMap: CvMap): Boolean { return File(dirFloorCvMapsLocal(cvMap)).exists() }

  fun deleteFloorCvMapsLocal(cvMap: CvMap) {  File(dirFloorCvMapsLocal(cvMap)).deleteRecursively() }
  fun deleteFloorCvMapsLocal(floor: Floor) {  File(dirFloorCvMapsLocal(floor)).deleteRecursively() }

  fun jsonFloorCvMapModelLocal(f: Floor, m: String) : String {  return "${dirFloorCvMapsLocal(f)}/${filenameCvMapModel(m)}" }
  fun jsonFloorCvMapModelLocal(f: Floor, m: DetectionModel) : String {  return "${dirFloorCvMapsLocal(f)}/${filenameCvMapModel(m)}" }
  fun jsonFloorCvMapModelLocal(cvm: CvMap) : String {  return "${dirFloorCvMapsLocal(cvm)}/${filenameCvMapModel(cvm)}" }

  fun hasJsonFloorCvMapModelLocal(f: Floor, m: String): Boolean { return File(jsonFloorCvMapModelLocal(f, m)).exists() }
  fun printJsonFloorCvMapModelLocal(f: Floor, m: DetectionModel) { LOG.V4(TAG, jsonFloorCvMapModelLocal(f, m)) }
  fun hasJsonFloorCvMapModelLocal(f: Floor, m: DetectionModel): Boolean {
    printJsonFloorCvMapModelLocal(f, m)
    return File(jsonFloorCvMapModelLocal(f, m)).exists() }
  fun hasJsonFloorCvMapModelLocal(cvm: CvMap): Boolean { return File(jsonFloorCvMapModelLocal(cvm)).exists() }

  fun deleteFloorCvMapModelLocal(f: Floor, m: String) {  File(jsonFloorCvMapModelLocal(f, m)).delete() }
  fun deleteFloorCvMapModelLocal(f: Floor, m: DetectionModel) {  File(jsonFloorCvMapModelLocal(f, m)).delete() }
  fun deleteFloorCvMapModelLocal(cvm: CvMap) {  File(jsonFloorCvMapModelLocal(cvm)).delete() }

  /** Deletes the local [CvMap]s of all [Space]s. */
  fun deleteCvMapsLocal() {
    val dir = File(spacesDir)
    val spaceFolders = dir.list()
    val totalSpaces=spaceFolders?.size
    var cnt = 0
    spaceFolders?.forEach { spaceFoldername ->
      LOG.D(TAG_METHOD, "DELETE FOR SPACE: $spaceFoldername")
      val spaceDirStr= "${spacesDir}/$spaceFoldername"
      val spaceDir = File(spaceDirStr)
      spaceDir.list()?.forEach { floorFoldername ->

        val cvMapFolder = File("${spaceDirStr}/$floorFoldername/$foldernameCvMap")
        if (cvMapFolder.exists()) { // TODO it's dir
          cvMapFolder.deleteRecursively()
          cnt++
        }
      }
    }

    LOG.D(TAG_METHOD, "Deleted CvMaps in $cnt floors of $totalSpaces spaces.")
  }

  // fun jsonFloorCvMapModel(cvMap: CvMap) : String {  return "${dirFloor(cvMap)}/${cvMap.detectionModel}/$JS_CVMAP" }
  // fun hasJsonFloorCvMapModel(floor: Floor): Boolean { return File(jsonFloorCvMap(floor)).exists() }
  // fun hasJsonFloorCvMapModel(cvMap: CvMap): Boolean { return File(jsonFloorCvMap(cvMap)).exists() }
  // fun deleteFloorCvMap(cvMap: CvMap) {  _deleteFloorCvMap(jsonFloorCvMap(cvMap)) }
  // fun deleteFloorCvMap(floor: Floor) {  _deleteFloorCvMap(jsonFloorCvMap(floor)) }

  /**
   * This overrides any previous Cv Map,
   * so any merging must be done earlier
   *
   */
  fun saveFloorCvMap(cvMap: CvMap): Boolean {
    val filename = jsonFloorCvMapModelLocal(cvMap)
    return try {
      File(dirFloorCvMapsLocal(cvMap)).mkdirs()
      val fw= FileWriter(File(filename))
      Gson().toJson(cvMap, fw)
      fw.close()
      LOG.D2(TAG_METHOD, "$cvMap")
      LOG.D2(TAG_METHOD, "filename: $filename")
      true
    } catch (e: Exception) {
      LOG.E(TAG, "saveFloorCvMap: $filename: ${e.message}")
      false
    }
  }

  fun readFloorCvMap(cvMap: CvMap) = _readFloorCvMap(jsonFloorCvMapModelLocal(cvMap))
  fun readFloorCvMap(floor: Floor, model: DetectionModel)
          = _readFloorCvMap(jsonFloorCvMapModelLocal(floor, model))

  private fun _readFloorCvMap(filename: String): CvMap? {
    LOG.V4(TAG, "_readFloorCvMap: file: $filename")
    try {
      val str = File(filename).readText()
      return Gson().fromJson(str, CvMap::class.java)
    } catch (e: Exception) {
      LOG.E(TAG, "_readFloorCvMap: $filename: ${e.message}")
      // TODO deleting local cache...
    }
    return null
  }

  fun hasFingerprints(): Boolean { return File(fingerprintsFilename).exists() }

  fun storeFingerprints(userCoords: UserCoordinates, detectionsReq: List<CvDetectionREQ>, model: DetectionModel) {
    LOG.D(TAG, "$METHOD: to local cache")
    val time = utlTime.epoch().toString()
    val entry = FingerprintEntry(userCoords, time, detectionsReq, model.idSmas)

    // val gson: Gson = GsonBuilder().create()
    val fw= FileWriter(File(fingerprintsFilename), true)
    val lineEntry = gson.toJson(entry)
    LOG.D2(TAG, "$METHOD: ENTRY: $lineEntry")
    fw.write(lineEntry + "\n")
    fw.close()
  }

  fun topFingerprintsEntry(): FingerprintEntry? {
    val str = File(fingerprintsFilename).bufferedReader().use { it.readLine() } ?: return null

    return gson.fromJson(str, FingerprintEntry::class.java)
  }

  fun deleteFingerprintsCache() {
    File(fingerprintsFilename).delete()
  }

  fun popFingerprintsEntry() {
    removeFirstLine(fingerprintsFilename)

    if (isEmptyFile(fingerprintsFilename))  deleteFingerprintsCache()
  }

  fun isEmptyFile(source: String): Boolean {
    try {
      for (line in Files.readAllLines(Paths.get(source))) {
        if (line != null && line.trim { it <= ' ' }.isNotEmpty()) {
          return false
        }
      }
    } catch (e: IOException) {
    }
    // Default to true.
    return true
  }

  /**
   * https://stackoverflow.com/a/13178980/776345
   */
  private fun removeFirstLine(fileName: String?) {
    val raf = RandomAccessFile(fileName, "rw")

    // Initial write position
    var writePosition: Long = raf.filePointer
    raf.readLine()
    // Shift the next lines upwards.
    var readPosition: Long = raf.filePointer
    val buff = ByteArray(1024)
    var n: Int
    while (-1 != raf.read(buff).also { n = it }) {
      raf.seek(writePosition)
      raf.write(buff, 0, n)
      readPosition += n.toLong()
      writePosition += n.toLong()
      raf.seek(readPosition)
    }
    raf.setLength(writePosition)
    raf.close()
  }

}