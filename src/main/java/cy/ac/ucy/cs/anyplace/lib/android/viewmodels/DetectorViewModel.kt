package cy.ac.ucy.cs.anyplace.lib.android.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.cv.enums.YoloConstants
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.legacy.gnk.utils.Detector
import cy.ac.ucy.cs.anyplace.lib.android.data.Repository
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite.Classifier
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite.DetectorActivityBase
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite.YoloV4Classifier
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite.customview.OverlayView
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite.env.BorderedText
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite.env.ImageUtils
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite.tracking.MultiBoxTracker
import cy.ac.ucy.cs.anyplace.lib.android.utils.demo.AssetReader
import cy.ac.ucy.cs.anyplace.lib.android.utils.network.RetrofitHolder
import cy.ac.ucy.cs.anyplace.lib.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.processor.internal.definecomponent.codegen._dagger_hilt_android_components_ActivityRetainedComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*
import javax.inject.Inject

// TODO MOVE
// /**
//  * Localization is generally an one-time call. It gets a list of objects from the camera,
//  * and calculates the user location.
//  *
//  * However, YOLO (and it's camera-related components) operate asynchronously in the background,
//  * and store detection lists in 'scanning windows'.
//  * Therefore we need the below states.
//  */
// enum class Localization {
//   running,
//   stopped,
//   stoppedNoDetections,
// }

/**
 * [DetectorViewModel] contains only whatever is related to
 * - object detection (YOLOV4-TFLITE):
 *    - YOLO-related setup
 *
 *  Initialized by [DetectorActivityBase.onPreviewSizeChosen]
 *
 *  TODO put in CvMapViewModel
 *    - floorplan fetching
 *    - gmap markers
 */
