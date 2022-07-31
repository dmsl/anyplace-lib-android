package cy.ac.ucy.cs.anyplace.lib.android.cache.anyplace

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.DetectionModel
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.*
import cy.ac.ucy.cs.anyplace.lib.smas.models.CvObjectReq
import cy.ac.ucy.cs.anyplace.lib.smas.models.FingerprintScan
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths

/**
 * File cache using [ctx.filesDir]/spaces as a base directory.
 *
 */
open class Cache(val ctx: Context) {

  companion object {
    // FILES
    //// Space
    const val JS_SPACE="space.json"
    const val JS_FLOORS = "floors.json"
    // const val JS_FLOORS="floors.json"
    const val JS_SPACE_LASTVAL="s.lastval.json" // Cached settings, like last floor
    const val JS_SPACE_CONNECTIONS="s.connections.json"
    const val JS_SPACE_POIS="s.pois.json"
    //// Space/Floor
    const val PNG_FLOORPLAN = "f.png"
  }

  private val gson : Gson by lazy { GsonBuilder().create() }

  val baseDir get() = "${ctx.filesDir}"

  val spacesDir get() = "$baseDir/spaces"
  val fingerprintsFilename get() = "$baseDir/cv-fingerprints.jsonl"

  // SPACES

  fun dirSpace(buid: String) : String {  return "$spacesDir/$buid"  }
  fun dirSpace(space: Space) : String {  return "$spacesDir/${space.id}"  }
  fun dirSpace(floor: Floor) : String {  return "$spacesDir/${floor.buid}" }
  fun dirSpace(floors: Floors) : String {  return "$spacesDir/${floors.floors[0].buid}" }
  fun jsonSpace(space: Space) : String {  return "${dirSpace(space)}/$JS_SPACE" }
  fun jsonSpace(buid: String) : String {  return "${dirSpace(buid)}/$JS_SPACE" }
  fun hasJsonSpace(buid: String) : Boolean {  return File(jsonSpace(buid)).exists() }
  fun deleteJsonSpace(space: Space) : Boolean {  return File(jsonSpace(space)).delete() }

  fun readJsonSpace(buid: String): Space? {
    val filename = jsonSpace(buid)
    LOG.V4(TAG, "$METHOD: file: $filename")
    try {
      val json = File(filename).readText()
      return Gson().fromJson(json, Space::class.java)
    } catch (e: Exception) {
      LOG.E(TAG, "$METHOD: $filename: ${e.message}")
    }
    return null
  }

  fun storeJsonSpace(space: Space): Boolean {
    val filename = jsonSpace(space)
    return try {
      File(dirSpace(space)).mkdirs()
      val fw = FileWriter(File(filename))
      Gson().toJson(space, fw)
      fw.close()
      LOG.V3(TAG, "$METHOD: $filename")
      true
    } catch (e: Exception) {
      LOG.E(TAG, "$METHOD: $filename: ${e.message}")
      false
    }
  }


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

  fun jsonSpaceConnections(space: Space) : String {  return "${dirSpace(space)}/$JS_SPACE_CONNECTIONS" }
  fun hasSpaceConnections(space: Space): Boolean { return File(jsonSpaceConnections(space)).exists() }
  fun deleteSpaceConnections(space: Space) {  File(jsonSpaceConnections(space)).delete()  }

  fun saveSpaceConnections(space: Space, connections: ConnectionsResp): Boolean {
    val filename = jsonSpaceConnections(space)
    return try {
      File(dirSpace(space)).mkdirs()
      val fw= FileWriter(File(filename))
      Gson().toJson(connections, fw)
      fw.close()
      true
    } catch (e: Exception) {
      LOG.E(TAG, "$METHOD: $filename: ${e.message}")
      false
    }
  }

  fun readSpaceConnections(space: Space): ConnectionsResp? {
    val filename = jsonSpaceConnections(space)
    LOG.V4(TAG, "$METHOD: file: $filename")
    try {
      val json = File(filename).readText()
      return Gson().fromJson(json, ConnectionsResp::class.java)
    } catch (e: Exception) {
      LOG.E(TAG, "$METHOD: $filename: ${e.message}")
    }
    return null
  }


