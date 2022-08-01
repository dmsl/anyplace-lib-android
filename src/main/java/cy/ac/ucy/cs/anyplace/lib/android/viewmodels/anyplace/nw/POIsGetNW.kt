package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw

import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.cache.anyplace.Cache
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.di.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlException
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.ConnectionsResp
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.POIsResp
import retrofit2.Response
import java.lang.Exception

/**
 * Fetches the [ConnectionsResp] of a floor
 */
class POIsGetNW(
        private val app: AnyplaceApp,
        private val VM: CvViewModel,
        private val RH: RetrofitHolderAP,
        private val repo: RepoAP) {

  private val C by lazy { CONST(app.applicationContext) }
  private val cache by lazy { Cache(app.applicationContext) }

  val tag = "nw-ap-pois"

  suspend fun callBlocking(buid: String) : Boolean {
    LOG.W(TAG, "$tag: safecall")

    if (app.space!=null && cache.hasSpacePOIs(app.space!!)) return true

    if (app.hasInternet()) {
      try {
        val response = repo.remote.getSpacePOIsAll(buid)
        LOG.D4(TAG, "$tag: ${response.message()}" )

        when (val resp = handleResponse(response)) {
          is NetworkResult.Success -> {
            val wSpace = app.wSpace
            wSpace.cachePois(resp.data!!)

            return true
          }
          else -> {
            handleError("$tag: something went wrong: ${resp.message}")
          }
        }

      } catch(e: Exception) {
        val errMsg = utlException.handleException(app, RH, VM.viewModelScope, e, tag)
        LOG.E(TAG, "$tag: $errMsg")
      }
    } else {
      handleError(C.ERR_MSG_NO_INTERNET)
    }

    return false
  }

  private fun handleResponse(resp: Response<POIsResp>): NetworkResult<POIsResp> {
    LOG.D3(TAG)
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
      LOG.E(TAG, "$tag: $METHOD: unsuccessful")
    }

    return NetworkResult.Error("$TAG: $tag: ${resp.message()}")
  }

  private fun handleError(msg:String, e: Exception?=null) {
    if (e != null) LOG.E(TAG, "$tag: $msg", e)
    else LOG.E(TAG, "$tag: $msg")
  }
}
