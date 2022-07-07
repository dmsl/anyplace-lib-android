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
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.entities.LocationOfl
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.source.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.nw.SmasErrors
import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationResult
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.*
import cy.ac.ucy.cs.anyplace.lib.smas.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.Response
import java.lang.Exception
import java.net.ConnectException

/**
 * Sends recognized objects to the SMAS backend and receives the user's location.
 */
class CvLocalizeNW(
        private val app: SmasApp,
        private val VM: CvViewModel,
        private val RH: RetrofitHolderSmas,
        private val repo: RepoSmas) {

  companion object {
   const val CV_LOC_ALGORITHM = 2
    const val TAG_TASK = "FP-LOCALIZE"
  }

  private val err by lazy { SmasErrors(app, VM.viewModelScope) }

  /** Network Responses from API calls */
  private val resp: MutableStateFlow<NetworkResult<CvLocalizationResp>>
  = MutableStateFlow(NetworkResult.Unset())

  private val C by lazy { SMAS(app.applicationContext) }
  private lateinit var smasUser : SmasUser

  /** Send the [Chatuser]'s location (safecall) */
  suspend fun safeCall(buid: String,
                       detectionsReq: List<CvDetectionREQ>,
                       model: DetectionModel) {
    smasUser = app.dsChatUser.readUser.first()

    resp.value = NetworkResult.Unset()

    if (detectionsReq.isEmpty()) {
      app.showToast(VM.viewModelScope, "No objects detected.")
      return
    }

    resp.value = NetworkResult.Loading()
    if (app.hasInternet()) {
      try {
        val req= CvLocalizationReq(smasUser, utlTime.epoch().toString(),
                buid, model.idSmas, detectionsReq, CV_LOC_ALGORITHM)
        LOG.V2(TAG, "$TAG_TASK: ${req.time}: #: ${detectionsReq.size}")
        val response = repo.remote.cvLocalization(req)

        LOG.V2(TAG, "$TAG_TASK: Resp: ${response.message()}" )
        resp.value = handleResponse(response)
      } catch(ce: ConnectException) {
        val msg = "Connection failed:\n${RH.retrofit.baseUrl()}"
        handleException(msg, ce)
      } catch(e: Exception) {
        val msg = "Something went wrong ($TAG)" + "\nURL: ${RH.retrofit.baseUrl()}"
        handleException(msg, e)
      }
    } else {
      resp.value = NetworkResult.Error(C.ERR_MSG_NO_INTERNET)
    }
  }

  fun postResult(l: LocationOfl?) {
    if (l==null) {
      resp.value=NetworkResult.Error("Failed to get location")
    } else {
      val r=CvLocalizationResp(uid="",
              listOf(CvLocalization(l.deck, l.dissimilarity, l.flid, l.x, l.y)), status="")
      resp.value=NetworkResult.Success(r)
    }
  }

  private fun handleResponse(resp: Response<CvLocalizationResp>): NetworkResult<CvLocalizationResp> {
    LOG.D3(TAG, "$TAG_TASK: handleResponse")
    if(resp.isSuccessful) {
      when {
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
    resp.value = NetworkResult.Error(msg)
    LOG.E(TAG, msg)
    LOG.E(TAG, e)
  }

  fun collect() {
    VM.viewModelScope.launch(Dispatchers.IO) {
      resp.collect {
        when (it)  {
          is NetworkResult.Success -> {
            if (it.data==null || it.data!!.rows.isEmpty()) {
              val msg = "Failed to get location (from SMAS)"
              app.showToast(VM.viewModelScope, msg, LENGTH_SHORT)
              VM.locationSmas.value = LocalizationResult.Unset()
            } else {
              val cvLoc = it.data!!.rows[0]
              val msg = "$TAG_TASK: REMOTE: $cvLoc"
              // val msg = "$TAG_TASK: REMOTE: coords: ${cvLoc.x} ${cvLoc.y}, FL: ${cvLoc.deck} FLID: ${cvLoc.flid} diss: ${cvLoc.dissimilarity}"

              LOG.W(TAG, msg)
              // app.showToast(VM.viewModelScope, msg, LENGTH_SHORT)

              // Propagating the result
              val coord = Coord(cvLoc.x, cvLoc.y, cvLoc.deck)
              VM.locationSmas.value = LocalizationResult.Success(coord)
            }
          }
          is NetworkResult.Error -> {
            val msg = it.message ?: "unspecified error"
            app.showToast(VM.viewModelScope, msg, LENGTH_SHORT)
          }
          else -> {}
        }
      }
    }
  }

}
