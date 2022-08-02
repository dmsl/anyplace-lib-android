package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.LevelWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.di.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlImg
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Level
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Response

/**
 * Downloads levelplans: floorplans or deckplans
 */
class LevelPlanNW(
        private val app: AnyplaceApp,
        private val VM: CvViewModel,
        private val RH: RetrofitHolderSmas,
        private val repo: RepoSmas) {
  val TG = "nw-levelplan"
  val notify = app.notify

  val scope = VM.viewModelScope

  /** the [Bitmap] of the current level (floorplan, or deckplan) */
  val bitmap: MutableStateFlow<NetworkResult<Bitmap>> = MutableStateFlow(NetworkResult.Loading())

  /**
   * Requests a floorplan from remote and publishes outcome to [bitmap].
   */
  suspend fun getLevelplan(LW: LevelWrapper) {
    val MT = ::getLevelplan.name
    LOG.E(TG, "$MT: remote")

    scope.launch(Dispatchers.IO) {
      bitmap.update { NetworkResult.Loading() }

      if (app.hasInternet()) {
        val bitmap = requestInternal(LW.obj)
        if (bitmap != null) {
          this@LevelPlanNW.bitmap.value = NetworkResult.Success(bitmap)
          LW.cacheLevelplan(bitmap)
        } else {
          val msg ="$TG: bitmap was null. Failed to get ${LW.wSpace.prettyLevelplan}. Base URL: ${RH.retrofit.baseUrl()}"
          this@LevelPlanNW.bitmap.update { NetworkResult.Error(msg) }
        }
      } else {
        bitmap.update { NetworkResult.Error(VM.C.ERR_MSG_NO_INTERNET) }
      }
    }
  }

  /**
   * Reads a LevelPlan from assets
   */
  @Deprecated("used for testing/development")
  fun loadFromAssets() {
    LOG.W(TG, "loading from asset file")
    val base64 = VM.assetReader.getFloorplan64Str()
    val bitmap = base64?.let { utlImg.base64toBitmap(it) }
    this.bitmap.value =
            when (bitmap) {
              null -> NetworkResult.Error("Cant read asset deckplan.")
              else -> NetworkResult.Success(bitmap)
            }
  }

  var showedMsgDone=true
  var showedMsgDownloading=false
  /**
   * It downloads each missing levelplan
   */
  suspend fun downloadAll() {
    val MT = ::downloadAll.name

    var alreadyCached=""
    val app = VM.app
    app.wLevels.obj.forEach { level ->
      LOG.W(TG, "$MT: level: ${level.name}")
      val LW = LevelWrapper(level, app.wSpace)
      if (!LW.hasLevelplanCached()) {
        // at least one floor needs to be downloaded:
        // show notification now (and when done [showedMsgDone]
        if (!showedMsgDownloading) {
          notify.long(VM.viewModelScope, "Downloading all ${LW.prettyFloors} ..\nKeep app open..")
          showedMsgDownloading=true
          showedMsgDone=false // show another msg at the end
        }
        val bitmap = requestInternal(level)
        if (bitmap != null) {
          LW.cacheLevelplan(bitmap)
          LOG.V2("Downloaded: ${LW.prettyLevelplanNumber()}.")
        }
      } else {
        alreadyCached+="${LW.obj.number}, "
      }
    }

    if (!showedMsgDone) {
      showedMsgDone=true
      notify.short(VM.viewModelScope, "All ${app.wLevels.size} ${app.wSpace.prettyFloors} downloaded!")
    }

    if (alreadyCached.isNotEmpty()) {
      LOG.V3(TAG_METHOD, "already cached ${app.wSpace.prettyLevelplans}: ${alreadyCached.dropLast(2)}")
    }
  }

  /**
   * Internal request code to get the Base64 image from remote
   */
  private suspend fun requestInternal(level: Level) : Bitmap? {
    val MT = ::requestInternal.name
    LOG.D3(TG,"$MT: ${level.buid}: ${level.number}")
    val response = app.wSpace.repo.remote.getFloorplanBase64(level.buid, level.number)
    return handleResponse(response)
  }

  /** Handles the response by decoding the base64 to a [bitmap] */
  private fun handleResponse(response: Response<ResponseBody>): Bitmap? {
    if (response.errorBody() != null) {
      LOG.E("Response: ErrorBody: ${response.errorBody().toString()}")
      return null
    }
    val base64 = response.body()?.string()
    val byteArray = Base64.decode(base64, Base64.DEFAULT)
    return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
  }


  /**
   * Reads a levelplan image from the devices cache
   */
  fun readFromCache(VM: CvViewModel, FW: LevelWrapper) {
    val MT = ::readFromCache.name
    LOG.V(TG, "$MT: ${FW.prettyLevelName()}")

    val localResult =
            when (val bitmap = FW.loadLevelplanFromCache()) {
              null -> {
                val msg ="Failed to load from local cache"
                LOG.W(TG, "$MT: msg")
                NetworkResult.Error(msg)
              }
              else -> {
                LOG.D2(TG, "$MT: success.")
                NetworkResult.Success(bitmap)
              }
            }
    VM.nwLevelPlan.bitmap.update { localResult }
  }

  /**
   * Renders a levelplan on map
   */
  fun render(bitmap: Bitmap?, LW: LevelWrapper) {
    val method = ::render.name
    LOG.E(TG, method)
    LOG.E(TG, "$method: ${LW.wSpace.obj.name}: ${LW.wSpace.obj.buid}")

    VM.ui.map.overlays.drawFloorplan(bitmap, VM.ui.map.obj, LW.bounds())
  }
}