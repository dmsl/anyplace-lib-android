package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers

import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.legacy_cv_gnk.tensorflow.legacy.gnk.utils.Detector
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.DetectionModel
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.Classifier
import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationResult
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Coord
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.CvDetection
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.CvMap
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.min
import kotlin.math.sqrt

/** Extension for pretty printing purposes */
fun Boolean.toInt() = if (this) 1 else 0

class CoordClass(
        /** Coordinates */
        val coord: Coord,
        /** Detection class ID */
        val classId: Int) {
  override fun equals(other: Any?): Boolean {
     return when (other) {
        is CoordClass -> {
          this.coord.lat == other.coord.lat &&
                  this.coord.lon == other.coord.lon &&
                  this.classId == other.classId
        }
        else -> false
      }
  }

  override fun hashCode(): Int {
    return coord.lat.hashCode() + coord.lon.hashCode() + classId.hashCode()
  }

}

/**
 * Helper for creating a [PriorityQueue].
 *
 * - [distance] is the euclidian distance between a particular [row]
 *   and a given input of detections
 */
class DistanceRow(val distance: Double, val row: Int): Comparable<DistanceRow> {
  override fun compareTo(other: DistanceRow): Int {
    return distance.compareTo(other.distance)
  }
}

/**
 * Optimized representation of a [CvMap].
 */
class CvMapFast(private val cvMap: CvMap, private val labels: List<String>) {
  /** [rows] are the different locations where objects were detected.  */
  private val rows = cvMap.locations.size

  /** [columns] is the number of classes. It depends on the used YOLO detectionModel. */
  private val columns = labels.size

  /** length of the label with the biggest name */
  private var maxLabelLength: Int = 0

  /** NxM bitmap (actually byte map, but no difference in JVM ANW),
   * If bitmap[i][j] == 1: location [i] contains some objects of label[j]
   */
  private val bitmap = Array(rows) { BooleanArray(columns) }

  /** Maps a [String] label to it's class id.
   * Using a little bit of space for O(1) lookups */
  private val labelMap: HashMap<String, Int> = HashMap()

  /** for debugging */
  private val _locNames: HashMap<String, String> = HashMap()

  /** Mapping (coordinates+class id) to a list of detections.
   * This allows us accessing the full detection info.
   *
   * For example, if the [bitmap] indicates that location [i] has objects of class [j],
   * then the [locationMap] allows retrieving the list of those objects.
   *
   * It might be the case that there are m objects of class [j] in location [i]
   */
  private val locationMap: HashMap<CoordClass, MutableList<CvDetection>> = HashMap()

  /** [Coord] for each row of the bitmap */
  private val coordinates = arrayOfNulls<Coord>(rows)

  /** Number of occurrences per label/class */
  private val occurrences = IntArray(columns)

  init {
    populateLabelMap()
    populateBitmap()
    computeImportance()
    LOG.D(TAG, "CvMapFast: $rows x $columns")
  }

  /**
   * Populates:
   * - [coordinates] rows
   * - [bitmap] NxM array, and
   * - [locationMap]
   */
  fun populateBitmap() {
    LOG.D2(TAG, "populateBitmap")
    for (locationIndex in cvMap.locations.indices) {
      val location = cvMap.locations[locationIndex]

      val coord = Coord.get(location.lat, location.lon)
      val dbgkey = coord.lat.toString()+"_"+coord.lon.toString()
      _locNames[dbgkey] = "LOC${locationIndex+1}"
      var locValues = ""
      location.detections.forEach {
        val idx: Int = labelMap[it.detection]!!

        locValues += " ${it.detection}:$idx"
        // populate locations
        coordinates[locationIndex] = coord

        // populate bitmap
        bitmap[locationIndex][idx] = true

        // populate location map
        val coordClass = CoordClass(coord, idx)
        if (locationMap[coordClass] == null) {
          locationMap[coordClass] = mutableListOf(it)
        } else {
          locationMap[coordClass]?.add(it)
        }
      }

      LOG.V3(TAG_METHOD, "${_locNames[dbgkey]}: classes: $locValues")
    }


    locationMap.forEach { (coordClass, detections) ->
      val dbgkey = coordClass.coord.lat.toString()+"_"+coordClass.coord.lon.toString()
      LOG.V4(TAG_METHOD, "LM: ${_locNames[dbgkey]}: class: ${labels[coordClass.classId]}:${coordClass.classId} num: ${detections.size} ")
      // LOG.D("${_locNames[dbgkey]}: class: ${it.detection} idx:$idx")
    }


    if (DBG.D3) printBitmap(maxLabelLength, rows, columns, bitmap, labels)
  }

