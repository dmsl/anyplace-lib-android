package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw

import android.widget.Toast.*
import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.android.SmasApp
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.CHAT
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.DetectionModel
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.smas.models.ChatUser
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.source.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvMapViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.nw.SmasErrors
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.Response
import java.lang.Exception
import java.net.ConnectException

/**
 * Manages Location fetching of other users
 *
 * NOTE: this is from SMAS API.
 * But cv localization should have been Anyplace API.
 */
class CvFingerprintSendNW(
        private val app: SmasApp,
        private val VM: CvMapViewModel,
        private val RH: RetrofitHolderSmas,
        private val repo: RepoSmas) {

  private val err by lazy { SmasErrors(app, VM.viewModelScope) }

  /** Network Responses from API calls */
  private val resp: MutableStateFlow<NetworkResult<FingerprintSendResp>> = MutableStateFlow(NetworkResult.Unset())

  private val C by lazy { CHAT(app.applicationContext) }
  private lateinit var chatUser : ChatUser

  /** Send the [Chatuser]'s location (safecall) */
  suspend fun safeCall(userCoords: UserCoordinates,
                       detectionsReq: List<CvDetectionREQ>, model: DetectionModel
  ) {
    chatUser = app.dsChatUser.readUser.first()

    LOG.D2(TAG, "Session: ${chatUser.uid} ${chatUser.sessionkey}")

    resp.value = NetworkResult.Unset()
    resp.value = NetworkResult.Loading()
    if (app.hasInternet()) {
      try {
        val req= FingerprintSendReq(chatUser, userCoords, utlTime.epoch().toString(),
                detectionsReq, model.idSmas)
        LOG.D2(TAG, "FP-Send: ${req.time}: #: ${detectionsReq.size} coords: deck: ${req.deck}: x:${req.x} y:${req.y}")
        val response = repo.remote.cvFingerprintSend(req)
        LOG.D2(TAG, "FP-Send: Resp: ${response.message()}" )
        resp.value = handleResponse(response)
      } catch(ce: ConnectException) {
        val msg = "Connection failed:\n${RH.retrofit.baseUrl()}"
        handleException(msg, ce)
      } catch(e: Exception) {
        val msg = "$TAG: Not Found." + "\nURL: ${RH.retrofit.baseUrl()}"
        handleException(msg, e)
      }
    } else {
      resp.value = NetworkResult.Error(C.ERR_MSG_NO_INTERNET)
    }
  }

  private fun handleResponse(resp: Response<FingerprintSendResp>): NetworkResult<FingerprintSendResp> {
    LOG.D3(TAG, "handleResponse")
    if(resp.isSuccessful) {
      when {
        resp.message().toString().contains("timeout") -> return NetworkResult.Error("Timeout.")
        resp.isSuccessful -> {
          // SMAS special handling (errors should not be 200/OK)
          val r = resp.body()!!
          if (r.status == "err")  {
            return NetworkResult.Error(r.descr)
          }

          return NetworkResult.Success(r)
        } // can be nullable
        else -> return NetworkResult.Error(resp.message())
      }
    }
    return NetworkResult.Error("$TAG: ${resp.message()}")
  }

  private fun handleException(msg: String, e: Exception) {
    resp.value = NetworkResult.Error(msg)
    LOG.E(TAG, msg)
    LOG.E(TAG, e)
  }

  fun collect() {
    VM.viewModelScope.launch(Dispatchers.IO) {
      resp.collect {
        when (it)  {
          is NetworkResult.Success -> {
            val msg = "Uploaded Fingerprints (${it.data?.rows})"
            LOG.W(TAG, msg)
            app.showToast(VM.viewModelScope, msg, LENGTH_SHORT)
          }
          is NetworkResult.Error -> {
            if (!err.handle(app, it.message, "loc-send")) {
              val msg = it.message ?: "unspecified error"
              app.showToast(VM.viewModelScope, msg, LENGTH_SHORT)
            }
          }
          else -> {}
        }
      }
    }
  }

}
