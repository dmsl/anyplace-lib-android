package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.SmasApp
import cy.ac.ucy.cs.anyplace.lib.android.cache.anyplace.Cache
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.FloorWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.FloorsWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.SpaceWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.*
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.source.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.utils.imu.IMU
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.FloorSelector
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.CvUI
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.Classifier
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.DetectorActivityBase
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
import cy.ac.ucy.cs.anyplace.lib.android.utils.net.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlImg
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw.*
import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationResult
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.*
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult.Error
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult.Success
import cy.ac.ucy.cs.anyplace.lib.smas.models.CvObjectReq
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject


/**
 * Localization is generally an one-time call. It gets a list of objects from the camera,
 * and calculates the user location.
 *
 * However, YOLO (and it's camera-related components) operate asynchronously in the background,
 * and store detection lists in 'scanning windows'.
 * Therefore we need the below states.
 */
enum class LocalizationStatus {
  running,
  stopped,
}

/** CvMapViewModel is used by:
 *  - Logger TODO
 *  - Navigator TODO <- DOING
 *  - SMAS TODO
 *
 *  Other notes:
 *    - floorplan fetching
 *    - gmap markers
 */
@HiltViewModel
open class CvViewModel @Inject constructor(
        /** [application] is not an [AnyplaceApp], hence it is not a field.
        [AnyplaceApp] can be used within the class as app through an Extension function */
        application: Application,
        dsCv: CvDataStore,
        private val dsMisc: MiscDataStore,
        dsCvMap: CvMapDataStore,
        val repo: RepoAP,
        val RH: RetrofitHolderAP,
        val repoSmas: RepoSmas,   // MERGE: rename all repoChat to repoSmas
        val RHsmas: RetrofitHolderSmas, ): DetectorViewModel(application, dsCv, dsCvMap) {

  private val C by lazy { CONST(app) }
  /** Updated on changes */
  lateinit var prefsCvMap: CvMapPrefs
  /** Updated on changes */
  lateinit var prefsCv: CvEnginePrefs

  val cache by lazy { Cache(application) }

  lateinit var mu: IMU
  var miEnabled = false

  // UI
  //// COMPONENTS
  lateinit var floorSelector: FloorSelector
  /** Initialized when [GoogleMap] is initialized (see [setupUiGmap]) */
  lateinit var ui: CvUI

  // CV WINDOW: on Localization/Logging the detections are grouped per scanning window,
  // e.g., each window might be 5seconds.
  /** related to cv scan window */
  var currentTime : Long = 0
  var windowStart : Long = 0

  val nwConnections by lazy { ConnectionsGetNW(app, this, RH, repo) }
  val nwPOIs by lazy { POIsGetNW(app, this, RH, repo) }

  val nwCvModelsGet by lazy { CvModelsGetNW(app as SmasApp, this, RHsmas, repoSmas) }
  val nwCvMapGet by lazy { CvMapGetNW(app as SmasApp, this, RHsmas, repoSmas) }
  val nwCvFingerprintSend by lazy { CvFingerprintSendNW(app as SmasApp, this, RHsmas, repoSmas) }
  val nwCvLocalize by lazy { CvLocalizeNW(app as SmasApp, this, RHsmas, repoSmas) }

  /** Controlling localization mode */
  val statusLocalization = MutableStateFlow(LocalizationStatus.stopped)

  /** Detections for the localization scan-window */
  val detectionsLOC: MutableStateFlow<List<Classifier.Recognition>> = MutableStateFlow(emptyList())

  /** Last remotely calculated location (SMAS) */
  val locationSmas: MutableStateFlow<LocalizationResult> = MutableStateFlow(LocalizationResult.Unset())

  /** Selected [Space] (model)*/
  var space: Space? = null
  /** All floors of the selected [space] (model) */
  var floors: Floors? = null
  /** Selected floor/deck ([Floor]) of [space] (model) */
  var floor: MutableStateFlow<Floor?> = MutableStateFlow(null)

  /** Selected [Space] ([SpaceWrapper]) */
  lateinit var wSpace: SpaceWrapper
  /** floorsH of selected [wSpace] */
  lateinit var wFloors: FloorsWrapper
  /** Selected floorH of [wFloors] */
  var wFloor: FloorWrapper? = null

  /** LastVals: user last selections regarding a space.
   * Currently not much use (for a field var), but if we have multiple
   * lastVals for space then it would make sense. */
  var lastValSpaces: LastValSpaces = LastValSpaces()
  val floorplanFlow : MutableStateFlow<NetworkResult<Bitmap>> = MutableStateFlow(NetworkResult.Loading())
  /** Holds the functionality of a [CvMapRM] and can generate the [CvMapFast] */

  // FLOOR PLANS
  fun getFloorplanFromRemote(fw: FloorWrapper) = viewModelScope.launch { getFloorplanSafeCall(fw) }
  private fun loadFloorplanFromAsset() {
    LOG.W(TAG, "loading from asset file")
    val base64 = assetReader.getFloorplan64Str()
    val bitmap = base64?.let { utlImg.decodeBase64(it) }
    floorplanFlow.value =
            when (bitmap) {
              null -> Error("Cant read asset deckplan.")
              else -> Success(bitmap)
            }
  }

  /**
   * Requests a floorplan from remote and publishes outcome to [floorplanFlow].
   */
  private suspend fun getFloorplanSafeCall(FH: FloorWrapper) {
    LOG.E(TAG, "$METHOD: getFloorplanSafeCall (remote)")
    floorplanFlow.value = NetworkResult.Loading()
    // loadFloorplanFromAsset()

    if (app.hasInternet()) {
      val bitmap = FH.requestRemoteFloorplan()
      if (bitmap != null) {
        floorplanFlow.value = Success(bitmap)
        FH.cacheFloorplan(bitmap)
      } else {
        val msg ="getFloorplanSafeCall: bitmap was null. Failed to get ${FH.spaceH.prettyFloorplan}. Base URL: ${RH.retrofit.baseUrl()}"
        LOG.E(TAG, msg)
        floorplanFlow.value = Error(msg)
      }
    } else {
      floorplanFlow.value = Error("No Internet Connection.")
    }
  }

  /**
   * React to [CvEnginePrefs] and [CvMapPrefs] changes
   */
  fun reactToPrefChanges() {
    viewModelScope.launch (Dispatchers.IO){
      dsCvMap.read.collectLatest { prefsCvMap=it }
      dsCv.read.collectLatest { prefsCv=it }
    }
  }

  protected open fun prefWindowLocalizationMs(): Int {
    return prefsCvMap.windowLocalizationMs.toInt()
  }

  open fun processDetections(recognitions: List<Classifier.Recognition>,
                             activity: DetectorActivityBase) {
    LOG.V2(TAG, "VM: CvBASE: $METHOD: ${recognitions.size}")
    when(statusLocalization.value) {
      LocalizationStatus.running -> {
        updateDetectionsLocalization(recognitions)
      }
      else -> {}
    }
  }

  /**
   * Update [detections] that are related only to the localization window.
   */
  protected fun updateDetectionsLocalization(detections: List<Classifier.Recognition>) {
    LOG.W(TAG, "$METHOD: updating for localization..")
    currentTime = System.currentTimeMillis()
    val appendedDetections = detectionsLOC.value + detections

    /**
     - HOW many objects are sending up

     */

    when {
      currentTime-windowStart > prefWindowLocalizationMs() -> { // window finished
        statusLocalization.tryEmit(LocalizationStatus.stopped)
        if (appendedDetections.isNotEmpty()) {
          LOG.W(TAG_METHOD, "stop: objects: ${appendedDetections.size}")
          // deduplicate detections (as we are scanning things in a window of a few seconds)
          val detectionsDedup = detectionsLOC.value
          // val detectionsDedup = YoloV4Classifier.NMS(detectionsLOC.value, detector.labels)

          LOG.W(TAG_METHOD, "stop: objects: ${detectionsDedup.size} (dedup)")

          // POINT OF LOCALIZING:
          if (!app.cvUtils.isModelInited()) {
            LOG.E(TAG, "CvUtils: classes not inited")
            return
          }

          localizeCvMap(detectionsLOC.value)

          detectionsLOC.value = emptyList()
        } else {
          LOG.W(TAG_METHOD, "stopped. no detections..")
        }
      } else -> {  // Within a window
      detectionsLOC.value = appendedDetections as MutableList<Classifier.Recognition>
      LOG.D5(TAG_METHOD, "append: ${appendedDetections.size}")
    }
    }
  }

  var collectingCvLocalization = false
  fun localizeCvMap(recognitions: List<Classifier.Recognition>) {
    LOG.D2(TAG, "$METHOD: performing remote localization")

    // TODO convert detections
    val detectionsReq = app.cvUtils.toCvDetections(viewModelScope, recognitions, model)
    viewModelScope.launch(Dispatchers.IO) {

      if (!collectingCvLocalization) {
        collectingCvLocalization=true
        nwCvLocalize.collect()
      }

      if (DBG.CVM) {
        lcz(detectionsReq)
        // return@launch
      }

      nwCvLocalize.safeCall(wSpace.obj.id, detectionsReq, model)
    }
  }

  suspend fun lcz(detectionsReq: List<CvObjectReq>) {
    if (!repoSmas.local.hasCvMap()) {
      app.showToast(viewModelScope, "Cannot localize. (No CvMap)")
      return
    }

    val chatUser = app.dsUser.readUser.first()
    repoSmas.local.localizeTemp(this, model.idSmas, space!!.id, detectionsReq, chatUser)
  }

  /** TODO in new class
   * Selects the first available floor, or the last floor that was picked
   * for a particular space.
   */
  fun selectInitialFloor(ctx: Context) {
    LOG.V2()
    LOG.V2(TAG,"${wSpace.prettyFloors}: ${wFloors.size}")

    if (!wFloors.hasFloors()) {  // space has no floors
      val msg = "Selected ${wSpace.prettyTypeCapitalize} has no ${wSpace.prettyFloors}."
      LOG.W(TAG_METHOD, msg)
      Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
      floor.update { null }
    }

    // var floor : Floor? = null
    // START OF: select floor: last selection or first available
    // VMb.floor.value = null
    if (wSpace.hasLastValuesCached()) {
      val lastVal = wSpace.loadLastValues()
      if (lastVal.lastFloor!=null) {
        LOG.V3(TAG_METHOD, "lastVal cache: ${wSpace.prettyFloor}${lastVal.lastFloor}.")
        floor.update { wFloors.getFloor(lastVal.lastFloor!!) }
      }
      lastValSpaces = lastVal
    }

    if (floor.value == null)  {
      LOG.V3(TAG_METHOD, "Loading first ${wSpace.prettyFloor}.")
      floor.update { wFloors.getFirstFloor() }
    }

    LOG.V2(TAG_METHOD, "Selected ${wSpace.prettyFloor}: ${floor.value!!.floorNumber}")
  }

  // TODO:PM network manager? ineternet connectiovity?
  var networkStatus = false
  /** normal var, filled by the observer (SelectSpaceActivity) */
  var backOnline = false

  // TODO:PM: bind this when connectivity status changes
  var readBackOnline = dsMisc.readBackOnline.asLiveData()
  var backFromSettings= false // INFO filled by the observer (collected from the fragment)
  var readBackFromSettings= dsMisc.readBackFromSettings.asLiveData()
  fun showNetworkStatus() {
    if (!networkStatus) {
      app.showToast(viewModelScope, "No internet connection!", Toast.LENGTH_LONG)
      saveBackOnline(true)
    } else if(networkStatus && backOnline)  {
      app.showToast(viewModelScope, "Back online!", Toast.LENGTH_LONG)
      saveBackOnline(false)
    }
  }
  private fun saveBackOnline(value: Boolean) =
          viewModelScope.launch(Dispatchers.IO) {
            dsMisc.saveBackOnline(value)
          }
  fun setBackFromSettings() = saveBackFromSettings(true)
  fun unsetBackFromSettings() = saveBackFromSettings(false)
  private fun saveBackFromSettings(value: Boolean) =
          viewModelScope.launch(Dispatchers.IO) {  dsMisc.saveBackFromSettings(value) }

  open fun onLocalizationStarted() {
  }

  open fun onLocalizationEnded() {
  }
}