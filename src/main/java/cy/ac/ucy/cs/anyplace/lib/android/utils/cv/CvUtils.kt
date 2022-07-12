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

  private val initedClasses: HashSet<Int> = HashSet()
  private val hmap : HashMap<CvKey, Int> = HashMap ()

  /** notify when the CvModels are once again ready */
  var showNotification = false

  fun clearConvertionTables () {
    initedClasses.clear()
    hmap.clear()
  }

  /**
   * [id] model id
   */
  suspend fun initConversionTables(id: Int) {
    if (initedClasses.contains(id)) return

    LOG.D2(TAG, "Initing conversion tables..")
    val cvModelClasses = repoSmas.local.readCvModelClasses(id)
    cvModelClasses.forEach { cvClass ->
      LOG.D5(TAG, "$METHOD: SmasModelClasses: ${cvClass.modelid} ${cvClass.name}")
      hmap[CvKey(id, cvClass.cid)] = cvClass.oid
    }

    initedClasses.add(id)
  }

  fun isModelInited() = initedClasses.isNotEmpty() && hmap.isNotEmpty()

  private fun isModelInited(modelId: Int) = initedClasses.contains(modelId)

  /**
   * Converts the YOLO [Classifier.Recognition] to [CvObject] that can be used by SMAS
   *
   * Requests further trim it to [CvObjectReq]
   */
  fun toCvDetection(scope: CoroutineScope, detection: Classifier.Recognition, model: DetectionModel) : CvObject? {
    val modelId = model.idSmas
    if (!isModelInited(modelId)) {
      app.showToast(scope, "Cannot process detection (model classes not found)")
      return null
    }

    val key = CvKey(modelId, detection.detectedClass)
    val oid = hmap[key]

    return if (oid == null) {
      LOG.E(TAG, "$METHOD: No class for: ${detection.detectedClass}:${detection.title}")
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
    LOG.D2(TAG, METHOD)
    // build detections request
    val detections = mutableListOf<CvObjectReq>()
    recognitions.forEach { detection ->
      val detectionStr = "${detection.detectedClass}:${detection.title}"
      val modelStr = "${model.idSmas}:${model.modelName}"

      LOG.V3(TAG, "$METHOD: CvModel: $detectionStr: $modelStr")

      val cvd = app.cvUtils.toCvDetection(scope, detection, model)
      if (cvd != null) {
        val detReq = CvObjectReq(cvd)
        LOG.V3(TAG, "$METHOD: CvModel: READY: ${detReq.oid}: w:${detReq.width} h:${detReq.height}")
        detections.add(detReq)
      }
    }
    return detections
  }

}