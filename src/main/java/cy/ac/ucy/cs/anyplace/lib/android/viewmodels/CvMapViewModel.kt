package cy.ac.ucy.cs.anyplace.lib.android.viewmodels

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.data.Repository
import cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers.CvMapHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers.FloorHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers.FloorsHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers.SpaceHelper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.maps.Markers
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.Classifier
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.YoloV4Classifier
import cy.ac.ucy.cs.anyplace.lib.android.utils.ImgUtils
import cy.ac.ucy.cs.anyplace.lib.android.utils.converters.toLatLng
import cy.ac.ucy.cs.anyplace.lib.android.utils.network.RetrofitHolder
import cy.ac.ucy.cs.anyplace.lib.core.LocalizationResult
import cy.ac.ucy.cs.anyplace.lib.models.*
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult.Error
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult.Success
import dagger.hilt.android.lifecycle.HiltViewModel
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
 *  - SMASS TODO
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
        val repository: Repository,
        val retrofitHolder: RetrofitHolder): DetectorViewModel(application) {

  private val C by lazy { CONST(app) }

  /** Controlling navigation mode */
  val localization = MutableStateFlow(Localization.stopped)
  val localizationFlow = localization.asStateFlow()
  // CV WINDOW: on Localization/Logging the detections are grouped per scanning window,
  // e.g., each window might be 5seconds.
  /** related to cv scan window */
  var currentTime : Long = 0
  var windowStart : Long = 0
  /** Detections for the localization scan-window */
  val detectionsLocalization: MutableStateFlow<List<Classifier.Recognition>> = MutableStateFlow(emptyList())
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
  var markers : Markers? = null
  val floorplanFlow : MutableStateFlow<NetworkResult<Bitmap>> = MutableStateFlow(Error(null))
  /** Holds the functionality of a [CvMap] and can generate the [CvMapFast] */
  var cvMapH: CvMapHelper? = null

  // FLOOR PLANS
  fun getFloorplanFromRemote(FH: FloorHelper) = viewModelScope.launch { getFloorplanSafeCall(FH) }
  private fun loadFloorplanFromAsset() {
    LOG.W(TAG, "loading from asset file")
    val base64 = assetReader.getFloorplan64Str()
    val bitmap = base64?.let { ImgUtils.stringToBitmap(it) }
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
    if (app.hasInternetConnection()) {
      val bitmap = FH.requestRemoteFloorplan()
      if (bitmap != null) {
        floorplanFlow.value = Success(bitmap)
        FH.cacheFloorplan(bitmap)
      } else {
        val msg ="Failed to get ${FH.spaceH.prettyFloorplan}. "
        "Base URL: ${retrofitHolder.retrofit.baseUrl()}"
        LOG.E(msg)
        floorplanFlow.value = Error(msg)
      }
    } else {
      floorplanFlow.value = Error("No Internet Connection.")
    }
  }

  //// GOOGLE MAPS
  fun addMarker(latLng: LatLng, msg: String) {
    markers?.addCvMarker(latLng, msg)
  }

  fun hideActiveMarkers() {
    markers?.hideActiveMakers()
  }

  /**
   * Sets a new marker location on the map.
   */
  fun setUserLocation(coord: Coord) {
    LOG.D(TAG, "setUserLocation")
    markers?.setLocationMarker(toLatLng(coord))
  }

  protected open fun prefWindowLocalizationMillis(): Int {
    return C.DEFAULT_PREF_CVLOG_WINDOW_LOCALIZATION_SECONDS.toInt()
  }

  /**
   * Update [detections] that are related only to the localization window.
   */
  protected fun updateDetectionsLocalization(detections: List<Classifier.Recognition>) {
    currentTime = System.currentTimeMillis()
    val appendedDetections = detectionsLocalization.value + detections

    when {
      currentTime-windowStart > prefWindowLocalizationMillis() -> { // window finished
        localization.tryEmit(Localization.stopped)
        location.value = LocalizationResult.Unset()
        if (appendedDetections.isNotEmpty()) {
          LOG.W(TAG_METHOD, "stop: objects: ${appendedDetections.size}")
          // TODO DEDUPLICATE DETECTIONS

          val detectionsDedup =
                  YoloV4Classifier.NMS(detectionsLocalization.value,
                          detector.labels)
          detectionsLocalization.value = detectionsDedup

          LOG.W(TAG_METHOD, "stop: objects: ${detectionsDedup.size} (dedup)")

          location.value = LocalizationResult.Unset()
          if (cvMapH == null) {
            location.value = LocalizationResult.Error("No CvMap on device", "create one with object logging")
          } else {  // estimate and publish position
            location.value = cvMapH!!.cvMapFast.estimatePositionNEW(
                    super.model,
                    detectionsLocalization.value)
          }

          detectionsLocalization.value = emptyList()
        } else {
          LOG.W(TAG_METHOD, "stopped. no detections..")
          location.value = LocalizationResult.Error("Location not found.", "no objects detected")
        }
      } else -> {  // Within a window
      detectionsLocalization.value = appendedDetections as MutableList<Classifier.Recognition>
      LOG.D5(TAG_METHOD, "append: ${appendedDetections.size}")
    }
    }
  }

  // TODO in new class
  // /** Go one floor up */
  fun floorGoUp() {
    LOG.E()
    val floorNumStr = floor.value?.floorNumber.toString()
    if (floorsH.canGoUp(floorNumStr)) {
      val to = floorsH.getFloorAbove(floorNumStr)
      LOG.D(TAG_METHOD, "from: ${floor.value} to: $to")
      floor.value = to
    } else {
      LOG.W(TAG_METHOD, "Cannot go further up.")
    }
  }

  /** Go one floor down */
  fun floorGoDown() {
    LOG.E()
    val floorNumStr = floor.value?.floorNumber.toString()
    if (floorsH.canGoDown(floorNumStr)) {
      val to = floorsH.getFloorBelow(floorNumStr)
      LOG.D(TAG_METHOD, "from: ${floor.value} to: $to")
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
    LOG.E()
    // val spaceH = spaceH!! CLR?
    // val floorsH = floorsH!! CLR?

    LOG.E(TAG,"FloorsH: ${floorsH}")

    if (!floorsH.hasFloors()) {  // space has no floors
      val msg = "Selected ${spaceH.prettyTypeCapitalize} has no ${spaceH.prettyFloors}."
      LOG.E(TAG_METHOD, msg)
      Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
      floor.value = null
    }

    // var floor : Floor? = null
    // START OF: select floor: last selection or first available
    // VMb.floor.value = null
    if (spaceH.hasLastValuesCached()) {
      val lastVal = spaceH.loadLastValues()
      if (lastVal.lastFloor!=null) {
        LOG.D2(TAG_METHOD, "lastVal cache: ${spaceH.prettyFloor}${lastVal.lastFloor}.")
        floor.value = floorsH.getFloor(lastVal.lastFloor!!)
      }
      lastValSpaces = lastVal
    }

    if (floor.value == null)  {
      LOG.D2(TAG_METHOD, "Loading first ${spaceH.prettyFloor}.")
      floor.value = floorsH.getFirstFloor()
    }

    LOG.D(TAG_METHOD, "Selected ${spaceH.prettyFloor}: ${floor.value!!.floorNumber}")
  }
}