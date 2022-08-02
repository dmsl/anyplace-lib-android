package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw

import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.cache.anyplace.Cache
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.di.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilErr
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.ConnectionsResp
import retrofit2.Response
import java.lang.Exception

/**
 * Fetches the [ConnectionsResp] of a floor
 */
class ConnectionsGetNW(
        private val app: AnyplaceApp,
        private val VM: CvViewModel,
        private val RH: RetrofitHolderAP,
        private val repo: RepoAP) {

  private val C by lazy { CONST(app.applicationContext) }
  private val cache by lazy { Cache(app.applicationContext) }
  private val TG = "nw-ap-conn"
  private val utlErr by lazy { UtilErr() }


  suspend fun callBlocking(buid: String) : Boolean {
    val MT = ::callBlocking.name
    LOG.W(TG, MT)

    if (app.space!=null && cache.hasSpaceConnections(app.space!!)) return true

    if (app.hasInternet()) {
      try {
        val response = repo.remote.getSpaceConnectionsAll(buid)
        LOG.D4(TG, "$MT: ${response.message()}" )

        when (val resp = handleResponse(response)) {
          is NetworkResult.Success -> {
            val wSpace = app.wSpace
            wSpace.cacheConnections(resp.data!!)
            return true
          }
          else -> {
            handleError("$MT: something went wrong: ${resp.message}")
          }
        }
      } catch(e: Exception) {
        // ignoring the msg resp..
        val msg = utlErr.handle(app, RH, VM.viewModelScope, e, TG)
      }
    } else {
      handleError(C.ERR_MSG_NO_INTERNET)
    }
    return false
  }

  private fun handleResponse(resp: Response<ConnectionsResp>): NetworkResult<ConnectionsResp> {
    val MT = ::handleResponse.name
    LOG.D3(TG, MT)
    if(resp.isSuccessful) {
      return when {
        resp.message().toString().contains("timeout") -> NetworkResult.Error("Timeout.")
        resp.isSuccessful -> {
          val r = resp.body()!!
          return NetworkResult.Success(r)
        } // can be nullable
        else -> NetworkResult.Error(resp.message())
      }
    } else {
      LOG.E(TG, "$MT: handleResponse: unsuccessful")
    }

    return NetworkResult.Error("$TG: $MT: ${resp.message()}")
  }

  private fun handleError(msg:String, e: Exception?=null) {
    val MT = ::handleError.name
    if (e != null) LOG.E(TG, "$MT: $msg", e)
    else LOG.E(TG, "$MT: $msg")
  }
}
