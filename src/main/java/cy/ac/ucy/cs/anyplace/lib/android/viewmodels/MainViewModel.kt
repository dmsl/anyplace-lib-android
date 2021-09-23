package cy.ac.ucy.cs.anyplace.lib.android.viewmodels

import android.app.Application
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.Toast
import androidx.lifecycle.*
import androidx.preference.Preference
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.EXCEPTION_MSG_HTTP_FORBIDEN
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.MSG_ERR_NPE
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.MSG_ERR_ONLY_SSL
import cy.ac.ucy.cs.anyplace.lib.android.data.Repository
import cy.ac.ucy.cs.anyplace.lib.android.data.datastore.DataStoreMisc
import cy.ac.ucy.cs.anyplace.lib.android.data.datastore.DataStoreServer
import cy.ac.ucy.cs.anyplace.lib.android.data.datastore.DataStoreUser
import cy.ac.ucy.cs.anyplace.lib.android.data.datastore.QuerySelectSpace
import cy.ac.ucy.cs.anyplace.lib.android.data.db.entities.SpaceEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.db.entities.SpaceType
import cy.ac.ucy.cs.anyplace.lib.android.data.db.entities.UserOwnership
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.utils.AnyplaceUtils
import cy.ac.ucy.cs.anyplace.lib.android.utils.GenUtils
import cy.ac.ucy.cs.anyplace.lib.android.utils.network.RetrofitHolder
import cy.ac.ucy.cs.anyplace.lib.models.Spaces
import cy.ac.ucy.cs.anyplace.lib.models.Version
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
import java.lang.Exception
import java.lang.NullPointerException
import java.net.ConnectException
import java.net.UnknownServiceException
import javax.inject.Inject

// TODO: PM SEPARATE CORE APP (MainViewModel) with something specific
//  (e.g. SpaceViewModel, Login)

