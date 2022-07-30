package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw

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
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlException
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

  val tag = "nw-fp-send"

  /** Network Responses from API calls */
  private val resp: MutableStateFlow<NetworkResult<FingerprintSendResp>> = MutableStateFlow(NetworkResult.Unset())

  private val C by lazy { SMAS(app.applicationContext) }
  private lateinit var smasUser : SmasUser

  private val utlUi by lazy { UtilUI(app, VM.viewModelScope) }

  suspend fun uploadFromCache(uiLog: CvLoggerUI) {
    smasUser = app.dsSmasUser.read.first()

    LOG.E(TAG, "$METHOD: upload from cache")
    val msg = C.ERR_MSG_NO_INTERNET

    if (!app.hasInternet()) {
      app.snackBarLong(VM.viewModelScope, msg)
      uiLog.bottom.logging.showUploadBtn()
      return
    }

    var totalLocations=0
    var totalObjects=0
    var nullEntries=0
    var uploadOK=0

    while (VM.cache.hasFingerprints()) {
      val entry = VM.cache.topFingerprintsEntry()

      if (!app.hasInternet()) {
        app.snackBarLong(VM.viewModelScope, "$msg (dropped)")
        uiLog.bottom.logging.showUploadBtn()
        break
      }

      if (entry==null || entry.cvDetections.isNullOrEmpty()) {
        LOG.E(TAG, "Ignoring null entry")
        nullEntries++
      } else {
        if (uploadEntry(smasUser, entry)) {
          uploadOK++
          totalObjects+=entry.cvDetections.size
        } else {
          app.snackBarLong(VM.viewModelScope, "Something went error during upload!")
          break
        }
      }

      VM.cache.popFingerprintsEntry()
      totalLocations++
    }
    uiLog.checkForUploadCache(true)

    delay(1000)
    val prettyLocations=if (totalLocations>1) "locations" else "location"
    var reportMsg = "Uploaded $totalObjects objects, in $totalLocations $prettyLocations."
    if (nullEntries > 0) reportMsg+="\n(ignored $nullEntries without objects)"

    app.snackBarLong(VM.viewModelScope, reportMsg)
  }

  /**
   * Uploads a single entry
   *
   * TODO:PMX OFL Implement this for offline
   */
  private suspend fun uploadEntry(smasUser: SmasUser, entry: FingerprintScan): Boolean {
    LOG.W(TAG, "$tag: $METHOD: $entry\n")
    try {
      val req= FingerprintSendReq(smasUser, entry)
      val coordStr="l:${req.deck}: x:${req.x} y:${req.y}"
      LOG.D3(TAG, "$tag: ${req.time}: #: ${entry.cvDetections.size} coords: $coordStr")
      val response = repo.remote.cvFingerprintSend(req)
      LOG.D3(TAG, "$tag: Resp: ${response.message()}" )

      return when (val resp = handleResponse(response)) {
        is NetworkResult.Success -> {
          val msg = "Uploaded objects: ${resp.data?.rows}"
          LOG.W(TAG, msg)
          // app.showSnackbarShortDEV(VM.viewModelScope, msg)
          true
        }
        else -> {
          false
        }
      }
    } catch(e: Exception) {
      val msg = utlException.handleException(app, RH, VM.viewModelScope, e, tag)
      resp.value = NetworkResult.Error(msg)
    }
    return false
  }


  /** Send the [Chatuser]'s location (safecall) */
  suspend fun safeCall(userCoords: UserCoordinates,
                       detectionsReq: List<CvObjectReq>, model: DetectionModel) {
    smasUser = app.dsSmasUser.read.first()

    LOG.D2(TAG, "Session: ${smasUser.uid} ${smasUser.sessionkey}")

    resp.value = NetworkResult.Unset()
    resp.value = NetworkResult.Loading()
    if (app.hasInternet()) {
      try {
        val req= FingerprintSendReq(smasUser, userCoords, utlTime.epoch().toString(),
                detectionsReq, model.idSmas)
        val coordStr="l:${req.deck}: x:${req.x} y:${req.y}"
        LOG.W(TAG, "$tag: ${req.time}: #: ${detectionsReq.size} coords: $coordStr")
        val response = repo.remote.cvFingerprintSend(req)
        LOG.W(TAG, "$tag: Resp: ${response.message()}" )
        resp.value = handleResponse(response)
      } catch(e: Exception) {
        val msg = utlException.handleException(app, RH, VM.viewModelScope, e, tag)
        resp.value = NetworkResult.Error(msg)
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
          return if (r.status == "err") NetworkResult.Error(r.descr) else NetworkResult.Success(r)
        } // can be nullable
        else -> return NetworkResult.Error(resp.message())
      }
    }
    return NetworkResult.Error("$tag: ${resp.message()}")
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
              app.snackBarInf(VM.viewModelScope, msg)
            }
          }
          else -> {}
        }
      }
    }
  }

}