  /**
   * find the importance of scanned element (in this cvmap)
   * by doing a horizontal iteration:
   * - get sum of an object's class (label) in all locations
   *
   * TODO:PM importance. now it finds only the occurrences.
   */
  private fun computeImportance() {
    for (j in 0 until columns) {
      for (i in 0 until rows) {
        if (bitmap[i][j]) {
          // get the number of detections
          val coord = coordinates[i]!!
          val coordClass = CoordClass(coord, j)
          val classDetections = locationMap[coordClass]
          val detectionNum = classDetections?.size ?: 0
          if (detectionNum > 0) {
            LOG.D3("$coord: detections of ${labels[j]}: $detectionNum")
          }
          occurrences[j] += detectionNum
        }
      }
    }
    if (DBG.D3) {
      printImportance()
    }
  }

  /**
   * Construct a [HashMap] to get a class index from a class name
   * Using a bit of space for O(1) class-id lookups.
   * Requires: O(n).
   *
   * It also calculates the maximum number of characters in a class.
   */
  private fun populateLabelMap() {
    LOG.D2("getLabelMap:")
    var maxChars = 0
    for (idx in labels.indices) {
      val cl = labels[idx]
      LOG.V5(TAG, "LBL: $idx:$cl")
      labelMap[cl] = idx

      if (cl.length > maxChars) maxChars = cl.length
    }
    maxLabelLength = maxChars
  }

  /**
   * Pretty printing the [occurrences]
   */
  private fun printImportance() {
    LOG.D("Importance:")
    for (j in 0 until columns) {
      if (occurrences[j] > 0) {
        LOG.D(TAG, "${labels[j]}: ${occurrences[j]}")
      }
    }
  }

  /**
   * Pretty printing the [bitmap]
   */
  fun printBitmap(maxLabelLength: Int, rows: Int, columns: Int,
                  bitmap: Array<BooleanArray>, labels: List<String>) {
    for (i in maxLabelLength - 1 downTo 0) {
      var headerLine = ""
      for (j in 0 until columns) {
        val len = labels[j].length
        headerLine += if (i < len) "${labels[j][len - 1 - i]} " else "  "
      }
      LOG.D(TAG, headerLine)
    }
    for (i in 0 until rows) {
      var rowS = ""
      for (j in 0 until columns) {
        rowS += "${bitmap[i][j].toInt()} "
      }
      LOG.D(TAG, rowS)
    }
  }

  fun getObjectsInRow(row: Int): String {
    // var rowBitmap = bitmap[row]
    var res = ""
    for (j in 0 until columns) {
      if (bitmap[row][j]) {
        // get the number of detections
        val coord = coordinates[row]!!
        val coordClass = CoordClass(coord, j)
        val classDetections = locationMap[coordClass]
        val detectionNum = classDetections?.size ?: 0
        if (detectionNum > 0) {
          res+=" ${labels[j]}: $detectionNum"
        }
      }
    }
    return res
  }

  @Deprecated("")
  fun estimatePosition(detectionModel: DetectionModel, detections: List<Detector.Detection>)
          : LocalizationResult {
    LOG.W(TAG, "estimatePosition")

    if (detectionModel.modelName.lowercase() != cvMap.detectionModel.lowercase()) {
      val msg = "Wrong model used"
      val details = "${detectionModel.modelName} instead of ${cvMap.detectionModel}"
      LOG.E(TAG, "$msg: $details")
      return LocalizationResult.Error(msg, details)
    }

    LOG.W(TAG, "inputMap: generating...")
    val inputMap: HashMap<Int, MutableList<CvDetection>> = HashMap()
    detections.forEach {
      val idx = labelMap[it.className]!!
      val cvDetection = CvMapHelper.toCvDetection(it)
      if (inputMap[idx] == null) {
        inputMap[idx] = mutableListOf(cvDetection)
      } else {
        inputMap[idx]?.add(cvDetection)
      }
    }

    LOG.W(TAG, "inputMap: done")

    return NN(inputMap)  // TODO make this an option
  }


