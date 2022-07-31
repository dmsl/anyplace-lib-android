package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw

import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.UserOwnership
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.SpaceWrapper.Companion.BUID_UCY_CS_BUILDING
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.ApUserDataStore
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.utils.net.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.AnyplaceViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Space
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Spaces
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.Response
import java.lang.Exception
import java.net.ConnectException

/**
 * Downloads a list of all [Spaces], so they can be rendered
 * by the [SpaceSelector]
 */
class SpacesGetNW(
        private val app: AnyplaceApp,
        private val VM: AnyplaceViewModel,
        private val RH: RetrofitHolderAP,
        private val dsUser: ApUserDataStore,
        private val repo: RepoAP) {

  val tag = "nw-spaces-get"

  private val C by lazy { CONST(app.applicationContext) }

  val resp: MutableStateFlow<NetworkResult<Spaces>> = MutableStateFlow(NetworkResult.Unset())

  // CHECK CHANGE TO THIS: for spacesResponse
  fun safeCall() = VM.viewModelScope.launch(Dispatchers.IO) { getSpaces() }

  /**
   * Hardcoded version:
   * - gets accessible spaces (owned or co-owned by user)
   * - and fetches also some UCY public buildings
   */
  private suspend fun getSpaces() {
    resp.value = NetworkResult.Loading()
    if (app.hasInternet()) {
      try {
        // GET ACCESSIBLE AND PUBLIC SPACES, and merge them into a single result
        // for the public spaces, filter the ones containing 'ucy' (just for demo purposes)

        // ACCESSIBLE SPACES
        val apUser = dsUser.readUser.first()
        val respAccessible = repo.remote.getSpacesAccessible(apUser.accessToken)
        // PUBLIC SPACES
        val respPublic= repo.remote.getSpacesPublic()
        LOG.W(TAG, "Accessible Spaces: ${respAccessible.message()}" )
        LOG.W(TAG, "Public Spaces: ${respPublic.message()}" )

        val nwAccessible= handleSpacesResponse(respAccessible)
        val nwPublic= handleSpacesResponse(respPublic)

        if (nwAccessible is NetworkResult.Error || nwPublic is NetworkResult.Error) {
          resp.value = nwAccessible
          return
        }

        val listAccessible= handleSpacesResponse(respAccessible).data
        val listPublic= handleSpacesResponse(respPublic).data

        // keep 3-4 buildings, including the UCY CS building
        val ucySpaces = listPublic?.spaces?.filter {
          it.name.contains("ucy", true) }?.take(2)
        // hardcoded: cs@ucy building
        val ucyCsBuilding = listPublic?.spaces?.filter { it.id== BUID_UCY_CS_BUILDING}
        LOG.E(TAG, "SIZE: ${ucySpaces?.size}")

        // add [ucySpaces] to the accessible spaces
        // nwAccessible.data.spaces.add
        val demoSpaces = mutableListOf<Space>()
        ucyCsBuilding?.forEach { demoSpaces.add(it) }
        ucySpaces?.forEach { demoSpaces.add(it) }
        listAccessible?.spaces?.forEach { demoSpaces.add(it) }

        resp.value = NetworkResult.Success(Spaces(demoSpaces))

        val spaces = resp.value!!.data
        // INFO: FORCING THE ACCESSIBLE BUILDINGS
        // TODO: put: public... this... and that..............
        // TODO OWNERSHIP: put it earlier..
        if (spaces != null) { offlineCacheSpaces(spaces, UserOwnership.ACCESSIBLE) }
      } catch(ce: ConnectException) {
        val msg = "Connection failed:\n${RH.retrofit.baseUrl()}"
        handleSafecallError(msg, ce)
      } catch(e: Exception) {
        val msg = "Space not found." + "\nURL: ${RH.retrofit.baseUrl()}"
        handleSafecallError(msg, e)
      }
    } else {
      resp.value = NetworkResult.Error(C.ERR_MSG_NO_INTERNET)
    }
  }


  private fun handleSpacesResponse(response: Response<Spaces>): NetworkResult<Spaces> {
    LOG.D2(TAG, "handleSpacesResponse")
    if(response.isSuccessful) {
      return when {
        response.message().toString().contains("timeout") -> NetworkResult.Error("Timeout.")
        response.body()!!.spaces.isNullOrEmpty() -> NetworkResult.Error("$tag not found.")
        response.isSuccessful -> NetworkResult.Success(response.body()!!) // can be nullable
        else -> NetworkResult.Error(response.message())
      }
    }
    return NetworkResult.Error("$tag: ${response.message()}")
  }

  private fun offlineCacheSpaces(spaces: Spaces, ownership: UserOwnership) {
    LOG.D2(TAG, "$METHOD: $ownership")
    insertSpaces(spaces, ownership)
  }

  /**
   * The [ownership] was supposed to provide some filtering here. Again, incomplete code.
   */
  private fun insertSpaces(spaces: Spaces, ownership: UserOwnership)  {
    val method=METHOD
    VM.viewModelScope.launch(Dispatchers.IO) {
      LOG.D2(TAG, "$method: total: " + spaces.spaces.size)
      spaces.spaces.forEach { space ->
        LOG.W(TAG, "INSERT SPACE: ${space.name}: $ownership")
        repo.local.insertSpace(space, ownership)
      }
    }
  }

  private fun handleSafecallError(msg:String, e: Exception) {
    resp.value = NetworkResult.Error(msg)
    LOG.E(TAG, msg)
    LOG.E(TAG, e)
  }

}
