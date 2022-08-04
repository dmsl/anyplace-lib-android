package cy.ac.ucy.cs.anyplace.lib.android.utils.cv

import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.DetectionModel
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.Classifier
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.smas.models.CvObject
import cy.ac.ucy.cs.anyplace.lib.smas.models.CvObjectReq
import kotlinx.coroutines.CoroutineScope

data class CvKey (
        val modelId: Int,
        val classId: Int,
)

class CvUtils(
        private val app: AnyplaceApp,
        private val repoSmas: RepoSmas,
) {
  private val TG = "utils-cv"

  private val initedClasses: HashSet<Int> = HashSet()
  private val hmap : HashMap<CvKey, Int> = HashMap ()

  /** notify when the CvModels are once again ready */
  var showNotification = false

  fun clearConvertionTables () {
    initedClasses.clear()
    hmap.clear()
  }

  /**
   * CvModels have some identifiers in the SMAS backend
   * So we need some mapping between those identifiers and the YOLO model itentifiers
   * - e.g. modelid X and oid Y might be mapped to Z in the yolo model (obj.names)
   * - these are stored locally (in SQLite, see [repoSmas.local.readCvModelClasses])
   * - instead of running a query each time, we prepare once a hash map ([hmap])
   * - then we use that hashmap whenever a conversion is needed
   *   - e.g. when we have some new scans and we want to run local or remote localization
   *
   * [id] model id
   */
  suspend fun initConversionTables(id: Int) {
    val MT = ::initConversionTables.name
    if (initedClasses.contains(id)) return

    LOG.D2(TG,  "Initing conversion tables..")
    val cvModelClasses = repoSmas.local.readCvModelClasses(id)
    cvModelClasses.forEach { cvClass ->
      LOG.D5(TG,  "$MT: SmasModelClasses: ${cvClass.modelid} ${cvClass.name}")
      hmap[CvKey(id, cvClass.cid)] = cvClass.oid
    }

    initedClasses.add(id)
  }

  fun isModelInited() = initedClasses.isNotEmpty() && hmap.isNotEmpty()

  private fun isModelInited(modelId: Int) = initedClasses.contains(modelId)

  /**
   * Converts the YOLO [Classifier.Recognition] to [CvObject] that can be used by SMAS
   * Requests further trim it to [CvObjectReq]
   */
  fun toCvDetection(scope: CoroutineScope, detection: Classifier.Recognition, model: DetectionModel) : CvObject? {
    val MT = ::toCvDetection.name
    val modelId = model.idSmas
    if (!isModelInited(modelId)) {
      app.showToast(scope, "Cannot process detection (model classes not found)")
      return null
    }

    val key = CvKey(modelId, detection.detectedClass)
    val oid = hmap[key]

    return if (oid == null) {
      LOG.E(TG,  "$MT: No class for: ${detection.detectedClass}:${detection.title}")
      null
    } else {
      return CvObject(oid,
              detection.location.width().toDouble(),
              detection.location.height().toDouble(),
              detection.title, detection.ocr)
    }
  }

  /**
   * Converts a list of [Classifier.Recognition] (YOLOV4),
   * to a list of CvDetectionREQ that the SMAS backend understands.
   */
  fun toCvDetections(scope: CoroutineScope, recognitions: List<Classifier.Recognition>, model: DetectionModel) : List<CvObjectReq> {
    val MT = ::toCvDetections.name
    LOG.D2(TG,  MT)
    // build detections request
    val detections = mutableListOf<CvObjectReq>()
    recognitions.forEach { detection ->
      val detectionStr = "${detection.detectedClass}:${detection.title}"
      val modelStr = "${model.idSmas}:${model.modelName}"

      LOG.V3(TG,  "$MT: CvModel: $detectionStr: $modelStr")

      val cvd = app.cvUtils.toCvDetection(scope, detection, model)
      if (cvd != null) {
        val detReq = CvObjectReq(cvd)
        LOG.V3(TG,  "$MT: CvModel: READY: ${detReq.oid}: w:${detReq.width} h:${detReq.height}")
        detections.add(detReq)
      }
    }
    return detections
  }

}