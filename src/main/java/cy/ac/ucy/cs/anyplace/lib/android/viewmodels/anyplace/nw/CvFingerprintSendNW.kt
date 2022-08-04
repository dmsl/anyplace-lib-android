package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw

import android.widget.Toast.*
import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.NavigatorAppBase
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.SMAS
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.DetectionModel
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.di.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerUI
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilErr
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
        private val app: NavigatorAppBase,
        private val VM: CvViewModel,
        private val RH: RetrofitHolderSmas,
        private val repo: RepoSmas) {
  private val TG = "nw-fp-send"
  private val notify = app.notify
  private val utlErr by lazy { UtilErr() }

  private val err by lazy { SmasErrors(app, VM.viewModelScope) }

  /** Network Responses from API calls */
  private val resp: MutableStateFlow<NetworkResult<FingerprintSendResp>> = MutableStateFlow(NetworkResult.Unset())

  private val C by lazy { SMAS(app.applicationContext) }

  /**
   * used in offline logging
   */
  suspend fun uploadFromCache(uiLog: CvLoggerUI) {
    val MT = ::uploadFromCache.name
    val smasUser = app.dsUserSmas.read.first()

    LOG.W(TG, "$MT: upload from cache")
    val msg = C.ERR_MSG_NO_INTERNET

    if (!app.hasInternet()) {
      notify.long(VM.viewModelScope, msg)
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
        notify.long(VM.viewModelScope, "$msg (dropped)")
        uiLog.bottom.logging.showUploadBtn()
        break
      }

      if (entry==null || entry.cvDetections.isNullOrEmpty()) {
        LOG.W(TG, "$MT: ignoring null entry")
        nullEntries++
      } else {
        if (uploadEntry(smasUser, entry)) {
          uploadOK++
          totalObjects+=entry.cvDetections.size
        } else {
          notify.warn(VM.viewModelScope, "Something went wrong during upload!")
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

    if (app.dsCvMap.read.first().autoUpdateCvFingerprints) {
      val fetched = VM.nwCvFingerprintsGet.safeCall(false)
      if (fetched>0) {
        reportMsg+="\nFetched $fetched fingerprints."
      }
    }

    notify.info(VM.viewModelScope, reportMsg)
  }

  /**
   * Uploads a single entry
   */
  private suspend fun uploadEntry(smasUser: SmasUser, entry: FingerprintScan): Boolean {
    val MT = ::uploadEntry.name
    LOG.W(TG, "$TG: $MT: $entry\n")

    try {
      val req= FingerprintSendReq(smasUser, entry)
      val coordStr="l:${req.level}: x:${req.x} y:${req.y}"
      LOG.D3(TG, "$MT: ${req.time}: #: ${entry.cvDetections.size} coords: $coordStr")
      val response = repo.remote.cvFingerprintSend(req)
      LOG.D3(TG, "$MT: Resp: ${response.message()}" )

      return when (val resp = handleResponse(response)) {
        is NetworkResult.Success -> {
          val msg = "Uploaded objects: ${resp.data?.rows}"
          LOG.W(TG, "$MT: $msg")
          true
        }
        else -> {
          false
        }
      }
    } catch(e: Exception) {
      val msg = utlErr.handle(app, RH, VM.viewModelScope, e, TG)
      resp.value = NetworkResult.Error(msg)
    }
    return false
  }


  /** Send the [Chatuser]'s location (safecall).
   * used for online logging.
   * it uploads right after a detection.
   * */
  suspend fun safeCall(userCoords: UserCoordinates,
                       detectionsReq: List<CvObjectReq>, model: DetectionModel) {
    val MT = ::safeCall.name
    val smasUser = app.dsUserSmas.read.first()

    LOG.D2(TG, "$MT: session: ${smasUser.uid} ${smasUser.sessionkey}")

    resp.value = NetworkResult.Unset()
    resp.value = NetworkResult.Loading()
    if (app.hasInternet()) {
      try {
        val req= FingerprintSendReq(smasUser, userCoords, utlTime.epoch().toString(),
                detectionsReq, model.idSmas)
        val coordStr="l:${req.level}: x:${req.x} y:${req.y}"
        LOG.W(TG, "$MT: ${req.time}: #: ${detectionsReq.size} coords: $coordStr")
        val response = repo.remote.cvFingerprintSend(req)
        LOG.W(TG, "$MT: resp: ${response.message()}" )
        resp.value = handleResponse(response)
      } catch(e: Exception) {
        val msg = utlErr.handle(app, RH, VM.viewModelScope, e, TG)
        resp.value = NetworkResult.Error(msg)
      }
    } else {
      resp.value = NetworkResult.Error(C.ERR_MSG_NO_INTERNET)
    }
  }

  private fun handleResponse(resp: Response<FingerprintSendResp>): NetworkResult<FingerprintSendResp> {
    val MT = ::handleResponse.name
    LOG.D3(TG, MT)
    if(resp.isSuccessful) {
      return when {
        resp.errorBody() != null -> {
          NetworkResult.Error(resp.body().toString())
        }
        resp.message().toString().contains("timeout") -> NetworkResult.Error("Timeout.")
        resp.isSuccessful -> {
          // SMAS special handling (errors should not be 200/OK)
          val r = resp.body()!!
          if (r.status == "err") NetworkResult.Error(r.descr) else NetworkResult.Success(r)
        } // can be nullable
        else -> NetworkResult.Error(resp.message())
      }
    }
    return NetworkResult.Error("$TG: ${resp.message()}")
  }

  fun collect() {
    val MT = ::collect.name
    VM.viewModelScope.launch(Dispatchers.IO) {
      resp.collect {
        when (it)  {
          is NetworkResult.Success -> {
            var reportMsg = "Uploaded Fingerprints (${it.data?.rows})"
            LOG.W(TG, "$MT: $reportMsg")

            if (app.dsCvMap.read.first().autoUpdateCvFingerprints) {
              val fetched = VM.nwCvFingerprintsGet.safeCall(false)
              if (fetched>0) {
                reportMsg+="\nFetched $fetched fingerprints."
              }
            }
            notify.shortDEV(VM.viewModelScope, reportMsg)
          }

          is NetworkResult.Error -> {
            if (!err.handle(app, it.message, TG)) {
              val msg = it.message ?: "unspecified error"
              notify.INF(VM.viewModelScope, msg)
            }
          }
          else -> {}
        }
      }
    }
  }

}
