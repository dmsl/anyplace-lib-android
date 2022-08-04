package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw

import android.widget.Toast
import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.NavigatorAppBase
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.smas.ChatUserAuth
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.SMAS
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
 * Downloads CvModels from remote
 *
 * NOTE: this is from SMAS API.
 * But cv localization should have been Anyplace API.
 */
class CvModelsGetNW(
        private val app: NavigatorAppBase,
        private val VM: CvViewModel,
        private val RH: RetrofitHolderSmas,
        private val repo: RepoSmas) {
  private val TG = "nw-cv-models"
  private val err by lazy { SmasErrors(app, VM.viewModelScope) }
  private val utlErr by lazy { UtilErr() }


  /** Network Responses from API calls */
  private val resp: MutableStateFlow<NetworkResult<CvModelsResp>> = MutableStateFlow(NetworkResult.Unset())

  private val C by lazy { SMAS(app.applicationContext) }

  /**
   * Downloads only the recourses. no other processing yet
   */
  suspend fun conditionalBlockingCall() : Boolean {
    val MT = ::conditionalBlockingCall.name
    val smasUser = app.dsUserSmas.read.first()
    if (!repo.local.hasCvModelClassesDownloaded()) {
      val response = repo.remote.cvModelsGet(ChatUserAuth(smasUser))
      LOG.D4(TG, "$TG: ${response.message()}")
      val resp = handleResponse(response)
      try {
        val cvModels = resp.data?.rows
        if (cvModels == null) {
          val msg = "Downloading $TG: no classes fetched"
          LOG.W(TG, msg)
          return false
        } else {
          cvModels.forEach {
            LOG.V3(TG, "$MT: ${it.oid}: ${it.modeldescr}.${it.cid}| ${it.name}")
          }
          persistToDB(cvModels)
          return true
        }
      } catch(e: Exception) {
        LOG.E(TG, "$MT: error: ${e.message}")
        return false
      }
    }
    return true
  }

  /** Get [UserLocations] SafeCall */
  suspend fun safeCall() {
    val MT = ::safeCall.name
    LOG.D2(TG, MT)
    val smasUser = app.dsUserSmas.read.first()

    resp.value = NetworkResult.Loading()
    if (repo.local.hasCvModelClassesDownloaded()) {
      LOG.W(TG, "$TG: already in DB")
      resp.value = NetworkResult.Unset(NetworkResult.DB_LOADED)

      // cache (once) the model ids for faster conversion
      initCvClassConversion()
      return
    }

    if (app.hasInternet()) {
      try {
        val response = repo.remote.cvModelsGet(ChatUserAuth(smasUser))
        LOG.D4(TG, "$TG: ${response.message()}" )
        resp.value = handleResponse(response)

        val cvModels = resp.value.data?.rows
        if (cvModels == null) {
          val msg = "Downloading $TG: no classes fetched"

          LOG.W(TG, msg)
        } else {
          cvModels.forEach {
            LOG.V3(TG, "$MT: ${it.oid}: ${it.modeldescr}.${it.cid}| ${it.name}")
          }
          persistToDB(cvModels)

        }
      } catch(e: Exception) {
        val errMsg = utlErr.handle(app, RH, VM.viewModelScope, e, TG)
        resp.value = NetworkResult.Error(errMsg)
      }
    } else {
      resp.value = NetworkResult.Error(C.ERR_MSG_NO_INTERNET)
    }
  }

  private fun handleResponse(resp: Response<CvModelsResp>): NetworkResult<CvModelsResp> {
    val MT = ::handleResponse.name
    LOG.D3(TG, MT)
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
      LOG.E(TG, "$MT: unsuccessful: ${resp.errorBody()}/${resp.body()}/${resp.message()}")
    }

    return NetworkResult.Error("$TG: ${resp.message()}")
  }

  var collectingCvModels = false
  suspend fun collect() {
    val MT = ::collect.name
    LOG.D2(TG, "$MT: $TG")

    if (collectingCvModels) return
    collectingCvModels=true

    resp.collect {
      when (it)  {
        is NetworkResult.Success -> {
          val cvModels = it.data
          LOG.D2(TG, "$TG: received: ${cvModels?.rows?.size} classes")
        }
        is NetworkResult.Error -> {
          LOG.D3(TG, "Error: msg: ${it.message}")
          if (!err.handle(app, it.message, "loc-get")) {
            val msg = it.message ?: "unspecified error"
            app.showToast(VM.viewModelScope, msg, Toast.LENGTH_SHORT)
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
  private fun persistToDB(obj: List<CvModelClass>) {
    val MT = ::persistToDB.name
    LOG.W(TG, "$MT: storing: ${obj.size} $TG classes")
    VM.viewModelScope.launch(Dispatchers.IO) {
      obj.forEach {
        repo.local.insertCvModelClass(it)
      }
    }
    app.cvUtils.clearConvertionTables()
    initCvClassConversion()
  }

  /**
   * Initialize maps that speed up conversion between modelid
   * and cid (YOLO-derived) to the oid that SMAS uses (CV backend).
   */
  private fun initCvClassConversion() {
    VM.viewModelScope.launch(Dispatchers.IO) {
      val ids = repo.local.getCvModelIds()
      ids.forEach { id -> app.cvUtils.initConversionTables(id) }
      if (app.cvUtils.showNotification) {
        app.cvUtils.showNotification=false
        app.showToastDEV(VM.viewModelScope, "CvModels are now ready!")
      }
    }
  }

}
