package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.nw

import android.widget.Toast
import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.UserCoordinates
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.android.SmasApp
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.SMAS
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.smas.models.*
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.helpers.ChatMsgHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.source.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.theme.WineRed
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlException
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasChatViewModel
import cy.ac.ucy.cs.anyplace.lib.smas.models.CONSTchatMsg.MDELIVERY_SAME_DECK
import cy.ac.ucy.cs.anyplace.lib.smas.models.CONSTchatMsg.prettyMDelivery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import retrofit2.Response
import java.lang.Exception

class MsgSendNW(private val app: SmasApp,
                private val VM: SmasChatViewModel,
                private val RH: RetrofitHolderSmas,
                private val repo: RepoSmas) {

  private val resp: MutableStateFlow<NetworkResult<MsgSendResp>> = MutableStateFlow(NetworkResult.Unset())

  val tag = "nw-smas-msg-send"

  private val C by lazy { SMAS(app.applicationContext) }
  private lateinit var smasUser: SmasUser
  private val err by lazy { SmasErrors(app, VM.viewModelScope) }

  suspend fun safeCall(userCoords: UserCoordinates, mdelivery: String, mtype: Int, msg: String?, mexten: String?) {
    LOG.D2(TAG_METHOD)
    resp.value = NetworkResult.Loading()
    smasUser = app.dsSmasUser.read.first()

    if (app.hasInternet()) {
      try {
        val req = MsgSendReq(smasUser, userCoords, mdelivery, msg, mtype, mexten, utlTime.epoch().toString())
        val content = if (ChatMsgHelper.isImage(mtype)) "<base64>" else msg
        LOG.D2(TAG, "$tag: Send: ${req.time}: mtype: ${mtype} msg: ${content} x,y: ${userCoords.lat},${userCoords.lon} deck: ${userCoords.level} ")
        val response = repo.remote.messagesSend(req)
        LOG.D2(TAG, "$tag: Resp: ${response.message()}")
        resp.value = handleResponse(response)
      } catch (e: Exception) {
        val errMsg = utlException.handleException(app, RH, VM.viewModelScope, e, tag)
        resp.value = NetworkResult.Error(errMsg)
      }
    } else {
      resp.value = NetworkResult.Error(C.ERR_MSG_NO_INTERNET)
    }
  }

  private fun handleResponse(response: Response<MsgSendResp>): NetworkResult<MsgSendResp> {
    LOG.V3(TAG, "HandleResponse:: MsgSend")
    if (response.isSuccessful) {
      when {
        response.message().toString().contains("timeout") -> return NetworkResult.Error("Timeout.")
        // response.body()!!.chatMsgs.isNullOrEmpty() -> return NetworkResult.Error("Can't get messages.")
        response.isSuccessful -> {

          // SMAS special handling (errors should not be 200/OK)
          val r = response.body()!!
          if (r.status == "err") {
            return NetworkResult.Error(r.descr)
          }

          LOG.D2(TAG, "$tag: Success. (pulling msgs)")
          app.pullMessagesONCE()

          return NetworkResult.Success(r)
        } // can be nullable
        else -> return NetworkResult.Error(response.message())
      }
    }
    return NetworkResult.Error("$TAG: ${response.message()}")
  }

  suspend fun collect() {
    resp.collect {
      when (it) {
        is NetworkResult.Loading -> {
          VM.isLoading = true
        }
        is NetworkResult.Success -> {
          LOG.D1(TAG, "MessageSend: ${it.data?.status}")
          VM.isLoading = false
          VM.clearReply()
          VM.clearTheReplyToMessage()
          val data = it.data!!
          val recipients =  data.deliveredTo
          var msg = when (recipients) {
            0 -> "No people reached."
            else -> "Sent to ${it.data?.deliveredTo} people."
          }

          if (data.level != null) msg+=" (${app.wSpace.prettyFloor}: ${data.level})"
          else if (data.mdelivery != MDELIVERY_SAME_DECK) {
            msg += " (${prettyMDelivery(data.mdelivery)})"
          }
          app.showSnackbarDEV(VM.viewModelScope, msg)
        }
        is NetworkResult.Error -> {
          LOG.D1(TAG, "$tag: Error: ${it.message}")
          VM.isLoading = false
          VM.errColor = WineRed
          app.showToast(VM.viewModelScope, "Message failed to send", Toast.LENGTH_SHORT)
        }
        else -> {
          // db error
          if (!err.handle(app, it.message, "$tag")) {
            val msg = it.message ?: "unspecified error"
            app.showToast(VM.viewModelScope, msg, Toast.LENGTH_SHORT)
            LOG.E(TAG, msg)
          }
        }
      }
    }
  }
}