@HiltViewModel
class MainViewModel @Inject constructor(
  app: Application,
  private val repository: Repository,
  private val retrofitHolder: RetrofitHolder,
  dataStoreServer: DataStoreServer,
  private val dataStoreMisc: DataStoreMisc,
  private val dataStoreUser: DataStoreUser,
  ): AndroidViewModel(app) {
  var loadedApiData = false // TODO move in SpaceVM

  private lateinit var spacesQueryType: QuerySelectSpace

  // PREFERENCES
  val serverPreferences = dataStoreServer.readServerPrefs

  //// ROOM
  val readSpaces: LiveData<List<SpaceEntity>> = repository.local.readSpaces().asLiveData()
  val querySpaces: LiveData<List<SpaceEntity>> = repository.local.querySpaces().asLiveData()

  // will be collected when applying a space query
  val readSpaceQueryType = dataStoreMisc.readQuerySpace

  private fun insertSpaces(spaces: Spaces, ownership: UserOwnership) =
    viewModelScope.launch(Dispatchers.IO) {
      LOG.D(TAG, "insertSpaces: total: " + spaces.spaces.size)
      spaces.spaces.forEach { space ->
        repository.local.insertSpace(space, ownership)
      }
    }

  fun applyQueries() : Map<String, String> {
    TODO("Apply ROOM queries")
  }

  //// RETROFIT
  ////// Mutable Data
  val spacesResponse: MutableLiveData<NetworkResult<Spaces>> = MutableLiveData()
  val versionResponse: MutableLiveData<NetworkResult<Version>> = MutableLiveData()

  var networkStatus = false
  /** normal var, filled by the observer (SelectSpaceActivity) */
  var backOnline = false
  var readBackOnline = dataStoreMisc.readBackOnline.asLiveData()

  var readUserLoggedIn = dataStoreUser.readUser.asLiveData()

  var backFromSettings= false // INFO filled by the observer (collected from the fragment)
  var readBackFromSettings= dataStoreMisc.readBackFromSettings.asLiveData()

  fun getSpaces() = viewModelScope.launch { getSpacesSafeCall() }
  fun displayBackendVersion(versionPreferences: Preference?) =
    viewModelScope.launch { displayBackendVersionSafeCall(versionPreferences) }

  private suspend fun displayBackendVersionSafeCall(versionPreferences: Preference?) {
    LOG.D4(TAG, "getVersionSafeCall: ${retrofitHolder.baseURL}")
    versionPreferences?.summary = "reaching server .."

    var msg = ""
    var exception : Exception? = null
    var versionColor : ForegroundColorSpan? = null
    
    if (app.hasInternetConnection()) {
      try {
        val response = repository.remote.getVersion()
        versionResponse.value = handleVersionResponse(response)
        val version = versionResponse.value!!.data
        if (version != null) {
          val prettyVersion = AnyplaceUtils.prettyVersion(version)
          msg = "$prettyVersion (connected: ${GenUtils.prettyTime()})"
          versionPreferences?.icon = null
        } else {
          exception = Exception("Failed to get version.")
        }
      } catch(e: UnknownServiceException) {
        LOG.E(TAG, "EXCEPTION: ${e.message}")
        exception = e
        e.let {
          if (e.message?.contains(EXCEPTION_MSG_HTTP_FORBIDEN) == true) {
            exception = Exception(MSG_ERR_ONLY_SSL)
          }
          spacesResponse.value = NetworkResult.Error(e.message)
        }
      } catch(e: Exception) {
        LOG.E(TAG, "EXCEPTION: ${e.message}")
        exception = when (e) {
          is NullPointerException -> Exception(MSG_ERR_NPE)
          else -> e
        }
        spacesResponse.value = NetworkResult.Error(exception?.message)
      }
    } else {
      exception = Exception("No internet connection.")
    }
    exception?.let { it ->
      msg = it.message.toString()
      versionPreferences?.setIcon(R.drawable.ic_sad)
      LOG.E(msg)
      LOG.E(it)

      versionColor = ForegroundColorSpan(app.getColor(R.color.redDark))
    } ?: run {
      versionPreferences?.setIcon(R.drawable.ic_happy)
    }

    val spannableMsg = SpannableString(msg)
    versionColor?.let { spannableMsg.setSpan(versionColor, 0, spannableMsg.length, 0) }
    versionPreferences?.summary = spannableMsg
  }

  // TODO modify this call for u
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

  private fun offlineCacheSpaces(spaces: Spaces, ownership: UserOwnership) {
    LOG.D2("offlineCacheSpaces: $ownership")
    insertSpaces(spaces, ownership)
  }

  private fun handleSafecallError(msg:String, e: Exception) {
    spacesResponse.value = NetworkResult.Error(msg)
    LOG.E(msg)
    LOG.E(TAG, e)
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

  private fun handleVersionResponse(response: Response<Version>): NetworkResult<Version>? {
    return when {
      response.message().toString().contains("timeout") -> NetworkResult.Error("Timeout.")
      response.body()!!.version.isEmpty() -> NetworkResult.Error("Version not found.")
      response.isSuccessful -> NetworkResult.Success(response.body()!!) // can be nullable
      else -> NetworkResult.Error("Cannot reach server.")
    }
  }

  fun showNetworkStatus() {
    if (!networkStatus) {
      Toast.makeText(getApplication(), "No internet connection!", Toast.LENGTH_SHORT).show()
      saveBackOnline(true)
    } else if(networkStatus && backOnline)  {
      Toast.makeText(getApplication(), "Back online!", Toast.LENGTH_SHORT).show()
      saveBackOnline(false)
    }
  }

  private fun saveBackOnline(value: Boolean) =
    viewModelScope.launch(Dispatchers.IO) {
      dataStoreMisc.saveBackOnline(value)
    }

  fun setBackFromSettings() = saveBackFromSettings(true)
  fun unsetBackFromSettings() = saveBackFromSettings(false)

  private fun saveBackFromSettings(value: Boolean) =
    viewModelScope.launch(Dispatchers.IO) {
      dataStoreMisc.saveBackFromSettings(value)
    }


  /**
   * Persistently saves the query (in Misc DataStore)
   */
  fun saveQueryTypeDataStore() =
    viewModelScope.launch(Dispatchers.IO) {
      dataStoreMisc.saveQuerySpace(spacesQueryType)
    }

  /**
   * Saves the query in the ViewModel (not persistent).
   */
  fun saveQueryTypeTemp(userOwnership: UserOwnership, ownershipId: Int,
                        spaceType: SpaceType, spaceTypeId: Int) {
    LOG.D(TAG, "Saving query type: $userOwnership $spaceType")
    spacesQueryType = QuerySelectSpace(userOwnership, ownershipId, spaceType, spaceTypeId)
  }

}
