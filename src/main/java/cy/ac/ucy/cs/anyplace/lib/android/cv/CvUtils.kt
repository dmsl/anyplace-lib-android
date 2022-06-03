package cy.ac.ucy.cs.anyplace.lib.android.cv

import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.DetectionModel
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.Classifier
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.CvDetection
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

  /**
   * [id] model id
   */
  suspend fun initConversionTables(id: Int) {
    if (initedClasses.contains(id)) return

    val cvModelClasses = repoSmas.local.readCvModelClasses(id)
    cvModelClasses.forEach { cvClass ->
      LOG.D5(TAG, "$METHOD: SmasModelClasses: ${cvClass.modelid} ${cvClass.name}")
      hmap[CvKey(id, cvClass.cid)] = cvClass.oid
    }

    initedClasses.add(id)
  }

  private fun isInited(modelId: Int) = initedClasses.contains(modelId)

  fun toCvDetection(detection: Classifier.Recognition, model: DetectionModel) : CvDetection? {
    val modelId = model.idSmas
    if (!isInited(modelId)) return null

    // CHECK: probably not needed as the above check will exit anyway
    // // to proceed: Must have CvModels on DB first
    // if (!repoSmas.local.hasCvModelClassesDownloaded()) {
    //   val msg = "Cannot upload detections (must download CvModels first)"
    //   LOG.E(TAG, "$METHOD: $msg")
    //   return null
    // }

    val key = CvKey(modelId, detection.detectedClass)
    val oid = hmap[key]
    if (oid == null) {
      LOG.E(TAG, "$METHOD: No class for: ${detection.detectedClass}:${detection.title}")
      return null
    } else {
      val cvd= CvDetection(oid,
              detection.location.width().toDouble(),
              detection.location.height().toDouble(),
              null, detection.title)
      LOG.D(TAG, "$METHOD: CvModel: READY: ${cvd.oid}: w:${cvd.width} h:${cvd.height}")
      return cvd
    }
  }

}