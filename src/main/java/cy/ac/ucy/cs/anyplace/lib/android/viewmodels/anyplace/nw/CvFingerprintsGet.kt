package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw

import android.widget.Toast
import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.android.SmasApp
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.SMAS
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.DetectionModel
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.smas.models.*
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.di.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilErr
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.nw.SmasErrors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.Response
import java.lang.Exception

/**
 * Downloads CvMap from remote.
 * This will be used for offline localization
 *
 * NOTE: this is from SMAS API.
 * But cv localization should have been Anyplace API.
 */
class CvFingerprintsGet(
        private val app: SmasApp,
        private val VM: CvViewModel,
        private val RH: RetrofitHolderSmas,
        private val repo: RepoSmas) {
  private val TG = "nw-cv-map"
  private val scope = VM.viewModelScope
  private val utlErr by lazy { UtilErr() }
  private val err by lazy { SmasErrors(app, scope) }

  /** Network Responses from API calls */
  private val resp: MutableStateFlow<NetworkResult<CvMapResp>> = MutableStateFlow(NetworkResult.Unset())

  private val C by lazy { SMAS(app.applicationContext) }

  /**
   * Get fingerprints for ALL the models (a bit hardcoded..
   */
  suspend fun blockingCall(buid: String) {
    val MT = ::blockingCall.name
    LOG.E(TG, MT)
    val smasUser = app.dsUserSmas.read.first()
    val from = getTimestamp()
    DetectionModel.list.forEach { model->
      val modelid = model.idSmas
      val request = CvMapReq(smasUser, modelid, buid, from)

      LOG.E(TG, "$MT: downloading: ${model.modelName}: $buid $from")
      val response = repo.remote.cvMapGet(request)
      LOG.D2(TG, "$MT: ${response.message()}" )
      val resp = handleResponse(response)
      if (resp is NetworkResult.Success) {
        val cvMap = resp.data?.rows
        if (cvMap == null) {
          val msg = "Downloading $TG: no classes fetched"
          LOG.E(TG, msg)
        } else {
          if (cvMap.isNotEmpty()) {
            val msg = "Fetched ${cvMap.size} new fingerprints"
            app.notify.shortDEV(scope, msg)
            LOG.W(TG, "$MT: $msg")
          }
          persistToDB(cvMap)
        }
      }
    }
  }

  /**
   * Get the last timestamp from db
   */
  private fun getTimestamp() : Long {
    return if (repo.local.hasCvFingerprints())  {
      repo.local.getLastFingerprintTimestamp() ?: 0
    } else 0
  }

  private fun getRequest(smasUser: SmasUser): CvMapReq {
    val MT = ::getRequest.name
    val from = getTimestamp()
    val modelid = VM.model.idSmas
    val buid = app.wSpace.obj.buid

    LOG.W(TG, "$MT: ${VM.model.modelName}: $from $buid")

    return CvMapReq(smasUser, modelid, buid, from)
  }

  fun dropFingerprints() {
    if (repo.local.hasCvFingerprints()) {
      LOG.W(TG, "$TG: dropping CvMap")
      repo.local.dropCvFingerprints()
    }
  }

  /** Get [UserLocations] SafeCall */
  suspend fun safeCall(showNotif: Boolean) : Int {
    val MT = ::safeCall.name
    LOG.D2(TG, MT)

    val smasUser = app.dsUserSmas.read.first()
    resp.value = NetworkResult.Loading()

    if (app.hasInternet()) {
      try {
        val response = repo.remote.cvMapGet(getRequest(smasUser))
        LOG.D2(TG, "$MT: ${response.message()}" )
        resp.value = handleResponse(response)

        val cvMap = resp.value.data?.rows
        if (cvMap == null) {
          val msg = "No fingerprints fetched (null)"
          LOG.W(TG, msg)
        } else {
          if (cvMap.isNotEmpty()) {
            val msg = "Fetched ${cvMap.size} new fingerprints"
            if (showNotif) app.notify.shortDEV(scope, msg)
            LOG.E(TG, "$MT: $msg")
          } else {
            var msg = "No fingerprints fetched"
            LOG.W(TG, msg)
          }
          persistToDB(cvMap)
          return cvMap.size
        }
      } catch(e: Exception) {
        val errMsg = utlErr.handle(app, RH, scope, e, TG)
        resp.value = NetworkResult.Error(errMsg)
      }
    } else {
      resp.value = NetworkResult.Error(C.ERR_MSG_NO_INTERNET)
    }
    return 0
  }

  private fun handleResponse(resp: Response<CvMapResp>): NetworkResult<CvMapResp> {
    val MT = ::handleResponse.name
    LOG.D2(TG, MT)
    if(resp.isSuccessful) {
      return when {
        resp.message().toString().contains("timeout") -> NetworkResult.Error("Timeout.")

        resp.isSuccessful -> {
          // SMAS special handling (errors should not be 200/OK)
          val r = resp.body()!!
          if (r.status == "err") return NetworkResult.Error(r.descr)
          return NetworkResult.Success(r)
        } // can be nullable
        else -> NetworkResult.Error(resp.message())
      }
    } else {
      LOG.E(TG, "$MT: unsuccessful")
    }

    return NetworkResult.Error("$TG: ${resp.message()}")
  }

  var collectingCvMap = false
  suspend fun collect() {
    val MT = ::collect.name
    if (collectingCvMap) return; collectingCvMap=true

    LOG.D2(TG, "$MT: $TG")

    resp.collect {
      when (it)  {
        is NetworkResult.Success -> {
          val cvModels = it.data
          LOG.D2(TG, "$TG: received: ${cvModels?.rows?.size} fingerprints")
        }
        is NetworkResult.Error -> {
          LOG.D3(TG, "Error: msg: ${it.message}")
          if (!err.handle(app, it.message, "cvmap-get")) {
            val msg = it.message ?: "unspecified error"
            app.showToast(scope, msg, Toast.LENGTH_SHORT)
          }
        }
        is NetworkResult.Unset -> { }
        else -> { } // might be unset or db loaded
      }
    }
  }

  /**
   * Persists [ChatMsg]s to SQLite (through ROOM).
   * It does not store images (base64) to DB.
   * Those will be stored in [SmasCache] (file cache)
   */
  private fun persistToDB(obj: List<CvMapRow>) {
    val MT = ::persistToDB.name
    LOG.W(TG, "$MT: storing: ${obj.size} CvModel classes")
    scope.launch(Dispatchers.IO) {
      obj.forEach {
        repo.local.insertCvFingerprintRow(it)
      }
    }
  }
}
