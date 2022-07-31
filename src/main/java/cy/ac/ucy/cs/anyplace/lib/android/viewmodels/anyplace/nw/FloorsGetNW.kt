package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw

import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.utils.net.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.AnyplaceViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Floors
import retrofit2.Response
import java.lang.Exception
import java.net.ConnectException

/**
 * Manages Location fetching of [Floors]
 */
class FloorsGetNW(
        private val app: AnyplaceApp,
        private val VMap: AnyplaceViewModel,
        private val RH: RetrofitHolderAP,
        private val repo: RepoAP) {

  val tag = "nw-floors-get"

  /** Network Responses from API calls */
  // private val resp: MutableStateFlow<NetworkResult<UserLocations>> = MutableStateFlow(NetworkResult.Unset())

  private val C by lazy { CONST(app.applicationContext) }

  /** Get [Floors]*/
  suspend fun blockingCall(buid: String) : Boolean {
    LOG.E(TAG, "$tag: blockingCall")

    if (app.hasInternet()) {
      try {
        val response = repo.remote.getFloors(buid)
        LOG.D4(TAG, "$tag: ${response.message()}" )
         return handleResponse(response)
      } catch(ce: ConnectException) {
        val msg = "Connection failed:\n${RH.retrofit.baseUrl()}"
        LOG.E(TAG, "$tag: $msg")
        return false
      } catch(e: Exception) {
        LOG.E(TAG, "$tag: ${e.message}")
        e.printStackTrace()
        return false
      }
    } else {
      return false
    }
  }

  private fun handleResponse(resp: Response<Floors>): Boolean {
    LOG.D3(TAG)
    if(resp.isSuccessful) {
      return when {
        resp.message().toString().contains("timeout") ->  {
            LOG.E(TAG, "$tag: Timeout")
            false
        }
        resp.isSuccessful -> {
          if (resp.body() == null) {
            LOG.E(TAG, "$tag: null floors")
            return false
          }
          VMap.cache.storeJsonFloors(resp.body()!!)
          true
        } // can be nullable
        else ->  {
          LOG.E(TAG, "$tag: ${resp.message()}")
          false
        }
      }
    }
    LOG.E(TAG, "$tag: ${resp.message()}")
    return false
  }

}