  fun estimatePositionNEW(detectionModel: DetectionModel, detections: List<Classifier.Recognition>)
          : LocalizationResult {
    LOG.W(TAG, "estimatePosition")

    if (detectionModel.modelName.lowercase() != cvMap.detectionModel.lowercase()) {
      val msg = "Wrong model used"
      val details = "${detectionModel.modelName} instead of ${cvMap.detectionModel}"
      LOG.E(TAG, "$msg: $details")
      return LocalizationResult.Error(msg, details)
    }

    LOG.W(TAG, "inputMap: generating...")
    val inputMap: HashMap<Int, MutableList<CvDetection>> = HashMap()
    detections.forEach {
      val idx = labelMap[it.title]!!
      val cvDetection = CvMapHelper.toCvDetection(it)
      if (inputMap[idx] == null) {
        inputMap[idx] = mutableListOf(cvDetection)
      } else {
        inputMap[idx]?.add(cvDetection)
      }
    }

    LOG.W(TAG, "inputMap: done")

    return NN(inputMap)  // TODO make this an option
  }

  /**
   *
   * Nearest Neighbor
   *
   * BestMatch..
   */
  private fun NN(inputMap: HashMap<Int, MutableList<CvDetection>>): LocalizationResult {
    LOG.D(TAG, "NN calling..")
    val distances: PriorityQueue<DistanceRow> = PriorityQueue()

    inputMap.forEach { (classId, detections) ->
      // detections.forEach { strDets+=" ${it.detection}" } // CLR:PM
      LOG.W(TAG_METHOD, "INPUT: ${labels[classId]}: ${detections.size}")
    }

    locationMap.forEach { (coordClass, detections) ->
      val dbgkey = coordClass.coord.lat.toString()+"_"+coordClass.coord.lon.toString()
      LOG.D(TAG_METHOD,"LMP: ${_locNames[dbgkey]}: ${labels[coordClass.classId]}: detections: ${detections.size} ")
      // LOG.D("${_locNames[dbgkey]}: class: ${it.detection} idx:$idx")
    }

    for (i in 0 until rows) {
      val dist = -calculateEuclideanDistance(i, inputMap)
      LOG.D(TAG_METHOD, "dist: ${dist}: for loc${i}: ${getObjectsInRow(i)}")
      distances.add(DistanceRow(dist, i))
    }

    LOG.D(TAG_METHOD, "got distances")

    // TODO FIX THIS:
    val top = distances.element()
    val coord = coordinates[top.row]
    if (coord != null) {
      val dbgkey = coord.lat.toString()+"_"+coord.lon.toString()
      LOG.D(TAG, "ret top: ${_locNames[dbgkey]}: ${coord} : ${top.distance}")

      return if (DBG.D2) LocalizationResult.Success(coord, _locNames[dbgkey])
      else LocalizationResult.Success(coord)
    } else {
      LOG.D(TAG, "NN empty PQueue")
      return LocalizationResult.Error("NN: Failed to get location")
    }
  }


  /**
   * Calculates the Euclidean Distance between a [CvMapFast] [row] and
   * an [inputMap] [HashMap] of detections.
   *
   * [inputMap] has:
   * - key: the class / label ID of the [DetectionModel]
   * - value: a list of [CvDetection]
   *
   * It is possible that we have more than 1 detection of the same type at a
   * particular location
   *
   */
  fun calculateEuclideanDistance(row: Int, inputMap: HashMap<Int, MutableList<CvDetection>>): Double {
    var finalResult = 0
    val coord = coordinates[row]!!

    inputMap.forEach { (classId, detections) ->
      val inputObjects = detections.size
      val coordClass = CoordClass(coord, classId)
      val scannedObjects = locationMap[coordClass]?.size ?: 0

      // var diff = v1 - v2
      // diff *= diff
      var matched = min(inputObjects, scannedObjects)
      LOG.D(TAG, "${labels[classId]}: in:$inputObjects cv:$scannedObjects diff:$matched")
      matched *= matched

      finalResult += matched
    }
    return sqrt(finalResult.toDouble())
  }
}
