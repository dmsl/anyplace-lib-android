package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw

import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.SmasApp
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.SMAS
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.DetectionModel
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.smas.models.*
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.di.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilErr
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.nw.SmasErrors
import kotlinx.coroutines.flow.first
import retrofit2.Response
import java.lang.Exception

/**
 * Downloads CvModel files from remote
 *
 * NOTE: this was very quickly written.
 * Probably not handling everything as we should
 */
class CvModelFilesGetNW(
        private val app: SmasApp,
        private val VM: CvViewModel,
        private val RH: RetrofitHolderSmas,
        private val repo: RepoSmas) {
  private val TG = "nw-cv-model-files"
  private val err by lazy { SmasErrors(app, VM.viewModelScope) }
  private val utlErr by lazy { UtilErr() }
  private val cache = VM.cache
  private val scope = VM.viewModelScope
  private val notify = app.notify

  /** Network Responses from API calls */
  private val C by lazy { SMAS(app.applicationContext) }
  private lateinit var user : SmasUser

  suspend fun downloadAllModels() {
    val MT = ::downloadAllModels.name
    DetectionModel.list.forEach { model ->
      LOG.W(TG, "$MT: ${model.modelName}")
      callBlocking(model.idSmas)
    }
  }

  /** Get [UserLocations] SafeCall */
  suspend fun callBlocking(modelId: Int) : Boolean {
    val MT = ::callBlocking.name
    LOG.W(TG, "$MT: downloading: $modelId")
    user = app.dsUserSmas.read.first()

    if (cache.hasCvModelFilesDownloaded(modelId)) {
      LOG.W(TG, "$MT: model $modelId already cached..")
      return true
    }

    if (app.hasInternet()) {
      return try {
        val response = repo.remote.cvModelFilesGet(CvModelFilesReq(user.uid, user.sessionkey, modelId))
        return handleResponse(response)
      } catch(e: Exception) {
        val msg = "$TG/$MT: ${e.message}"
        LOG.E(TG, msg)
        notify.DEV(scope, msg)

        false
      }
    } else {
      LOG.E(TG, "No internet connection")
      return false
    }
  }

  /**
   * Handles remote reponse.
   * If all goes well, it will store the weights (tflite) and the classes (obj.names) in the cache.
   */
  private fun handleResponse(resp: Response<CvModelFilesResp>) : Boolean {
    val MT = ::handleResponse.name
    LOG.D(TG, MT)

    if(resp.isSuccessful) {
      return when {
        resp.message().toString().contains("timeout") -> {
          val msg = "$TG/$MT: downloading models timeout"
          LOG.E(TG, msg)
          notify.DEV(scope, msg)
          return false
        }

        resp.isSuccessful -> {  // SMAS special handling (errors should not be 200/OK)
          val r = resp.body()!!
          if (r.status == "err") {
            val msg = "$TG/$MT: ${r.descr}"
            LOG.E(TG, msg)
            notify.DEV(scope, msg)
            return false
          }

          if (r.rows.classes.isEmpty() || r.rows.weights.isEmpty()) {
            val msg="$TG/$MT: classes or weights were empty"
            LOG.E(TG, msg)
            notify.DEV(scope, msg)
            return false
          }

          val modelid = r.rows.modelid
          if(!cache.storeModelFileLabels(modelid, r.rows.classes)) {
            val msg ="$TG/$MT: failed to store cv model classes (obj.names)"
            LOG.E(TG, msg)
            return false
          }
          if(!cache.storeModelFileWeights(modelid, r.rows.weights)) {
            val msg ="$TG/$MT: failed to store cv model weights (tflite)"
            LOG.E(TG, msg)
            return false
          }

          return true
        } // can be nullable
        else -> {
          val msg = "$MT: ${resp.message()}"
          LOG.E(TG, msg)
          notify.DEV(scope, msg)
          false
        }
      }
    } else {
      return false
    }
  }



}
