package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw

import android.widget.Toast
import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.smas.ChatUserAuth
import cy.ac.ucy.cs.anyplace.lib.android.SmasApp
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.SMAS
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.smas.models.*
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.source.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlException
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
class CvMapGetNW(
        private val app: SmasApp,
        private val VM: CvViewModel,
        private val RH: RetrofitHolderSmas,
        private val repo: RepoSmas) {

  val tag = "CvMap"

  private val err by lazy { SmasErrors(app, VM.viewModelScope) }

  /** Network Responses from API calls */
  private val resp: MutableStateFlow<NetworkResult<CvMapResp>> = MutableStateFlow(NetworkResult.Unset())

  private val C by lazy { SMAS(app.applicationContext) }
  private lateinit var smasUser : SmasUser

  /** Get [UserLocations] SafeCall */
  suspend fun safeCall() {
    LOG.D2(TAG, "$METHOD: $tag")
    smasUser = app.dsChatUser.readUser.first()

    resp.value = NetworkResult.Loading()

    if (app.hasInternet()) {
      if (repo.local.hasCvMap()) {
        LOG.W(TAG, "$tag: dropping CvMap")
        repo.local.dropCvMap()
      }

      try {
        val response = repo.remote.cvMapGet(ChatUserAuth(smasUser))
        LOG.D2(TAG, "CvModelsGet: ${response.message()}" )
        resp.value = handleResponse(response)

        val cvMap = resp.value.data?.rows
        if (cvMap == null) {
          val msg = "Downloading $tag: no classes fetched"
          LOG.W(TAG, msg)
        } else {
          persistToDB(cvMap)
        }
        // CLR:PM
      // } catch(ce: ConnectException) {
      //   val msg = "$tag: Connection failed:\n${RH.retrofit.baseUrl()}"
      //   handleException(msg, ce)
      } catch(e: Exception) {
        // val msg = "$tag: Not Found." + "\nURL: ${RH.retrofit.baseUrl()}"
        // handleException(msg, e)
        val errMsg = utlException.handleException(app, RH, VM.viewModelScope, e, tag)
        resp.value = NetworkResult.Error(errMsg)
      }
    } else {
      resp.value = NetworkResult.Error(C.ERR_MSG_NO_INTERNET)
    }
  }

  private fun handleResponse(resp: Response<CvMapResp>): NetworkResult<CvMapResp> {
    LOG.D2(TAG)
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
      LOG.E(TAG, "$tag: handleResponse: unsuccessful")
    }

    return NetworkResult.Error("$TAG: ${resp.message()}")
  }

  var collectingCvMap = false
  suspend fun collect() {
    LOG.D2(TAG, "$METHOD: $tag")

    if (collectingCvMap) return
    collectingCvMap=true

    resp.collect {
      when (it)  {
        is NetworkResult.Success -> {
          val cvModels = it.data
          LOG.D2(TAG, "$tag: received: ${cvModels?.rows?.size} fingerprints")
        }
        is NetworkResult.Error -> {
          LOG.D3(TAG, "Error: msg: ${it.message}")
          if (!err.handle(app, it.message, "cvmap-get")) {
            val msg = it.message ?: "unspecified error"
            app.showToast(VM.viewModelScope, msg, Toast.LENGTH_SHORT)
          }
        }
        is NetworkResult.Unset -> { }
        else -> { } // might be unset or db loaded
      }
    }
  }

  // ROOM
  /**
   * Persists [ChatMsg]s to SQLite (through ROOM).
   * It does not store images (base64) to DB.
   * Those will be stored in [SmasCache] (file cache)
   */
  private fun persistToDB(obj: List<CvMapRow>) {
    LOG.W(TAG, "$METHOD: storing: ${obj.size} CvModel classes")
    VM.viewModelScope.launch(Dispatchers.IO) {
      obj.forEach {
        repo.local.insertCvMapRow(it)
      }
    }
  }
}
