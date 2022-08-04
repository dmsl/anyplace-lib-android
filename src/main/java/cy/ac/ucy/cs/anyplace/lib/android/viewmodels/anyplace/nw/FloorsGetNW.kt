package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw

import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.di.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.AnyplaceViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Levels
import retrofit2.Response
import java.lang.Exception
import java.net.ConnectException

/**
 * Manages fetching of [Levels]
 */
class FloorsGetNW(
        private val app: AnyplaceApp,
        private val VMap: AnyplaceViewModel,
        private val RH: RetrofitHolderAP,
        private val repo: RepoAP) {
  val TG = "nw-floors-get"

  private val C by lazy { CONST(app) }

  /** Get [Levels]*/
  suspend fun blockingCall(buid: String) : Boolean {
    val MT = ::blockingCall.name
    LOG.E(TG, MT)

    if (app.hasInternet()) {
      return try {
        val response = repo.remote.getFloors(buid)
        LOG.D4(TG, "$TG: ${response.message()}" )
        handleResponse(response)
      } catch(ce: ConnectException) {
        val msg = "Connection failed:\n${RH.retrofit.baseUrl()}"
        LOG.E(TG, "$TG: $msg")
        false
      } catch(e: Exception) {
        LOG.E(TG, "$TG: ${e.message}")
        e.printStackTrace()
        false
      }
    } else {
      return false
    }
  }

  private fun handleResponse(resp: Response<Levels>): Boolean {
    val MT = ::handleResponse.name
    LOG.D3(TG, MT)
    if(resp.isSuccessful) {
      return when {
        resp.message().toString().contains("timeout") ->  {
            LOG.E(TG, "$TG: Timeout")
            false
        }
        resp.isSuccessful -> {
          if (resp.body() == null) {
            LOG.E(TG, "$TG: null floors")
            return false
          }
          VMap.cache.storeJsonFloors(resp.body()!!)
          true
        } // can be nullable
        else ->  {
          LOG.E(TG, "$TG: ${resp.message()}")
          false
        }
      }
    }
    LOG.E(TG, "$TG: ${resp.message()}")
    return false
  }

}
