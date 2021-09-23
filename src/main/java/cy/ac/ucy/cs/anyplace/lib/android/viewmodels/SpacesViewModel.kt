package cy.ac.ucy.cs.anyplace.lib.android.viewmodels

import android.app.Application
import androidx.lifecycle.*
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.Repository
import cy.ac.ucy.cs.anyplace.lib.android.data.datastore.DataStoreMisc
import cy.ac.ucy.cs.anyplace.lib.android.data.datastore.QuerySelectSpace
import cy.ac.ucy.cs.anyplace.lib.android.data.db.entities.SpaceEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.db.entities.SpaceType
import cy.ac.ucy.cs.anyplace.lib.android.data.db.entities.UserOwnership
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.utils.network.RetrofitHolder
import cy.ac.ucy.cs.anyplace.lib.models.Spaces
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
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
  private val repository: Repository,
  private val dataStoreMisc: DataStoreMisc,
  private val retrofitHolder: RetrofitHolder) : AndroidViewModel(app) {

  val searchViewData: MutableLiveData<String> = MutableLiveData()

  var loadedSpaces = false // TODO move in SpaceVM
  private var querySelectSpace = QuerySelectSpace()

  //// DATASTORE
  /**
   * Persistently saves the query (in Misc DataStore)
   */
  fun saveQueryTypeDataStore() =
    viewModelScope.launch(Dispatchers.IO) { dataStoreMisc.saveQuerySpace(querySelectSpace) }

  /**
   * Saves the query in the ViewModel (not persistent).
   */
  fun saveQueryTypeTemp(userOwnership: UserOwnership, ownershipId: Int,
                        spaceType: SpaceType, spaceTypeId: Int) {
    LOG.D(TAG, "Saving query type: $userOwnership $spaceType")
    querySelectSpace=QuerySelectSpace(userOwnership, ownershipId, spaceType, spaceTypeId)
  }

  private fun saveQuerySpaceName(newText: String) {
    querySelectSpace.spaceName = newText
    // CHECK: do NOT re initialize
    readSpacesQuery = repository.local.querySpaces(querySelectSpace).asLiveData() // TODO
    // TODO if we have previous results?!
    loadedSpaces=false // CHECK
  }

  fun applyQuery(newText: String) = saveQuerySpaceName(newText)

  //// ROOM
  val readSpaces: LiveData<List<SpaceEntity>> = repository.local.readSpaces().asLiveData()

  var readSpacesQuery: LiveData<List<SpaceEntity>> = repository.local.querySpaces(querySelectSpace).asLiveData()

  // will be collected when applying a space query
  var prefsQuerySpace = dataStoreMisc.readQuerySpace

  private fun insertSpaces(spaces: Spaces, ownership: UserOwnership) =
    viewModelScope.launch(Dispatchers.IO) {
      LOG.D(TAG, "insertSpaces: total: " + spaces.spaces.size)
      spaces.spaces.forEach { space ->
        repository.local.insertSpace(space, ownership)
      }
    }

  //// RETROFIT
  ////// Mutable Data
  val spacesResponse: MutableLiveData<NetworkResult<Spaces>> = MutableLiveData()

  fun getSpaces() = viewModelScope.launch { getSpacesSafeCall() }
  private suspend fun getSpacesSafeCall() {
    spacesResponse.value = NetworkResult.Loading()
    if (app.hasInternetConnection()) {
      try {
        val response = repository.remote.getSpacesPublic()
        LOG.D2(TAG, "Spaces msg: ${response.message()}" )
        spacesResponse.value = handleSpacesResponse(response)
        // LOG.E(TAG, "getSpaces: aft: handleSpacesResponse")

        val spaces = spacesResponse.value!!.data
        if (spaces != null) { offlineCacheSpaces(spaces, UserOwnership.PUBLIC) }
      } catch(ce: ConnectException) {
        val msg = "Connection failed:\n${retrofitHolder.retrofit.baseUrl()}"
        handleSafecallError(msg, ce)
      } catch(e: Exception) {
        val msg = "Indoor Spaces Not Found." + "\nURL: ${retrofitHolder.retrofit.baseUrl()}"
        handleSafecallError(msg, e)
      }
    } else {
      spacesResponse.value = NetworkResult.Error("No Internet Connection.")
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

}