@HiltViewModel
open class DetectorViewModel @Inject constructor(application: Application) :
        AndroidViewModel(application) {
  companion object {
  }

  /** Last Detections */
  val detections: MutableStateFlow<List<Classifier.Recognition>> = MutableStateFlow(emptyList())

  fun publishRecognitions(recognitions: MutableList<Classifier.Recognition>) {
    detections.value = recognitions
  }

  // private val timeUtils by lazy { timeUtils }
  protected val assetReader by lazy { AssetReader(app.applicationContext) }

  // TODO MOVE
  /** Controlling navigation mode */
  // val localization = MutableStateFlow(Localization.stopped)
  // val localizationFlow = localization.asStateFlow()
  // // CV WINDOW: on Localization/Logging the detections are grouped per scanning window,
  // // e.g., each window might be 5seconds.
  // /** related to cv scan window */
  // var currentTime : Long = 0
  // var windowStart : Long = 0
  // /** Detections for the localization scan-window */
  // val detectionsLocalization: MutableStateFlow<List<Detector.Detection>> = MutableStateFlow(emptyList())
  // /** Last Anyplace location */
  // val location: MutableStateFlow<LocalizationResult> = MutableStateFlow(LocalizationResult.Unset())
  // /** Selected [Space] */
  // var space: Space? = null
  // /** All floors of the selected [space]*/
  // var floors: Floors? = null
  // /** Selected [Space] ([SpaceHelper]) */
  // lateinit var spaceH: SpaceHelper
  // /** floorsH of selected [spaceH] */
  // lateinit var floorsH: FloorsHelper
  // TODO rebuild FLoorH and floor
  // /** Selected floorH of [floorsH] */
  // var floorH: FloorHelper? = null
  // /** Selected floor/deck ([Floor]) of [space] */
  // var floor: MutableStateFlow<Floor?> = MutableStateFlow(null)
  /** LastVals: user last selections regarding a space.
   * Currently not much use (for a field var), but if we have multiple
   * lastVals for space then it would make sense. */
  // var lastValSpaces: LastValSpaces = LastValSpaces()
  // /** Initialized onMapReady */
  // var markers : Markers? = null
  // // val floorplanResp: MutableLiveData<NetworkResult<Bitmap>> = MutableLiveData()
  // val floorplanFlow : MutableStateFlow<NetworkResult<Bitmap>> = MutableStateFlow(Error(null))
  // // MutableStateFlow(false)
  // // MutableStateFlow(false)
  // /** Holds the functionality of a [CvMap] and can generate the [CvMapFast] */
  // var cvMapH: CvMapHelper? = null

  //// YOLO TODO


  public val model = YoloConstants.DETECTION_MODEL

  // CHECK FOR CLR:
  // fun imageConvertedIsSetUpped(): Boolean { return imageConverter != null }
  // @SuppressLint("UnsafeOptInUsageError")
  // fun setUpImageConverter(context: Context, image: ImageProxy) {
  //   // LOG.V3(TAG, "Image size : ${image.width}x${image.height}")
  //   // imageConverter = RenderScriptImageToBitmapConverter(context, image.image!!)
  // }
  // protected fun rotateImage(bitmap: Bitmap, degrees: Float): Bitmap {
  //   val matrix = Matrix().apply { postRotate(degrees) }
  //   return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
  // }

  // TODO MOVE
  //// FLOOR PLANS
  // fun getFloorplanFromRemote(FH: FloorHelper) = viewModelScope.launch { getFloorplanSafeCall(FH) }
  // private fun loadFloorplanFromAsset() {
  //   LOG.W(TAG, "loading from asset file")
  //   val base64 = assetReader.getFloorplan64Str()
  //   val bitmap = base64?.let { ImgUtils.stringToBitmap(it) }
  //   floorplanFlow.value =
  //           when (bitmap) {
  //             null -> Error("Cant read asset deckplan.")
  //             else -> NetworkResult.Success(bitmap)
  //           }
  // }

  /**
   * Requests a floorplan from remote and publishes outcome to [floorplanFlow].
   */
  // private suspend fun getFloorplanSafeCall(FH: FloorHelper) {
  //   floorplanFlow.value = NetworkResult.Loading()
  //   // loadFloorplanFromAsset()
  //   if (app.hasInternetConnection()) {
  //     val bitmap = FH.requestRemoteFloorplan()
  //     if (bitmap != null) {
  //       floorplanFlow.value = NetworkResult.Success(bitmap)
  //       FH.cacheFloorplan(bitmap)
  //     } else {
  //       val msg ="Failed to get ${FH.spaceH.prettyFloorplan}. "
  //       "Base URL: ${retrofitHolder.retrofit.baseUrl()}"
  //       LOG.E(msg)
  //       floorplanFlow.value = Error(msg)
  //     }
  //   } else {
  //     floorplanFlow.value = Error("No Internet Connection.")
  //   }
  // }

  //// GOOGLE MAPS
  // fun addMarker(latLng: LatLng, msg: String) {
  //   markers?.addCvMarker(latLng, msg)
  // }
  //
  // fun hideActiveMarkers() {
  //   markers?.hideActiveMakers()
  // }

  /**
   * Sets a new marker location on the map.
   */
  // fun setUserLocation(coord: Coord) {
  //   LOG.D(TAG, "setUserLocation")
  //   markers?.setLocationMarker(toLatLng(coord))
  // }

  // fun stopLocalization(mapView: MapView) { // CLR:PM
  // }
  // protected abstract fun prefWindowLocalizationMillis(): Int

  /**
   * Update [detections] that are related only to the localization window.
   */
  // protected fun updateDetectionsLocalization(detections: List<Detector.Detection>) {
  //   currentTime = System.currentTimeMillis()
  //   val appendedDetections = detectionsLocalization.value + detections
  //
  //   when {
  //     currentTime-windowStart > prefWindowLocalizationMillis() -> { // window finished
  //       localization.tryEmit(Localization.stopped)
  //       location.value = LocalizationResult.Unset()
  //       if (appendedDetections.isNotEmpty()) {
  //         LOG.W(TAG_METHOD, "stop: objects: ${appendedDetections.size}")
  //         val detectionsDedup = YoloV4Detector.NMS(appendedDetections, detector.labels, 0.01f)
  //         detectionsLocalization.value = detectionsDedup
  //         LOG.W(TAG_METHOD, "stop: objects: ${detectionsDedup.size} (dedup)")
  //
  //         location.value = LocalizationResult.Unset()
  //         if (cvMapH == null) {
  //           location.value = LocalizationResult.Error("No CvMap on device", "create one with object logging")
  //         } else {  // estimate and publish position
  //           location.value = cvMapH!!.cvMapFast.estimatePosition(
  //                   detector.getDetectionModel(),
  //                   detectionsLocalization.value)
  //         }
  //
  //         detectionsLocalization.value = emptyList()
  //       } else {
  //         LOG.W(TAG_METHOD, "stopped. no detections..")
  //         location.value = LocalizationResult.Error("Location not found.", "no objects detected")
  //       }
  //     } else -> {  // Within a window
  //     detectionsLocalization.value = appendedDetections as MutableList<Detector.Detection>
  //     LOG.D5(TAG_METHOD, "append: ${appendedDetections.size}")
  //   }
  //   }
  //
  // }

  // /** Go one floor up */
  // fun floorGoUp() {
  //   val floorNumStr = floor.value?.floorNumber.toString()
  //   if (floorsH.canGoUp(floorNumStr) == true) {
  //     val to = floorsH.getFloorAbove(floorNumStr)
  //     LOG.D(TAG_METHOD, "from: ${floor.value} to: $to")
  //     floor.value = to
  //   } else {
  //     LOG.W(TAG_METHOD, "Cannot go further up.")
  //   }
  // }

  // /** Go one floor down */
  // fun floorGoDown() {
  //   val floorNumStr = floor.value?.floorNumber.toString()
  //   if (floorsH.canGoDown(floorNumStr) == true) {
  //     val to = floorsH.getFloorBelow(floorNumStr)
  //     LOG.D(TAG_METHOD, "from: ${floor.value} to: $to")
  //     floor.value = to
  //   } else {
  //     LOG.W(TAG_METHOD, "Cannot go further down.")
  //   }
  // }

  /**
   * Selects the first available floor, or the last floor that was picked
   * for a particular space.
   */
  // fun selectInitialFloor(ctx: Context) {
  //   LOG.E()
  //   // val spaceH = spaceH!!
  //   // val floorsH = floorsH!!
  //
  //   if (!floorsH.hasFloors()) {  // space has no floors
  //     val msg = "Selected ${spaceH.prettyTypeCapitalize} has no ${spaceH.prettyFloors}."
  //     LOG.E(TAG_METHOD, msg)
  //     Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
  //     floor.value = null
  //   }
  //
  //   // var floor : Floor? = null
  //   // START OF: select floor: last selection or first available
  //   // VMb.floor.value = null
  //   if (spaceH.hasLastValuesCached()) {
  //     val lastVal = spaceH.loadLastValues()
  //     if (lastVal.lastFloor!=null) {
  //       LOG.D2(TAG_METHOD, "lastVal cache: ${spaceH.prettyFloor}${lastVal.lastFloor}.")
  //       floor.value = floorsH.getFloor(lastVal.lastFloor!!)
  //     }
  //     lastValSpaces = lastVal
  //   }
  //
  //   if (floor.value == null)  {
  //     LOG.D2(TAG_METHOD, "Loading first ${spaceH.prettyFloor}.")
  //     floor.value = floorsH.getFirstFloor()
  //   }
  //
  //   LOG.D(TAG_METHOD, "Selected ${spaceH.prettyFloor}: ${floor.value!!.floorNumber}")
  // }

}