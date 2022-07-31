package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw

import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceOwnership
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
          /*

           */

        // ACCESSIBLE SPACES
        val apUser = dsUser.readUser.first()

        // read spaces that are:
        // - owned by user, are accessible by user, or are public (order matters)
        // - we'll insert them to DB in that order, which will leave us with the correct ownership
        // a building that already exists, wont be added again
        val rSpacesUser= repo.remote.getSpacesUser(apUser.accessToken)
        val rSpacesAccessible = repo.remote.getSpacesAccessible(apUser.accessToken)
        val rSpacesPublic= repo.remote.getSpacesPublic()

        LOG.W(TAG, "Owned Spaces: ${rSpacesAccessible.message()}" )
        LOG.W(TAG, "Accessible Spaces: ${rSpacesAccessible.message()}" )
        LOG.W(TAG, "Public Spaces: ${rSpacesPublic.message()}" )

        val nwOwned= handleSpacesResponse(rSpacesUser)
        val nwAccessible= handleSpacesResponse(rSpacesAccessible)
        val nwPublic= handleSpacesResponse(rSpacesPublic)

        // show error only if public spaces had issues. not the best handling..
        if (nwPublic is NetworkResult.Error) {
          resp.value = nwAccessible
          return
        }

        val listOwned= handleSpacesResponse(rSpacesUser).data
        val listAccessible= handleSpacesResponse(rSpacesAccessible).data
        val listPublic= handleSpacesResponse(rSpacesPublic).data

        storeSpaces(listOwned, listAccessible, listPublic)

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

  /**
   * Does some pre-processing (some of it hardcoded for demo purposes),
   * and then it saves spaces to DB
   *
   * 0. first it clears spaces the spaces
   * 1. then it fetches user own spaces
   * 2. then user accessible spaces (owned + co-owned)
   * 3. then public spaces
   *
   * SQLite ensures that we have no duplicates.
   * Because they are added in that particular order, the correct ownership is logged
   */
  private fun storeSpaces(listOwned: Spaces?, listAccessible: Spaces?, listPublic: Spaces?) {

    repo.local.dropSpaces()

    val demoSpaces = mutableListOf<Space>()

    // ADD SPACES: both to SQLite and to demo (to be shown in RecyclerView)
    // 1. ADD ALL OWNED SPACES:
    // both to SQLite and to demo (to be shown in RecyclerView)
    if (listOwned != null) {
      offlineCacheSpaces(listOwned, SpaceOwnership.OWNED)
      demoSpaces.addAll(listOwned.spaces)
    }

    // 2. ADD ALL ACCESSIBLE SPACES:
    // both to SQLite and to demo (to be shown in RecyclerView)
    // NOTE: if a user is an admin, this will be the WHOLE list of spaces.
    if (listAccessible != null) {
      offlineCacheSpaces(listAccessible, SpaceOwnership.ACCESSIBLE)
      demoSpaces.addAll(listAccessible.spaces)
    }

    // 3. ADD A SMALL SAMPLE OF PUBLIC SPACES
    // - For demo purposes:
    //   - UCY CS building
    //   - 2 more spaces
    if (listPublic != null) {
      // val ucyPublicSpaces = listPublic.spaces
      val ucyPublicSpaces = listPublic.spaces.filter {
        it.name.contains("ucy", true) }.take(2)
      val ucyCsBuilding = listPublic.spaces.filter { it.id==BUID_UCY_CS_BUILDING}

      val ucySelectSpaces = mutableListOf<Space>()
      ucySelectSpaces.addAll(ucyCsBuilding)
      ucySelectSpaces.addAll(ucyPublicSpaces)
      offlineCacheSpaces(Spaces(ucySelectSpaces), SpaceOwnership.PUBLIC)

      demoSpaces.addAll(ucySelectSpaces)
    }
    resp.value = NetworkResult.Success(Spaces(demoSpaces))
  }

  private fun handleSpacesResponse(response: Response<Spaces>): NetworkResult<Spaces> {
    LOG.D2()
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

  private fun offlineCacheSpaces(spaces: Spaces, ownership: SpaceOwnership) {
    LOG.D2(TAG, "$METHOD: $ownership")
    insertSpaces(spaces, ownership)
  }

  /**
   * The [ownership] was supposed to provide some filtering here. Again, incomplete code.
   */
  private fun insertSpaces(spaces: Spaces, ownership: SpaceOwnership)  {
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
