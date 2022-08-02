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
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilErr
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
  private val scope by lazy { VM.viewModelScope }
  private val utlErr by lazy { UtilErr() }

  companion object {
    private val TG = "nw-location-send"
    val TEST_COORDS = LatLng(57.69579631991111, 11.913666007922222)
  }

  enum class Mode {
    normal,
    alert,
  }

  val notify = app.notify

  private val err by lazy { SmasErrors(app, scope) }

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
    val MT = ::safeCall.name
    smasUser = app.dsUserSmas.read.first()

    LOG.D4(TG, "$MT Session: ${smasUser.uid} ${smasUser.sessionkey}")

    resp.value = NetworkResult.Unset()
    resp.value = NetworkResult.Loading()
    if (app.hasInternet()) {
      try {
        LOG.V3(TG, MT)
        val req= LocationSendReq(smasUser, getAlertFlag(), userCoords, utlTime.epoch().toString())
        LOG.V2(TG, "$MT  ${req.time}: tp: ${mode.value} deck: ${req.level}: x:${req.x} y:${req.y}")
        val response = repo.remote.locationSend(req)
        LOG.D2(TG, "$MT: Resp: ${response.message()}" )
        resp.value = handleResponse(response)
      } catch(ce: ConnectException) {
        val msg = "Connection failed:\n${RH.retrofit.baseUrl()}"
        handleException(msg, ce)
      } catch(e: Exception) {
        val msg = "$TG: Not Found." + "\nURL: ${RH.retrofit.baseUrl()}"
        handleException(msg, e)
      }
    } else {
      resp.value = NetworkResult.Error(C.ERR_MSG_NO_INTERNET)
    }
  }

  private fun handleResponse(resp: Response<LocationSendResp>): NetworkResult<LocationSendResp> {
    val MT = ::handleResponse.name

    LOG.D2(TG, MT)
    if(resp.isSuccessful) {
      when {
        resp.message().toString().contains("timeout") -> return NetworkResult.Error("Timeout.")
        resp.isSuccessful -> {
          // SMAS special handling (errors should not be 200/OK)
          val r = resp.body()!!
          if (r.status == "err")  {
            LOG.E(TG, "Error: ${r.descr}")
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
    val MT = ::handleException.name
    resp.value = NetworkResult.Error(msg)
    LOG.E(TG, "$MT: msg")
    LOG.E(TG, MT, e)
  }

  // var errCnt = 0
  // var errNoInternetShown = false
  suspend fun collect() {
    val MT = ::collect.name

    resp.collect {
      when (it)  {
        is NetworkResult.Success -> {
          LOG.D4(TG, "$MT: LocationSend: ${it.data?.status}")
        }
        is NetworkResult.Error -> {
          if (!err.handle(app, it.message, "loc-send")) {
            val msg = it.message ?: "unspecified error"
            utlErr.handlePollingRequest(app, scope, msg)
          }
        }
        else -> {}
      }
    }
  }

}
