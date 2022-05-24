package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.nw

import android.widget.Toast
import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import cy.ac.ucy.cs.anyplace.smas.ChatUserAuth
import cy.ac.ucy.cs.anyplace.lib.android.SmasApp
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.CHAT
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.smas.models.*
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.source.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvMapViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasMainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import retrofit2.Response
import java.lang.Exception
import java.net.ConnectException

/**
 * Downloads CvModels from remote
 */
class CvModelsGetNW(
        private val app: SmasApp,
        private val VM: CvMapViewModel,
        private val RH: RetrofitHolderSmas,
        private val repo: RepoSmas) {

  private val err by lazy { SmasErrors(app, VM.viewModelScope) }

  /** Network Responses from API calls */
  private val resp: MutableStateFlow<NetworkResult<CvModelsResp>> = MutableStateFlow(NetworkResult.Unset())

  private val C by lazy { CHAT(app.applicationContext) }
  private lateinit var chatUser : ChatUser

  /** Get [UserLocations] SafeCall */
  suspend fun safeCall() {
    LOG.D2(TAG, "CvModelsGet")
    chatUser = app.dsChatUser.readUser.first()

    resp.value = NetworkResult.Loading()

    // TODO: if in DB: don't fetch them!

    if (app.hasInternet()) {
      try {
        val response = repo.remote.cvModelsGet(ChatUserAuth(chatUser))
        LOG.D4(TAG, "CvModelsGet: ${response.message()}" )
        resp.value = handleResponse(response)

        // TODO:PM Persist: put in cache & list (main mem) ?
        // val userLocations = resp.value.data
        // if (userLocations != null) { cache(useLocations, UserOwnership.PUBLIC) }
      } catch(ce: ConnectException) {
        val msg = "CvModelsGet: Connection failed:\n${RH.retrofit.baseUrl()}"
        handleException(msg, ce)
      } catch(e: Exception) {
        val msg = "$TAG: CvModelsGet: Not Found." + "\nURL: ${RH.retrofit.baseUrl()}"
        handleException(msg, e)
      }
    } else {
      resp.value = NetworkResult.Error(C.ERR_MSG_NO_INTERNET)
    }
  }

  private fun handleResponse(resp: Response<CvModelsResp>): NetworkResult<CvModelsResp> {
    LOG.D3(TAG)
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
      LOG.E(TAG, "handleResponse: unsuccessful")
    }

    return NetworkResult.Error("$TAG: ${resp.message()}")
  }

  private fun handleException(msg:String, e: Exception) {
    LOG.E(TAG, msg)
    LOG.E(TAG, e)
    resp.value = NetworkResult.Error(msg)
  }

  suspend fun collect() {
    LOG.D3()

    resp.collect {
      when (it)  {
        is NetworkResult.Success -> {
          val cvModels = it.data
          LOG.D2(TAG, "Got cv models: ${cvModels?.rows?.size}")
          // TODO persist them in DB
        }
        is NetworkResult.Error -> {
          LOG.D3(TAG, "Error: msg: ${it.message}")
          if (!err.handle(app, it.message, "loc-get")) {
            val msg = it.message ?: "unspecified error"
            app.showToast(VM.viewModelScope, msg, Toast.LENGTH_SHORT)
          }
        }
        else -> {
          //db error
          if (!err.handle(app, it.message, "msg-send")) {
            val msg = it.message ?: "unspecified error"
            app.showToast(VM.viewModelScope, msg, Toast.LENGTH_SHORT)
            LOG.E(TAG, msg)
          }
        }
      }
    }
  }
}
