package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw

import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.Preference
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.di.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlAP
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.AnyplaceViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Version
import kotlinx.coroutines.launch
import retrofit2.Response
import java.lang.Exception
import java.lang.NullPointerException
import java.net.UnknownServiceException

/**
 * Fetches the Anyplace Version
 */
class VersionApNW(
        private val app: AnyplaceApp,
        VM: AnyplaceViewModel,
        private val RH: RetrofitHolderAP,
        private val repo: RepoAP) {

  val tag = "nw-ap-version-get"
  private val scope = VM.viewModelScope

  private val C by lazy { CONST(app) }
  val resp: MutableLiveData<NetworkResult<Version>> = MutableLiveData()

  fun displayVersion(versionPreferences: Preference?) =
          scope.launch { display(versionPreferences) }

  /**
   * Displaying the anyplace backend version based on the response
   */
  private suspend fun display(versionPreferences: Preference?) {
    LOG.D4(TAG, "$METHOD: ${RH.baseURL}")
    versionPreferences?.summary = "reaching Anyplace .."

    var msg = ""
    var exception : Exception? = null
    var versionColor : ForegroundColorSpan? = null

    if (app.hasInternet()) {
      try {
        val response = repo.remote.getVersion()
        resp.value = handleResponse(response)
        val version = resp.value!!.data
        if (version != null) {
          val prettyVersion = utlAP.prettyVersion(version)
          msg = "$prettyVersion (connected: ${utlTime.currentTimePretty()})"
          versionPreferences?.icon = null
        } else {
          exception = Exception("Failed to get version.")
        }
      } catch(e: UnknownServiceException) {
        LOG.E(TAG, "Exception: ${e.message}")
        exception = e
        e.let {
          if (e.message?.contains(C.ERR_MSG_HTTP_FORBIDEN) == true) {
            exception = Exception(C.MSG_ERR_ONLY_SSL)
          }
          resp.value = NetworkResult.Error(e.message)
        }
      } catch(e: Exception) {
        LOG.E(TAG, "EXCEPTION: ${e.message}")
        exception = when (e) {
          is NullPointerException -> Exception(C.MSG_ERR_NPE)
          else -> e
        }
        resp.value = NetworkResult.Error(exception?.message)
      }
    } else {
      exception = Exception(C.ERR_MSG_NO_INTERNET)
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


  private fun handleResponse(response: Response<Version>): NetworkResult<Version>? {
    return when {
      response.message().toString().contains("timeout") -> NetworkResult.Error("Timeout.")
      response.body()!!.version.isEmpty() -> NetworkResult.Error("Version not found.")
      response.isSuccessful -> NetworkResult.Success(response.body()!!) // can be nullable
      else -> NetworkResult.Error("Cannot reach server.")
    }
  }

}
