package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw

import android.widget.Toast
import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.smas.ChatUserAuth
import cy.ac.ucy.cs.anyplace.lib.android.SmasApp
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.CHAT
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.smas.models.*
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.source.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.nw.SmasErrors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.Response
import java.lang.Exception
import java.net.ConnectException

/**
 * Downloads CvModels from remote
 *
 * NOTE: this is from SMAS API.
 * But cv localization should have been Anyplace API.
 */
class CvModelsGetNW(
        private val app: SmasApp,
        private val VM: CvViewModel,
        private val RH: RetrofitHolderSmas,
        private val repo: RepoSmas) {

  private val err by lazy { SmasErrors(app, VM.viewModelScope) }

  /** Network Responses from API calls */
  private val resp: MutableStateFlow<NetworkResult<CvModelsResp>> = MutableStateFlow(NetworkResult.Unset())


  private val C by lazy { CHAT(app.applicationContext) }
  private lateinit var chatUser : ChatUser

  /** Get [UserLocations] SafeCall */
  suspend fun safeCall() {
    LOG.D2(TAG, "$METHOD: CvModels")
    chatUser = app.dsChatUser.readUser.first()

    resp.value = NetworkResult.Loading()

    if (repo.local.hasCvModelClassesDownloaded()) {
      LOG.W(TAG, "CvModels: already in DB")
      resp.value = NetworkResult.Unset(NetworkResult.DB_LOADED)

      // cache (once) the model ids for faster conversion
      initCvClassConversion()
      return
    }

    if (app.hasInternet()) {
      try {
        val response = repo.remote.cvModelsGet(ChatUserAuth(chatUser))
        LOG.D4(TAG, "CvModelsGet: ${response.message()}" )
        resp.value = handleResponse(response)

        // TODO:PM Persist: put in cache & list (main mem) ?
        // val userLocations = resp.value.data
        // if (userLocations != null) { cache(useLocations, UserOwnership.PUBLIC) }
        val cvModels = resp.value.data?.rows
        if (cvModels == null) {
          val msg = "Downloading CvModels: no classes fetched"
          LOG.W(TAG, msg)
        } else {
          cvModels.forEach {
            LOG.D(TAG, "CvModel: ${it.oid}: ${it.modeldescr}.${it.cid}| ${it.name}")
          }
          persistToDB(cvModels)
        }

        // if (!cacheChat.hasImgCache() && !repo.local.hasMsgs()) {

        // TODO:PMX
      } catch(ce: ConnectException) {
        val msg = "CvModelsGet: Connection failed:\n${RH.retrofit.baseUrl()}"
        handleException(msg, ce)
      } catch(e: Exception) {
        val msg = "$TAG: CvModelsGet: Not Found." + "\nURL: ${RH.retrofit.baseUrl()}"
        handleException(msg, e)
      }
    } else {
      resp.value = NetworkResult.Error(C.ERR_MSG_NO_INTERNET)
    }
  }

  private fun handleResponse(resp: Response<CvModelsResp>): NetworkResult<CvModelsResp> {
    LOG.D3(TAG)
    if(resp.isSuccessful) {
      return when {
        resp.message().toString().contains("timeout") -> NetworkResult.Error("Timeout.")

        resp.isSuccessful -> {
          // SMAS special handling (errors should not be 200/OK)
          val r = resp.body()!!
          if (r.status == "err") return NetworkResult.Error(r.descr)
          return NetworkResult.Success(r)
        } // can be nullable
        else -> NetworkResult.Error(resp.message())
      }
    } else {
      LOG.E(TAG, "handleResponse: unsuccessful")
    }

    return NetworkResult.Error("$TAG: ${resp.message()}")
  }

  private fun handleException(msg:String, e: Exception) {
    LOG.E(TAG, msg)
    LOG.E(TAG, e)
    resp.value = NetworkResult.Error(msg)
  }

  suspend fun collect() {
    LOG.D2(TAG, "$METHOD: CvModelsGet")

    resp.collect {
      when (it)  {
        is NetworkResult.Success -> {
          val cvModels = it.data
          LOG.D2(TAG, "CvModels: received: ${cvModels?.rows?.size} classes")
        }
        is NetworkResult.Error -> {
          LOG.D3(TAG, "Error: msg: ${it.message}")
          if (!err.handle(app, it.message, "loc-get")) {
            val msg = it.message ?: "unspecified error"
            app.showToast(VM.viewModelScope, msg, Toast.LENGTH_SHORT)
          }
        }
        is NetworkResult.Unset -> { }
        else -> { } // might be unset or db loaded
      }
    }
  }

  // ROOM
  /**
   * Persists [ChatMsg]s to SQLite (through ROOM).
   * It does not store images (base64) to DB.
   * Those will be stored in [SmasCache] (file cache)
   */
  private fun persistToDB(obj: List<CvModelClass>) {
    LOG.W(TAG, "$METHOD: storing: ${obj.size} CvModel classes")
    VM.viewModelScope.launch(Dispatchers.IO) {
      obj.forEach {
        repo.local.insertCvModelClass(it)
      }
    }
    initCvClassConversion()
  }

  /**
   * Initialize maps that speed up conversion between modelid
   * and cid (YOLO-derived) to the oid that SMAS uses (CV backend).
   */
  private fun initCvClassConversion() {
    VM.viewModelScope.launch(Dispatchers.IO) {
      val ids = repo.local.getCvModelIds()
      ids.forEach { id ->
        app.cvUtils.initConversionTables(id)
      }
    }
  }

}
