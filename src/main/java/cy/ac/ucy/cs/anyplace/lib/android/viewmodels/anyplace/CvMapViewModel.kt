package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import cy.ac.ucy.cs.anyplace.lib.android.SmasApp
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.CvMapHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.FloorHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.FloorsHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.SpaceHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.*
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.source.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.maps.Markers
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.Classifier
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.YoloV4Classifier
import cy.ac.ucy.cs.anyplace.lib.android.utils.net.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlImg
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlLoc
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw.CvFingerprintSendNW
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw.CvModelsGetNW
import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationResult
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.*
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult.Error
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
enum class Localization {
  running,
  stopped,
  stoppedNoDetections,
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
open class CvMapViewModel @Inject constructor(
        /** [application] is not an [AnyplaceApp], hence it is not a field.
        [AnyplaceApp] can be used within the class as app through an Extension function */
        application: Application,
        dsCv: CvDataStore,
        private val dsMisc: MiscDataStore,
        dsCvNav: CvNavDataStore,
        val repo: RepoAP,
        val RH: RetrofitHolderAP,
        val repoSmas: RepoSmas,   // MERGE: rename all repoChat to repoSmas
        val RHsmas: RetrofitHolderSmas, ): DetectorViewModel(application, dsCv, dsCvNav) {

  private val C by lazy { CONST(app) }

  lateinit var prefsCV: CvPrefs
  // lateinit var prefsNav: CvNavigationPrefs
  val prefsCvNav = dsCvNav.read

  lateinit var prefsNav: CvNavigationPrefs

  val nwCvModelsGet by lazy { CvModelsGetNW(app as SmasApp, this, RHsmas, repoSmas) }
  val nwCvFingerprintSend by lazy { CvFingerprintSendNW(app as SmasApp, this, RHsmas, repoSmas) }

  /** Controlling navigation mode */
  val localization = MutableStateFlow(Localization.stopped)
  val localizationFlow = localization.asStateFlow()
  // CV WINDOW: on Localization/Logging the detections are grouped per scanning window,
  // e.g., each window might be 5seconds.
  /** related to cv scan window */
  var currentTime : Long = 0
  var windowStart : Long = 0
  /** Detections for the localization scan-window */
  val detectionsNAV: MutableStateFlow<List<Classifier.Recognition>> = MutableStateFlow(emptyList())
  /** Last Anyplace location */
  val location: MutableStateFlow<LocalizationResult> = MutableStateFlow(LocalizationResult.Unset())
  /** Selected [Space] */
  var space: Space? = null
  /** All floors of the selected [space] */
  var floors: Floors? = null
  /** Selected [Space] ([SpaceHelper]) */
  lateinit var spaceH: SpaceHelper
  /** floorsH of selected [spaceH] */
  lateinit var floorsH: FloorsHelper
  /** Selected floorH of [floorsH] */
  var floorH: FloorHelper? = null
  /** Selected floor/deck ([Floor]) of [space] */
  var floor: MutableStateFlow<Floor?> = MutableStateFlow(null)
  /** LastVals: user last selections regarding a space.
   * Currently not much use (for a field var), but if we have multiple
   * lastVals for space then it would make sense. */
  var lastValSpaces: LastValSpaces = LastValSpaces()
  /** Initialized onMapReady */
  lateinit var markers : Markers
  val floorplanFlow : MutableStateFlow<NetworkResult<Bitmap>> = MutableStateFlow(Error(null))
  /** Holds the functionality of a [CvMap] and can generate the [CvMapFast] */
  var cvMapH: CvMapHelper? = null

  // FLOOR PLANS
  fun getFloorplanFromRemote(FH: FloorHelper) = viewModelScope.launch { getFloorplanSafeCall(FH) }
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
  private suspend fun getFloorplanSafeCall(FH: FloorHelper) {
    floorplanFlow.value = NetworkResult.Loading()
    // loadFloorplanFromAsset()

    // CHECK:PM: BUG: "Failed to fetch": sometimes (with internet) it failed to fetch..
    if (FH.hasFloorplanCached()) {
      LOG.W(TAG, "$METHOD: loading from cache?? why always fetching?")
      // floorplanFlow.value = Success(FH.loadFromCache()!!)
    }

    if (app.hasInternet()) {
      val bitmap = FH.requestRemoteFloorplan()
      if (bitmap != null) {
        floorplanFlow.value = Success(bitmap)
        FH.cacheFloorplan(bitmap)
      } else {
        val msg ="Failed to get ${FH.spaceH.prettyFloorplan}. Base URL: ${RH.retrofit.baseUrl()}"
        LOG.E(TAG, msg)
        floorplanFlow.value = Error(msg)
      }
    } else {
      floorplanFlow.value = Error("No Internet Connection.")
    }
  }

  //// GOOGLE MAPS
  fun addCvMarker(latLng: LatLng, msg: String) {
    markers.addCvMarker(latLng, msg)
  }

  fun hideCvMarkers() {
    markers.hideCvObjMarkers()
  }

  fun addUserMarkers(userLocation: List<UserLocation>, scope: CoroutineScope) {
    userLocation.forEach {
      scope.launch(Dispatchers.Main) {
        markers.addUserMarker(LatLng(it.x, it.y), it.uid, scope, it.alert, it.time)
      }
    }
  }

  fun hideUserMarkers() {
    markers.hideUserMarkers()
  }

  /**
   * Sets a new marker location on the map.
   */
  fun setUserLocation(coord: Coord) {
    LOG.D(TAG, "setUserLocation")
    markers?.setLocationMarker(utlLoc.toLatLng(coord))
  }

  protected open fun prefWindowLocalizationMillis(): Int {
    return C.DEFAULT_PREF_CVLOG_WINDOW_LOCALIZATION_SECONDS.toInt()
  }

  /**
   * Update [detections] that are related only to the localization window.
   */
  protected fun updateDetectionsLocalization(detections: List<Classifier.Recognition>) {
    currentTime = System.currentTimeMillis()
    val appendedDetections = detectionsNAV.value + detections

    when {
      currentTime-windowStart > prefWindowLocalizationMillis() -> { // window finished
        localization.tryEmit(Localization.stopped)
        location.value = LocalizationResult.Unset()
        if (appendedDetections.isNotEmpty()) {
          LOG.W(TAG_METHOD, "stop: objects: ${appendedDetections.size}")
          // TODO DEDUPLICATE DETECTIONS

          val detectionsDedup =
                  YoloV4Classifier.NMS(detectionsNAV.value, detector.labels)
          detectionsNAV.value = detectionsDedup

          LOG.W(TAG_METHOD, "stop: objects: ${detectionsDedup.size} (dedup)")

          location.value = LocalizationResult.Unset()
          if (cvMapH == null) {
            location.value = LocalizationResult.Error("No CvMap on device", "create one with object logging")
          } else {  // estimate and publish position
            location.value = cvMapH!!.cvMapFast.estimatePositionNEW(
                    app.cvUtils, model, detectionsNAV.value)
          }

          detectionsNAV.value = emptyList()
        } else {
          LOG.W(TAG_METHOD, "stopped. no detections..")
          location.value = LocalizationResult.Error("Location not found.", "no objects detected")
        }
      } else -> {  // Within a window
      detectionsNAV.value = appendedDetections as MutableList<Classifier.Recognition>
      LOG.D5(TAG_METHOD, "append: ${appendedDetections.size}")
    }
    }
  }

  // TODO in new class
  /** Go one floor up */
  fun floorGoUp() {
    LOG.V3()
    val floorNumStr = floor.value?.floorNumber.toString()
    if (floorsH.canGoUp(floorNumStr)) {
      val to = floorsH.getFloorAbove(floorNumStr)
      LOG.V2(TAG_METHOD, "from: ${floor.value?.floorNumber} to: ${to?.floorNumber}")
      floor.value = to
    } else {
      LOG.W(TAG_METHOD, "Cannot go further up.")
    }
  }

  /** Go one floor down */
  fun floorGoDown() {
    LOG.V3()
    val floorNumStr = floor.value?.floorNumber.toString()
    if (floorsH.canGoDown(floorNumStr)) {
      val to = floorsH.getFloorBelow(floorNumStr)
      LOG.V2(TAG_METHOD, "from: ${floor.value?.floorNumber} to: ${to?.floorNumber}")
      floor.value = to
    } else {
      LOG.W(TAG_METHOD, "Cannot go further down.")
    }
  }


  /** TODO in new class
   * Selects the first available floor, or the last floor that was picked
   * for a particular space.
   */
  fun selectInitialFloor(ctx: Context) {
    LOG.V2()
    LOG.V2(TAG,"${spaceH.prettyFloors}: ${floorsH.size}")

    if (!floorsH.hasFloors()) {  // space has no floors
      val msg = "Selected ${spaceH.prettyTypeCapitalize} has no ${spaceH.prettyFloors}."
      LOG.W(TAG_METHOD, msg)
      Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
      floor.value = null
    }

    // var floor : Floor? = null
    // START OF: select floor: last selection or first available
    // VMb.floor.value = null
    if (spaceH.hasLastValuesCached()) {
      val lastVal = spaceH.loadLastValues()
      if (lastVal.lastFloor!=null) {
        LOG.V3(TAG_METHOD, "lastVal cache: ${spaceH.prettyFloor}${lastVal.lastFloor}.")
        floor.value = floorsH.getFloor(lastVal.lastFloor!!)
      }
      lastValSpaces = lastVal
    }

    if (floor.value == null)  {
      LOG.V3(TAG_METHOD, "Loading first ${spaceH.prettyFloor}.")
      floor.value = floorsH.getFirstFloor()
    }

    LOG.V2(TAG_METHOD, "Selected ${spaceH.prettyFloor}: ${floor.value!!.floorNumber}")
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

}