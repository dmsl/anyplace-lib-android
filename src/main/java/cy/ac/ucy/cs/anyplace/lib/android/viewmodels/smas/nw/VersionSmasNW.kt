package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.nw

import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.preference.Preference
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.NavigatorAppBase
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.SMAS
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.smas.models.SmasVersion
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.di.RetrofitHolderSmas
import kotlinx.coroutines.flow.MutableStateFlow
import retrofit2.Response
import java.lang.Exception
import java.lang.NullPointerException
import java.net.UnknownServiceException

/**
 * Utility method to encapsulate:
 * - the SafeCall of the version endpoint and it's handling:
 */
class VersionSmasNW(
        private val app: NavigatorAppBase,
        private val RH: RetrofitHolderSmas,
        private val repoSmas: RepoSmas) {
  val TG = "nw-version-smas"

  private val resp: MutableStateFlow<NetworkResult<SmasVersion>> = MutableStateFlow(NetworkResult.Unset())

  private val C by lazy { SMAS(app.applicationContext) }

  /**
   * Gets version from remote and on success it updates the [dsChat]
   */
  suspend fun getVersion(): NetworkResult<SmasVersion> {
    val response = repoSmas.remote.getVersion()
    val resp = handleVersionResponse(response)
    val version = resp.data

    if (version != null) {  // SUCCESS
      app.dsSmas.storeVersion(version.rows.version)
    }

    return resp
  }

  suspend fun safeCallAndUpdateUi(versionPref: Preference?) {
    val MT = ::safeCallAndUpdateUi.name
    LOG.D4(TG, "$MT: base url: ${RH.baseURL}")
    versionPref?.summary = "reaching SMAS .."

    var msg = ""
    var exception : Exception? = null
    var versionColor : ForegroundColorSpan? = null

    if (app.hasInternet()) {
      try {
        resp.value = getVersion()
        val version = resp.value.data
        if (version != null) {  // SUCCESS
          msg = "${version.rows.version} (connected: ${utlTime.currentTimePretty()})"
          versionPref?.icon = null
        } else {
          exception = Exception("Failed to get version.")
        }

      } catch(e: UnknownServiceException) {
        LOG.E(TG, e)
        exception = e
        e.let {
          if (e.message?.contains(C.ERR_MSG_HTTP_FORBIDEN) == true) {
            exception = Exception(C.MSG_ERR_ONLY_SSL)
          }
          resp.value = NetworkResult.Error(e.message)
        }
      } catch(e: Exception) {
        LOG.E(TG, "$MT: EXCEPTION: ${e.javaClass}")
        LOG.E(TG, e)
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
      versionPref?.setIcon(R.drawable.ic_sad)
      LOG.E(msg)
      LOG.E(it)
      versionColor = ForegroundColorSpan(app.getColor(R.color.redDark))
    } ?: run {
      versionPref?.setIcon(R.drawable.ic_happy)
    }

    val spannableMsg = SpannableString(msg)
    versionColor?.let { spannableMsg.setSpan(versionColor, 0, spannableMsg.length, 0) }
    versionPref?.summary = spannableMsg
  }

  private fun handleVersionResponse(response: Response<SmasVersion>): NetworkResult<SmasVersion> {
    return when {
      response.message().toString().contains("timeout") -> NetworkResult.Error("Timeout.")
      response.body()!!.rows.version.isEmpty() -> NetworkResult.Error("Version not found.")
      response.isSuccessful -> NetworkResult.Success(response.body()!!) // can be nullable
      else -> NetworkResult.Error("Cannot reach server.")
    }
  }
}