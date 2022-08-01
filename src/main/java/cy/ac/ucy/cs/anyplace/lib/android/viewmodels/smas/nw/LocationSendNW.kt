package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.nw

import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.UserCoordinates
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.android.SmasApp
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.SMAS
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.smas.models.SmasUser
import cy.ac.ucy.cs.anyplace.lib.smas.models.LocationSendReq
import cy.ac.ucy.cs.anyplace.lib.smas.models.LocationSendResp
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.di.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasMainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import retrofit2.Response
import java.lang.Exception
import java.net.ConnectException

/**
 * Manages Location fetching of other users
 */
class LocationSendNW(
        private val app: SmasApp,
        private val VM: SmasMainViewModel,
        private val RH: RetrofitHolderSmas,
        private val repo: RepoSmas) {

  companion object {
    val TEST_COORDS = LatLng(57.69579631991111, 11.913666007922222)
  }

  enum class Mode {
    normal,
    alert,
  }

  val tag = "nw-location-send"

  private val err by lazy { SmasErrors(app, VM.viewModelScope) }

  /** Network Responses from API calls */
  private val resp: MutableStateFlow<NetworkResult<LocationSendResp>> = MutableStateFlow(NetworkResult.Unset())

  /** whether the user is issuing an alert or not */
  val mode: MutableStateFlow<Mode> = MutableStateFlow(Mode.normal)

  fun alerting() = mode.value == Mode.alert

  private fun getAlertFlag(): Int {
    return when (mode.value) {
      Mode.alert -> 1
      Mode.normal -> 0
    }
  }

  private val C by lazy { SMAS(app.applicationContext) }
  private lateinit var smasUser : SmasUser

  /** Send the [Chatuser]'s location (safecall) */
  suspend fun safeCall(userCoords: UserCoordinates) {
    smasUser = app.dsUserSmas.read.first()

    LOG.D4(TAG, "Session: ${smasUser.uid} ${smasUser.sessionkey}")

    resp.value = NetworkResult.Unset()
    resp.value = NetworkResult.Loading()
    if (app.hasInternet()) {
      try {
        LOG.V3(TAG, "LOC SEND")
        val req= LocationSendReq(smasUser, getAlertFlag(), userCoords, utlTime.epoch().toString())
        LOG.V2(TAG, "LocSend: ${req.time}: tp: ${mode.value} deck: ${req.deck}: x:${req.x} y:${req.y}")
        val response = repo.remote.locationSend(req)
        LOG.D2(TAG, "LocationSend: Resp: ${response.message()}" )
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

  private fun handleResponse(resp: Response<LocationSendResp>): NetworkResult<LocationSendResp> {
    LOG.D2(TAG, "handleResponse")
    if(resp.isSuccessful) {
      when {
        resp.message().toString().contains("timeout") -> return NetworkResult.Error("Timeout.")
        resp.isSuccessful -> {
          // SMAS special handling (errors should not be 200/OK)
          val r = resp.body()!!
          if (r.status == "err")  {
            LOG.E(TAG, "Error: ${r.descr}")
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
    LOG.W(TAG, "Handle Exception")
    resp.value = NetworkResult.Error(msg)
    LOG.E(TAG, "ERROR HERE")
    LOG.E(TAG, msg)
    LOG.E(TAG, e)
  }

  var errCnt = 0
  var errNoInternetShown = false
  suspend fun collect() {
    resp.collect {
      when (it)  {
        is NetworkResult.Success -> {
          LOG.D4(TAG, "LocationSend: ${it.data?.status}")
        }
        is NetworkResult.Error -> {
          if (!err.handle(app, it.message, "loc-send")) {
            val msg = it.message ?: "unspecified error"

            // show just one internet related msg
            if (msg != C.MSG_ERR_ILLEGAL_STATE) errNoInternetShown=true
            errCnt+=1
            if((errCnt < C.MAX_ERR_MSGS && msg != C.ERR_MSG_NO_INTERNET)
                      || !errNoInternetShown ) {

              app.snackbarShort(VM.viewModelScope, msg)
              LOG.W(TAG, "$tag: $msg")
            } else {
              LOG.E(TAG, "$tag: [SUPPRESSING ERR MSGS]")
              LOG.E(TAG, "$tag: $msg")
            }

          }
        }
        else -> {}
      }
    }
  }

}
