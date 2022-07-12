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
import cy.ac.ucy.cs.anyplace.lib.android.utils.EXP
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
   const val CV_LOG_ALGORITHM = 2
    const val tag = "cv-loc"
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
    user = app.dsChatUser.readUser.first()

    resp.value = NetworkResult.Unset()

    if (detections.isEmpty()) {
      app.showToast(VM.viewModelScope, "No objects detected.")
      return
    }

    resp.value = NetworkResult.Loading()
    if (app.hasInternet()) {
      try {
        val algo = CV_LOG_ALGORITHM
        val prevCoord  = VM.locationSmas.value.coord
        val epoch = utlTime.epoch().toString()
        val req =  if (prevCoord!=null)
          CvLocalizeReq(user, epoch, buid, model.idSmas, detections, algo, prevCoord)
        else {
          if (EXP.LOCALIZATION) {
           val floorNum = VM.wFloor?.floorNumber()!!
            LOG.E(TAG,"EXP MODE: FLOOR: $floorNum")
            CvLocalizeReq(user, epoch, buid, model.idSmas, detections, algo, Coord(0.0, 0.0, floorNum))
          } else {
            CvLocalizeReq(user, epoch, buid, model.idSmas, detections, algo)
          }

        }

        LOG.V2(TAG, "$tag: ${req.time}: #: ${detections.size}")
        val response = repo.remote.cvLocalization(req)

        LOG.V2(TAG, "$tag: Resp: ${response.message()}" )
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

  // TODO:PMX CVM
  fun postResult(l: LocationOfl?) {
    if (l==null) {
      resp.value=NetworkResult.Error("Failed to get location")
    } else {
      val r=CvLocalizeResp(uid="",
              listOf(CvLocation(l.deck, l.dissimilarity, l.flid, l.x, l.y)), status="")
      resp.value=NetworkResult.Success(r)
    }
  }

  private fun handleResponse(resp: Response<CvLocalizeResp>): NetworkResult<CvLocalizeResp> {
    LOG.D3(TAG, "$tag: handleResponse")
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
              val msg = "$tag: REMOTE: $cvLoc"

              LOG.W(TAG, msg)

              // Propagating the result
              val coord = Coord(cvLoc.x, cvLoc.y, cvLoc.deck)
              VM.locationSmas.value = LocalizationResult.Success(coord)
            }
          }
          is NetworkResult.Error -> {
            val msg = it.message ?: "unspecified error"
            LOG.E(TAG, "$tag: $msg")
            app.showToast(VM.viewModelScope, msg, LENGTH_SHORT)
          }
          else -> {}
        }
      }
    }
  }

}
