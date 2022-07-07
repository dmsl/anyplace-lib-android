package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw

import android.widget.Toast
import android.widget.Toast.*
import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.android.SmasApp
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.SMAS
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.DetectionModel
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.source.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerUI
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.UtilUI
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.nw.SmasErrors
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.*
import cy.ac.ucy.cs.anyplace.lib.smas.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.Response
import java.lang.Exception
import java.net.ConnectException

/**
 * Manages Location fetching of other users
 *
 * NOTE: this is from SMAS API.
 * But cv localization should have been Anyplace API.
 */
class CvFingerprintSendNW(
        private val app: SmasApp,
        private val VM: CvViewModel,
        private val RH: RetrofitHolderSmas,
        private val repo: RepoSmas) {

  private val err by lazy { SmasErrors(app, VM.viewModelScope) }

  /** Network Responses from API calls */
  private val resp: MutableStateFlow<NetworkResult<FingerprintSendResp>> = MutableStateFlow(NetworkResult.Unset())

  private val C by lazy { SMAS(app.applicationContext) }
  private lateinit var smasUser : SmasUser

  private val utlUi by lazy { UtilUI(app, VM.viewModelScope) }

  suspend fun uploadFromCache(uiLog: CvLoggerUI) {
    smasUser = app.dsChatUser.readUser.first()

    LOG.E(TAG, "$METHOD: upload from cache")
    val msg = "No internet!"

    if (!app.hasInternet()) {
      app.showToast(VM.viewModelScope, msg)
      uiLog.bottom.logging.showUploadBtn()
      return
    }

    var total=0
    var nullEntries=0
    var uploadOK=0

    while (VM.cache.hasFingerprints()) {
      val entry = VM.cache.topFingerprintsEntry()

      if (!app.hasInternet()) {
        app.showToast(VM.viewModelScope, "$msg (dropped)")
        uiLog.bottom.logging.showUploadBtn()
        break
      }

      if (entry==null || entry.cvDetections.isNullOrEmpty()) {
        LOG.E(TAG, "Ignoring null entry")
        nullEntries++
      } else {
        if (uploadEntry(smasUser, entry)) {
          uploadOK++
        } else {
          app.showToast(VM.viewModelScope, "Something went error during upload!")
          break
        }
      }

      VM.cache.popFingerprintsEntry()
      total++
    }
    uiLog.checkForUploadCache(true)

    delay(1000)
    var reportMsg = "Uploaded objects in $total locations"
    if (nullEntries > 0) reportMsg+="\n(ignored $nullEntries without objects)"

    app.showToast(VM.viewModelScope, reportMsg, Toast.LENGTH_LONG)
  }

  private suspend fun uploadEntry(smasUser: SmasUser, entry: FingerprintScan): Boolean {
    LOG.E(TAG, "ENTRY: $entry\n")
    try {
      val req= FingerprintSendReq(smasUser, entry)
      LOG.D3(TAG, "FP-Send: ${req.time}: #: ${entry.cvDetections.size} coords: deck: ${req.deck}: x:${req.x} y:${req.y}")
      val response = repo.remote.cvFingerprintSend(req)
      LOG.D3(TAG, "FP-Send: Resp: ${response.message()}" )

      return when (val resp = handleResponse(response)) {
        is NetworkResult.Success -> {
          val msg = "Uploaded objects: ${resp.data?.rows}"
          LOG.W(TAG, msg)
          app.showToast(VM.viewModelScope, msg, LENGTH_SHORT)
          true
        }
        else -> {
          false
        }
      }

    } catch(ce: ConnectException) {
      val msg = "Connection failed:\n${RH.retrofit.baseUrl()}"
      // handleException(msg, ce)
      LOG.E(TAG, "$METHOD", ce)
    } catch(e: Exception) {
      val msg = "$TAG: Not Found." + "\nURL: ${RH.retrofit.baseUrl()}"
      LOG.E(TAG, "$METHOD", e)
      // handleException(msg, e)
    }
    return false
  }


  /** Send the [Chatuser]'s location (safecall) */
  suspend fun safeCall(userCoords: UserCoordinates,
                       detectionsReq: List<CvDetectionREQ>, model: DetectionModel) {
    smasUser = app.dsChatUser.readUser.first()

    LOG.D2(TAG, "Session: ${smasUser.uid} ${smasUser.sessionkey}")

    LOG.W(TAG, "Session: ${smasUser.uid} ${smasUser.sessionkey}")

    resp.value = NetworkResult.Unset()
    resp.value = NetworkResult.Loading()
    if (app.hasInternet()) {
      try {
        val req= FingerprintSendReq(smasUser, userCoords, utlTime.epoch().toString(),
                detectionsReq, model.idSmas)
        LOG.W(TAG, "FP-Send: ${req.time}: #: ${detectionsReq.size} coords: deck: ${req.deck}: x:${req.x} y:${req.y}")
        val response = repo.remote.cvFingerprintSend(req)
        LOG.W(TAG, "FP-Send: Resp: ${response.message()}" )
        resp.value = handleResponse(response)
      } catch(ce: ConnectException) {
        val msg = "Connection failed:\n${RH.retrofit.baseUrl()}"
        handleException(msg, ce)
      } catch(e: Exception) {
        LOG.W(TAG, "WILL THROW MSG")
        val msg = "$TAG: Not Found." + "\nURL: ${RH.retrofit.baseUrl()}"
        handleException(msg, e)
      }
    } else {
      resp.value = NetworkResult.Error(C.ERR_MSG_NO_INTERNET)
    }
  }

  private fun handleResponse(resp: Response<FingerprintSendResp>): NetworkResult<FingerprintSendResp> {
    LOG.D3(TAG, "handleResponse")
    if(resp.isSuccessful) {
      when {
        resp.errorBody() != null -> {
          return NetworkResult.Error(resp.body().toString())
        }
        resp.message().toString().contains("timeout") -> return NetworkResult.Error("Timeout.")
        resp.isSuccessful -> {
          // SMAS special handling (errors should not be 200/OK)
          val r = resp.body()!!
          if (r.status == "err")  {
            return NetworkResult.Error(r.descr)
          }

          return NetworkResult.Success(r)
        } // can be nullable
        else -> return NetworkResult.Error(resp.message())
      }
    }
    return NetworkResult.Error("$TAG: ${resp.message()}")
  }

  private fun handleException(msg: String, e: Exception) {
    LOG.W(TAG, "handling exception")
    resp.value = NetworkResult.Error(msg)
    LOG.E(TAG, msg)
    LOG.E(TAG, e)
  }

  fun collect() {
    VM.viewModelScope.launch(Dispatchers.IO) {
      resp.collect {
        when (it)  {
          is NetworkResult.Success -> {
            val msg = "Uploaded Fingerprints (${it.data?.rows})"
            LOG.W(TAG, msg)
            app.showToast(VM.viewModelScope, msg, LENGTH_SHORT)
          }
          is NetworkResult.Error -> {
            if (!err.handle(app, it.message, "loc-send")) {
              val msg = it.message ?: "unspecified error"
              app.showToast(VM.viewModelScope, msg, LENGTH_SHORT)
            }
          }
          else -> {}
        }
      }
    }
  }

}
