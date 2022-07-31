package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.lifecycle.*
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.MiscDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.QuerySelectSpace
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceType
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.UserOwnership
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.ApUserDataStore
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.utils.net.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Space
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Spaces
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.Response
import java.lang.Exception
import java.net.ConnectException
import javax.inject.Inject

/**
 * Used as a reference / starting point..
 */
@HiltViewModel
class SpacesViewModel @Inject constructor(
        app: Application,
        private val repoAP: RepoAP,
        private val dsUserAP: ApUserDataStore,
        private val dsMisc: MiscDataStore,
        private val RFHap: RetrofitHolderAP) : AndroidViewModel(app) {

  /** This is outdated.. */
  val searchViewData: MutableLiveData<String> = MutableLiveData()
  private val C by lazy { CONST(app.applicationContext) }

  var loadedSpaces = false // TODO move in SpaceVM
  private var querySelectSpace = QuerySelectSpace()

  //// DATASTORE
  /**
   * Persistently saves the query (in Misc DataStore)
   */
  fun saveQueryTypeDataStore() =
    viewModelScope.launch(Dispatchers.IO) { dsMisc.saveQuerySpace(querySelectSpace) }

  fun resetQuery() =
    viewModelScope.launch(Dispatchers.IO) {
      dsMisc.saveQuerySpace(QuerySelectSpace())
    }

  /**
   * Saves the query in the ViewModel (not persistent).
   */
  fun saveQueryTypeTemp(userOwnership: UserOwnership, ownershipId: Int,
                        spaceType: SpaceType, spaceTypeId: Int) {
    LOG.D(TAG, "Saving query type: $userOwnership $spaceType")
    querySelectSpace= QuerySelectSpace(userOwnership, ownershipId, spaceType, spaceTypeId)
  }

  fun saveQueryTypeTemp(query : QuerySelectSpace) { querySelectSpace=query }

  private fun saveQuerySpaceName(newText: String) {
    querySelectSpace.spaceName = newText
    readSpacesQuery = repoAP.local.querySpaces(querySelectSpace)
    loadedSpaces=false
  }

  fun applyQuery(newText: String) = saveQuerySpaceName(newText)

  //// ROOM
  var readSpacesQuery: Flow<List<SpaceEntity>> = MutableStateFlow(emptyList())

  // will be collected when applying a space query
  var storedSpaceQuery = dsMisc.readQuerySpace

  /**
   * The [ownership] was supposed to provide some filtering here. Again, incomplete code.
   */
  private fun insertSpaces(spaces: Spaces, ownership: UserOwnership) =
    viewModelScope.launch(Dispatchers.IO) {
      LOG.D(TAG, "insertSpaces: total: " + spaces.spaces.size)
      spaces.spaces.forEach { space ->
        LOG.E(TAG, "INSERT SPACE: ${space.name}: $ownership")
        repoAP.local.insertSpace(space, ownership)
      }
    }

  //// RETROFIT
  ////// Mutable Data
  val spacesResponse: MutableLiveData<NetworkResult<Spaces>> = MutableLiveData()

  fun getSpaces() = viewModelScope.launch { getSpacesSafeCall() }

  /**
   * Hardcoded version:
   * - gets accessible spaces (owned or co-owned by user)
   * - and fetches also some UCY public buildings
   */
  private suspend fun getSpacesSafeCall() {
    spacesResponse.value = NetworkResult.Loading()
    if (app.hasInternet()) {
      try {
        // GET ACCESSIBLE AND PUBLIC SPACES, and merge them into a single result
        // for the public spaces, filter the ones containing 'ucy' (just for demo purposes)

        // ACCESSIBLE SPACES
        val apUser = dsUserAP.readUser.first()
        val respAccessible = repoAP.remote.getSpacesAccessible(apUser.accessToken)
        // PUBLIC SPACES
        val respPublic= repoAP.remote.getSpacesPublic()
        LOG.W(TAG, "Accessible Spaces: ${respAccessible.message()}" )
        LOG.W(TAG, "Public Spaces: ${respPublic.message()}" )

        val nwAccessible= handleSpacesResponse(respAccessible)
        val nwPublic= handleSpacesResponse(respPublic)

        if (nwAccessible is NetworkResult.Error || nwPublic is NetworkResult.Error) {
          spacesResponse.value = nwAccessible
          return
        }

        val listAccessible= handleSpacesResponse(respAccessible).data
        val listPublic= handleSpacesResponse(respPublic).data

        // keep 3-4 buildings, including the UCY CS building
        val ucySpaces = listPublic?.spaces?.filter { it.name.contains("ucy", true) }?.take(2)
        // hardcoded: ucy building
        val ucyCsBuilding = listPublic?.spaces?.filter { it.id=="username_1373876832005" }
        LOG.E(TAG, "SIZE: ${ucySpaces?.size}")

        // add [ucySpaces] to the accessible spaces
        // nwAccessible.data.spaces.add
        val demoSpaces = mutableListOf<Space>()
        ucyCsBuilding?.forEach { demoSpaces.add(it) }
        ucySpaces?.forEach { demoSpaces.add(it) }
        listAccessible?.spaces?.forEach { demoSpaces.add(it) }

        spacesResponse.value = NetworkResult.Success(Spaces(demoSpaces))

        val spaces = spacesResponse.value!!.data
        // INFO: FORCING THE ACCESSIBLE BUILDINGS
        if (spaces != null) { offlineCacheSpaces(spaces, UserOwnership.ACCESSIBLE) }
      } catch(ce: ConnectException) {
        val msg = "Connection failed:\n${RFHap.retrofit.baseUrl()}"
        handleSafecallError(msg, ce)
      } catch(e: Exception) {
        val msg = "Indoor Spaces Not Found." + "\nURL: ${RFHap.retrofit.baseUrl()}"
        handleSafecallError(msg, e)
      }
    } else {
      spacesResponse.value = NetworkResult.Error(C.ERR_MSG_NO_INTERNET)
    }
  }

  private fun offlineCacheSpaces(spaces: Spaces, ownership: UserOwnership) {
    LOG.D2(TAG, "offlineCacheSpaces: $ownership")
    insertSpaces(spaces, ownership)
  }

  private fun handleSpacesResponse(response: Response<Spaces>): NetworkResult<Spaces> {
    val tag = "Indoor Spaces"
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

  private fun handleSafecallError(msg:String, e: Exception) {
    spacesResponse.value = NetworkResult.Error(msg)
    LOG.E(TAG, msg)
    LOG.E(TAG, e)
  }

  private var runnedFirstedQuery = false

  fun runFirstQuery() {
    if (!runnedFirstedQuery) {
      LOG.D(TAG, "runFirstQuery: ${querySelectSpace.spaceType}")
      readSpacesQuery = repoAP.local.readSpaces()
    }
    runnedFirstedQuery = true
  }

}