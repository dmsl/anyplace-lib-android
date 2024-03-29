package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw

import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.di.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.AnyplaceViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Space
import retrofit2.Response
import java.lang.Exception
import java.net.ConnectException

/**
 * Manages fetching of a [Space]
 */
class SpaceGetNW(
        private val app: AnyplaceApp,
        private val VMap: AnyplaceViewModel,
        private val RH: RetrofitHolderAP,
        private val repo: RepoAP) {
  val TG = "nw-space-get"

  /** Network Responses from API calls */
  // private val resp: MutableStateFlow<NetworkResult<UserLocations>> = MutableStateFlow(NetworkResult.Unset())

  private val C by lazy { CONST(app) }
  // private lateinit var smasUser : SmasUser

  /** Get [Space] */
  suspend fun blockingCall(buid: String) : Boolean {
    val MT = ::blockingCall.name
    LOG.D(TG, "$MT: blockingCall")

    if (app.hasInternet()) {
      return try {
        val response = repo.remote.getSpace(buid)
        LOG.D4(TG, "$MT: ${response.message()}" )
        handleResponse(response)
      } catch(ce: ConnectException) {
        val msg = "Connection failed:\n${RH.retrofit.baseUrl()}"
        LOG.E(TG, "$MT: $msg")
        false
      } catch(e: Exception) {
        LOG.E(TG, "$MT: ${e.message}")
        e.printStackTrace()
        false
      }
    } else {
      return false
    }
  }

  private fun handleResponse(resp: Response<Space>): Boolean {
    LOG.D3(TAG)
    if(resp.isSuccessful) {
      return when {
        resp.message().toString().contains("timeout") ->  {
            LOG.E(TG, "$TG: Timeout")
            false
        }
        resp.isSuccessful -> {
          if (resp.body() == null) {
            LOG.E(TG, "$TG: null space")
            return false
          }
          VMap.cache.storeJsonSpace(resp.body()!!)
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