  fun jsonSpacePOIs(space: Space) : String {  return "${dirSpace(space)}/$JS_SPACE_POIS" }
  fun hasSpacePOIs(space: Space): Boolean { return File(jsonSpacePOIs(space)).exists() }
  fun deleteSpacePOIs(space: Space) {  File(jsonSpacePOIs(space)).delete()  }
  fun saveSpacePois(space: Space, pois: POIsResp) : Boolean {
    val filename = jsonSpacePOIs(space)
    return try {
      File(dirSpace(space)).mkdirs()
      val fw= FileWriter(File(filename))
      Gson().toJson(pois, fw)
      fw.close()
      true
    } catch (e: Exception) {
      LOG.E(TAG, "$METHOD: $filename: ${e.message}")
      false
    }
  }

  fun readSpacePOIs(space: Space): POIsResp? {
    val filename = jsonSpacePOIs(space)
    LOG.V4(TAG, "$METHOD: file: $filename")
    try {
      val json = File(filename).readText()
      return Gson().fromJson(json, POIsResp::class.java)
    } catch (e: Exception) {
      LOG.E(TAG, "$METHOD: $filename: ${e.message}")
    }
    return null
  }

  fun hasSpaceConnectionsAndPois(space: Space): Boolean {
    return hasSpacePOIs(space) && hasSpaceConnections(space)
  }

  fun jsonFloors(buid: String) : String {  return "${dirSpace(buid)}/$JS_FLOORS"  }
  fun jsonFloors(floors: Floors) : String {  return "${dirSpace(floors)}/$JS_FLOORS"  }
  fun hasJsonFloors(buid: String) : Boolean {  return File(jsonFloors(buid)).exists() }
  fun deleteJsonFloors(floors: Floors) : Boolean {  return File(jsonFloors(floors)).delete() }

  fun readJsonFloors(buid: String): Floors? {
    val filename = jsonFloors(buid)
    LOG.V4(TAG, "$METHOD: file: $filename")
    try {
      val json = File(filename).readText()
      return Gson().fromJson(json, Floors::class.java)
    } catch (e: Exception) {
      LOG.E(TAG, "$METHOD: $filename: ${e.message}")
    }
    return null
  }

  fun storeJsonFloors(floors: Floors): Boolean {
    val filename = jsonFloors(floors)
    return try {
      File(dirSpace(floors)).mkdirs()
      val fw = FileWriter(File(filename))
      Gson().toJson(floors, fw)
      fw.close()
      LOG.V3(TAG, "$METHOD: $filename")
      true
    } catch (e: Exception) {
      LOG.E(TAG, "$METHOD: $filename: ${e.message}")
      false
    }
  }

  //// FLOOR
  fun dirFloor(floor: Floor) : String { return "${dirSpace(floor)}/${floor.floorNumber}" }
  fun floorplan(floor: Floor) : String { return "${dirFloor(floor)}/$PNG_FLOORPLAN" }

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

  fun hasFingerprints(): Boolean { return File(fingerprintsFilename).exists() }

  fun storeFingerprints(userCoords: UserCoordinates, detectionsReq: List<CvObjectReq>, model: DetectionModel) {
    LOG.D(TAG, "$METHOD: to local cache")
    val time = utlTime.epoch().toString()
    val entry = FingerprintScan(userCoords, time, detectionsReq, model.idSmas)

    // val gson: Gson = GsonBuilder().create()
    val fw= FileWriter(File(fingerprintsFilename), true)
    val lineEntry = gson.toJson(entry)
    LOG.D2(TAG, "$METHOD: ENTRY: $lineEntry")
    fw.write(lineEntry + "\n")
    fw.close()
  }

  fun topFingerprintsEntry(): FingerprintScan? {
    val str = File(fingerprintsFilename).bufferedReader().use { it.readLine() } ?: return null

    return gson.fromJson(str, FingerprintScan::class.java)
  }

  fun deleteFingerprintsCache() {
    File(fingerprintsFilename).delete()
  }

  fun countFingerprintsCacheLines(): Int {
    return try {
      var lines = 0
      for (line in Files.readAllLines(Paths.get(fingerprintsFilename))) {
        if (line != null && line.trim { it <= ' ' }.isNotEmpty()) {
          lines ++
        }
      }
      lines
    } catch (e: IOException) {
      0
    }
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

  fun hasSpaceAndFloor(buid: String): Boolean {
    return hasJsonSpace(buid) && hasJsonFloors(buid)
  }

}