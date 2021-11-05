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
import cy.ac.ucy.cs.anyplace.lib.android.data.datastore.*
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
  dataStoreUser: DataStoreUser,
  private val dataStoreMisc: DataStoreMisc,
  ): AndroidViewModel(app) {

  // PREFERENCES
  val serverPreferences = dataStoreServer.readServerPrefs

  //// RETROFIT
  ////// Mutable Data
  val versionResponse: MutableLiveData<NetworkResult<Version>> = MutableLiveData()

  var networkStatus = false
  /** normal var, filled by the observer (SelectSpaceActivity) */
  var backOnline = false
  // TODO:PM: bind this when connectivity status changes
  var readBackOnline = dataStoreMisc.readBackOnline.asLiveData()
  var readUserLoggedIn = dataStoreUser.readUser.asLiveData()

  var backFromSettings= false // INFO filled by the observer (collected from the fragment)
  var readBackFromSettings= dataStoreMisc.readBackFromSettings.asLiveData()

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
          versionResponse.value = NetworkResult.Error(e.message)
        }
      } catch(e: Exception) {
        LOG.E(TAG, "EXCEPTION: ${e.message}")
        exception = when (e) {
          is NullPointerException -> Exception(MSG_ERR_NPE)
          else -> e
        }
        versionResponse.value = NetworkResult.Error(exception?.message)
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
    viewModelScope.launch(Dispatchers.IO) {  dataStoreMisc.saveBackFromSettings(value) }
}
