package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw

import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceOwnership
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.wrappers.SpaceWrapper.Companion.BUID_UCY_CS_BUILDING
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.ApUserDataStore
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.di.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.wrappers.SpaceWrapper.Companion.BUID_UCY_FST02
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.AnyplaceViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Space
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Spaces
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Response
import java.lang.Exception
import java.net.ConnectException

/**
 * Downloads a list of all [Spaces], so they can be rendered by the (SpaceSelector)
 * - that activity is: [SelectSpaceActivity]
 */
class SpacesGetNW(
        private val app: AnyplaceApp,
        private val VM: AnyplaceViewModel,
        private val RH: RetrofitHolderAP,
        private val dsUser: ApUserDataStore,
        private val repo: RepoAP) {
  private val TG = "nw-spaces-get"

  private val C by lazy { CONST(app) }

  val resp: MutableStateFlow<NetworkResult<Spaces>> = MutableStateFlow(NetworkResult.Unset())

  fun safeCall() = VM.viewModelScope.launch(Dispatchers.IO) { getSpaces() }

  /**
   * Download Spaces
   * DEMO MODE (For SMAS/LASH):
   * - gets accessible spaces (owned or co-owned by user)
   * - and fetches also some UCY public buildings
   * - see [storeSpaces]
   */
  private suspend fun getSpaces() {
    val MT = ::getSpaces.name
    resp.value = NetworkResult.Loading()
    if (app.hasInternet()) {
      try {
        val apUser = dsUser.read.first()

        // read spaces that are:
        // - owned by user, are accessible by user, or are public (order matters)
        // - we'll insert them to DB in that order, which will leave us with the correct ownership
        // a building that already exists, wont be added again
        val rSpacesUser= repo.remote.getSpacesUser(apUser.accessToken)
        val rSpacesAccessible = repo.remote.getSpacesAccessible(apUser.accessToken)
        val rSpacesPublic= repo.remote.getSpacesPublic()

        val nwOwned=handleSpacesResponse(rSpacesUser)
        val nwAccessible=handleSpacesResponse(rSpacesAccessible)
        val nwPublic=handleSpacesResponse(rSpacesPublic)

        // show error only if public spaces had issues. not the best handling..
        if (nwPublic is NetworkResult.Error) {
          resp.update { nwPublic }
          return
        }

        val listOwned= nwOwned.data
        val listAccessible= nwAccessible.data
        val listPublic= nwPublic.data

        LOG.W(TG, "$MT: spaces: ${listOwned?.spaces?.size} owned, ${listAccessible?.spaces?.size} accessible ${listPublic?.spaces?.size}")

        storeSpaces(listOwned, listAccessible, listPublic)
        resp.update { NetworkResult.Success(Spaces(emptyList())) }

      } catch(ce: ConnectException) {
        val msg = "Connection failed:\n${RH.retrofit.baseUrl()}"
        handleSafecallError(msg, ce)
      } catch(e: Exception) {
        val msg = "Spaces not found.."
        LOG.E(TG, "$MT: ${e.message}")
        LOG.E(TG, "$MT: ${e.cause}")
        LOG.E(TG, "$MT: ${e.localizedMessage}")
        LOG.E(TG, "$MT: ${e.stackTrace}")
        e.stackTrace.toString()
        handleSafecallError(msg, e)
      }
    } else {
      resp.update { NetworkResult.Error(C.ERR_MSG_NO_INTERNET) }
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
  private suspend fun storeSpaces(listOwned: Spaces?, listAccessible: Spaces?, listPublic: Spaces?) {
    val MT = ::storeSpaces.name
    LOG.E(TG, MT)
    repo.local.dropSpaces()

    // ADD SPACES: both to SQLite and to demo (to be shown in RecyclerView)
    // 1. ADD ALL OWNED SPACES:
    // both to SQLite and to demo (to be shown in RecyclerView)
    if (listOwned != null) {
      storeToDB(listOwned, SpaceOwnership.OWNED)
    }

    // 2. ADD ALL ACCESSIBLE SPACES:
    // both to SQLite and to demo (to be shown in RecyclerView)
    // NOTE: if a user is an admin, this will be the WHOLE list of spaces.
    if (listAccessible != null) {
      storeToDB(listAccessible, SpaceOwnership.ACCESSIBLE)
    }

    // 3. ADD PUBLIC SPACES
    // - for navigator: add all public
    // - for SMAS: add some select spaces (DEMO MODE)
    if (listPublic != null) {
      LOG.E(TG, "$MT: storing public spaces")

      val publicSpaces= if (app.isSMAS()) { // DEMO MODE FOR LASH
        // show only 2 selected public spaces
        val ucyCsBuilding = listPublic.spaces.filter { it.buid==BUID_UCY_CS_BUILDING}
        val ucyFst02Building = listPublic.spaces.filter { it.buid== BUID_UCY_FST02}

        // and put all those together
        val demoSpaceSelection = mutableListOf<Space>()
        demoSpaceSelection.addAll(ucyCsBuilding)
        demoSpaceSelection.addAll(ucyFst02Building)

        LOG.W(TG, "$MT: using select ${demoSpaceSelection.size} spaces (DEMO)")

        demoSpaceSelection
      } else {  // show all public spaces
        LOG.W(TG, "$MT: using all ${listPublic.spaces.size} spaces")
        listPublic.spaces
      }

      storeToDB(Spaces(publicSpaces.toList()), SpaceOwnership.PUBLIC)
    }

    // no need to store anything, as we will read spaces from DB
    // we do this, as we augment DB entries with [SpaceOwnership] info
    // We'll do this once we have collected a [NetworkResult.Success].
    LOG.V(TG, "$MT: updated NOW the NW RESP")
  }

  private fun handleSpacesResponse(response: Response<Spaces>): NetworkResult<Spaces> {
    val MT = ::handleSpacesResponse.name
    LOG.D2(TG, MT)
    if(response.isSuccessful) {
      return when {
        response.message().toString().contains("timeout") -> NetworkResult.Error("Timeout.")
        response.body()!!.spaces.isNullOrEmpty() -> NetworkResult.Error("$TG not found.")
        response.isSuccessful -> NetworkResult.Success(response.body()!!) // can be nullable
        else -> NetworkResult.Error(response.message())
      }
    }
    return NetworkResult.Error("$TG: ${response.message()}")
  }

  private suspend fun storeToDB(spaces: Spaces, ownership: SpaceOwnership) {
    val MT = ::storeToDB.name
    LOG.W(TG, "$MT: $ownership")
    insertSpaces(spaces, ownership)
  }

  /**
   * The [ownership] was supposed to provide some filtering here. Again, incomplete code.
   */
  private suspend fun insertSpaces(spaces: Spaces, ownership: SpaceOwnership)  {
    val MT=::insertSpaces.name
    LOG.D(TG, "$MT: total: " + spaces.spaces.size)
    spaces.spaces.forEach { space ->
      LOG.V(TG, "$MT: space: ${space.name}: $ownership: ${space.buid}")
      repo.local.insertSpace(space, ownership)
    }
  }

  private fun handleSafecallError(msg:String, e: Exception) {
    resp.update { NetworkResult.Error(msg) }
    LOG.E(TG, msg)
    LOG.E(TG, e)
  }

}
