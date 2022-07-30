package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace

import android.app.Application
import androidx.lifecycle.*
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.MiscDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.QuerySelectSpace
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceType
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.UserOwnership
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.utils.net.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Spaces
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
import java.lang.Exception
import java.net.ConnectException
import javax.inject.Inject

@HiltViewModel
class SpacesViewModel @Inject constructor(
        app: Application,
        private val repoAP: RepoAP,
        private val miscDataStore: MiscDataStore,
        private val retrofitHolderAP: RetrofitHolderAP) : AndroidViewModel(app) {

  val searchViewData: MutableLiveData<String> = MutableLiveData()

  private val C by lazy { CONST(app.applicationContext) }

  var loadedSpaces = false // TODO move in SpaceVM
  private var querySelectSpace = QuerySelectSpace()

  //// DATASTORE
  /**
   * Persistently saves the query (in Misc DataStore)
   */
  fun saveQueryTypeDataStore() =
    viewModelScope.launch(Dispatchers.IO) { miscDataStore.saveQuerySpace(querySelectSpace) }

  fun resetQuery() =
    viewModelScope.launch(Dispatchers.IO) {
      miscDataStore.saveQuerySpace(QuerySelectSpace())
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
    readSpacesQuery = repoAP.local.querySpaces(querySelectSpace).asLiveData()
    loadedSpaces=false
  }

  fun applyQuery(newText: String) = saveQuerySpaceName(newText)

  //// ROOM
  // var readSpacesQuery: LiveData<List<SpaceEntity>> = apply { emptyList<SpaceEntity>() }
  var readSpacesQuery: LiveData<List<SpaceEntity>> = MutableLiveData()

  // will be collected when applying a space query
  var storedSpaceQuery = miscDataStore.readQuerySpace

  private fun insertSpaces(spaces: Spaces, ownership: UserOwnership) =
    viewModelScope.launch(Dispatchers.IO) {
      LOG.D(TAG, "insertSpaces: total: " + spaces.spaces.size)
      spaces.spaces.forEach { space ->
        repoAP.local.insertSpace(space, ownership)
      }
    }

  //// RETROFIT
  ////// Mutable Data
  val spacesResponse: MutableLiveData<NetworkResult<Spaces>> = MutableLiveData()

  fun getSpaces() = viewModelScope.launch { getSpacesSafeCall() }
  private suspend fun getSpacesSafeCall() {
    spacesResponse.value = NetworkResult.Loading()
    if (app.hasInternet()) {
      try {
        val response = repoAP.remote.getSpacesPublic()
        LOG.D2(TAG, "Spaces msg: ${response.message()}" )
        spacesResponse.value = handleSpacesResponse(response)
        // LOG.E(TAG, "getSpaces: aft: handleSpacesResponse")

        val spaces = spacesResponse.value!!.data
        if (spaces != null) { offlineCacheSpaces(spaces, UserOwnership.PUBLIC) }
      } catch(ce: ConnectException) {
        val msg = "Connection failed:\n${retrofitHolderAP.retrofit.baseUrl()}"
        handleSafecallError(msg, ce)
      } catch(e: Exception) {
        val msg = "Indoor Spaces Not Found." + "\nURL: ${retrofitHolderAP.retrofit.baseUrl()}"
        handleSafecallError(msg, e)
      }
    } else {
      spacesResponse.value = NetworkResult.Error(C.ERR_MSG_NO_INTERNET)
    }
  }

  private fun offlineCacheSpaces(spaces: Spaces, userOwnership: UserOwnership) {
    LOG.D2(TAG, "offlineCacheSpaces: $userOwnership")
    insertSpaces(spaces, userOwnership)
  }

  private fun handleSpacesResponse(response: Response<Spaces>): NetworkResult<Spaces>? {
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

  private var firstQuery = false

  fun runFirstQuery() {
    if (!firstQuery) {
      LOG.D(TAG, "runFirstQuery: ${querySelectSpace.spaceType}")
      readSpacesQuery = repoAP.local.querySpaces(querySelectSpace).asLiveData()
    }
    firstQuery = true
  }

}