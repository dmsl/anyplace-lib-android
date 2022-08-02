package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw

import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.android.SmasApp
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.SMAS
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.DetectionModel
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.entities.OfflineLocalization
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.di.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilErr
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.TrackingMode
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.nw.SmasErrors
import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationResult
import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationResult.Companion.ENGINE_QUERY_OFFLINE
import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationResult.Companion.ENGINE_QUERY_ONLINE
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.*
import cy.ac.ucy.cs.anyplace.lib.smas.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.Response
import java.lang.Exception

/**
 * Sends recognized objects to the SMAS backend and receives the user's location.
 */
class CvLocalizeNW(
        private val app: SmasApp,
        private val VM: CvViewModel,
        private val RH: RetrofitHolderSmas,
        private val repo: RepoSmas) {
  private val notify = app.notify
  private val utlErr by lazy { UtilErr() }

  companion object {
    private const val TG = "nw-cv-loc"
    
    const val CV_LOG_ALGO_GLOBAL = 3
    const val CV_LOG_ALGO_NEW = 4
  }

  private val err by lazy { SmasErrors(app, VM.viewModelScope) }

  /** Network Responses from API calls */
  private val resp: MutableStateFlow<NetworkResult<CvLocalizeResp>>
          = MutableStateFlow(NetworkResult.Unset())

  private val C by lazy { SMAS(app.applicationContext) }
  private lateinit var user : SmasUser

  /** Send the [SmasUser]'s location (safecall) */
  suspend fun safeCall(buid: String,
                       detections: List<CvObjectReq>,
                       model: DetectionModel) {
    val MT = ::safeCall.name

    user = app.dsUserSmas.read.first()

    resp.value = NetworkResult.Unset()

    val tracking = VM.trackingMode.first() == TrackingMode.on
    if (detections.isEmpty() && !tracking) {
      notify.shortDEV(VM.viewModelScope, "No objects detected.")
      return
    }

    resp.value = NetworkResult.Loading()
    if (app.hasInternet()) {
      try {
        var algo = CV_LOG_ALGO_NEW // TODO this can become an option
        val prevCoord  = app.locationSmas.value.coord
        val epoch = utlTime.epoch().toString()

        val req =  if (prevCoord!=null) {
          CvLocalizeReq(user, epoch, buid, model.idSmas, detections, algo, prevCoord)
        } else {
          algo= CV_LOG_ALGO_GLOBAL
          CvLocalizeReq(user, epoch, buid, model.idSmas, detections, algo)
        }

        val strInfo = "Recognitions: ${detections.size}. Algo: Online: $algo"
        // detections.forEach { strInfo+= "${it.oid} " }

        val trackingOn = VM.trackingMode.first() == TrackingMode.on
        if (!trackingOn) {
          notify.shortDEV(VM.viewModelScope, strInfo)
        }

        LOG.V2(TG, "$MT: ${req.time}: #: ${detections.size}")
        LOG.W(TG, "$MT: calling remote endpoint..")
        val response = repo.remote.cvLocalization(req)
        LOG.W(TG, "$MT: Resp: ${response.message()}" )
        resp.value = handleResponse(response)
      } catch(e: Exception) {
        val msg = utlErr.handle(app, RH, VM.viewModelScope, e, TG)
        resp.value = NetworkResult.Error(msg)
      }
    } else {
      resp.value = NetworkResult.Error(C.ERR_MSG_NO_INTERNET)
    }
  }

  /**
   * Post offline localization result
   */
  fun postOfflineResult(l: OfflineLocalization?) {
    if (l==null) {
      resp.value=NetworkResult.Error("Failed to get location")
    } else {
      val r=CvLocalizeResp(uid="",
              listOf(CvLocation(l.deck, l.dissimilarity, l.flid, l.x, l.y)), status=ENGINE_QUERY_OFFLINE)
      resp.value=NetworkResult.Success(r)
    }
  }

  private fun handleResponse(resp: Response<CvLocalizeResp>): NetworkResult<CvLocalizeResp> {
    LOG.D3(TG, "$TG: handleResponse")
    if(resp.isSuccessful) {
      when {
        resp.message().toString().contains("timeout") -> return NetworkResult.Error("Timeout.")
        resp.isSuccessful -> {
          // SMAS special handling (errors should not be 200/OK)
          val r = resp.body()!!
          if (r.status == "err")  { return NetworkResult.Error(r.descr) }

          r.status= ENGINE_QUERY_ONLINE
          return NetworkResult.Success(r)
        } // can be nullable
        else -> return NetworkResult.Error(resp.message())
      }
    }
    return NetworkResult.Error("$TAG: ${resp.message()}")
  }

  fun collect() {
    val MT = ::collect.name
    VM.viewModelScope.launch(Dispatchers.IO) {
      resp.collect {
        when (it)  {
          is NetworkResult.Success -> {
            if (it.data==null || it.data!!.rows.isEmpty()) {
              val msg = "Unable to localize.\nPlease collect more fingerprints with Logger\nor set manually (long-press)"
              LOG.E(TG, "$MT: $msg")
              notify.WARN(VM.viewModelScope, msg)
              app.locationSmas.value = LocalizationResult.Unset()
              LOG.E(TG, "$MT: Failed to get location: ${it.message.toString()}")
            } else {
              val cvLoc = it.data!!.rows[0]
              val msg = "$MT: REMOTE: $cvLoc"
              LOG.W(TG, msg)

              // Propagating the result
              val coord = Coord(cvLoc.x, cvLoc.y, cvLoc.level)
              app.locationSmas.value = LocalizationResult.Success(coord, it.data!!.status)
            }
          }
          is NetworkResult.Error -> {
            val msg = it.message ?: "unspecified error"
            LOG.E(TG, "$MT: $msg")
            notify.warn(VM.viewModelScope, msg)
          }
          else -> {}
        }
      }
    }
  }
}
