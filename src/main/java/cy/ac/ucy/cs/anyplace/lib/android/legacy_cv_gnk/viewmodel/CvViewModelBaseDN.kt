// package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.gnk
//
// import android.annotation.SuppressLint
// import android.app.Application
// import android.content.Context
// import android.content.res.AssetManager
// import android.graphics.Bitmap
// import android.graphics.Matrix
// import android.util.DisplayMetrics
// import android.view.Surface
// import android.view.View
// import android.widget.Toast
// import androidx.camera.core.ImageProxy
// import androidx.camera.view.PreviewView
// import androidx.lifecycle.AndroidViewModel
// import androidx.lifecycle.viewModelScope
// import com.google.android.gms.maps.model.LatLng
// import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
// import cy.ac.ucy.cs.anyplace.lib.android.utils.cv.enums.YoloConstants
// import cy.ac.ucy.cs.anyplace.lib.android.utils.cv.tensorflow.legacy.gnk.utils.misc.DetectionProcessor
// import cy.ac.ucy.cs.anyplace.lib.android.utils.cv.tensorflow.legacy.gnk.utils.Detector
// import cy.ac.ucy.cs.anyplace.lib.android.utils.cv.tensorflow.legacy.gnk.utils.DetectorFactory
// import cy.ac.ucy.cs.anyplace.lib.android.utils.cv.tensorflow.legacy.gnk.utils.YoloV4Detector
// import cy.ac.ucy.cs.anyplace.lib.android.utils.cv.tensorflow.legacy.gnk.utils.ImageToBitmapConverter
// import cy.ac.ucy.cs.anyplace.lib.android.utils.cv.tensorflow.legacy.gnk.utils.RenderScriptImageToBitmapConverter
// import cy.ac.ucy.cs.anyplace.lib.android.utils.cv.tensorflow.legacy.gnk.utils.visualization.TrackingOverlayView
// import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
// import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.CvMapHelper
// import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.FloorHelper
// import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.FloorsHelper
// import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.SpaceHelper
// import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
// import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
// import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
// import cy.ac.ucy.cs.anyplace.lib.android.maps.Markers
// import cy.ac.ucy.cs.anyplace.lib.android.utils.demo.AssetReader
// import cy.ac.ucy.cs.anyplace.lib.android.utils.net.RetrofitHolderAP
// import cy.ac.ucy.cs.anyplace.lib.android.utils.utlImg
// import cy.ac.ucy.cs.anyplace.lib.android.utils.utlLoc
// import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationResult
// import cy.ac.ucy.cs.anyplace.lib.anyplace.models.*
// import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
// import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult.Error
// import kotlinx.coroutines.Dispatchers
// import kotlinx.coroutines.delay
// import kotlinx.coroutines.flow.MutableStateFlow
// import kotlinx.coroutines.flow.asStateFlow
// import kotlinx.coroutines.launch
//
// /**
//  * Localization is generally an one-time call. It gets a list of objects from the camera,
//  * and calculates the user location.
//  *
//  * However, YOLO (and it's camera-related components) operate asynchronously in the background,
//  * and store detection lists in 'scanning windows'.
//  * Therefore we need the below states.
//  */
// // enum class Localization {
// //   running,
// //   stopped,
// //   stoppedNoDetections,
// // }
//
// /**
//  * Sharing functionality between [CvLoggerViewModelRM] and TODO:PM [CvNavigatorViewModel], like:
//  * - YOLO-related setup
//  * - floorplan fetching
//  * - gmap markers
//  */
// @Deprecated("")
// abstract class CvViewModelBase constructor(
//         /** [application] is not an [AnyplaceApp], hence it is not a field.
//       [AnyplaceApp] can be used within the class as app through an Extension function */
//   application: Application,
//         val repoAP: RepoAP,
//         val retrofitHolderAP: RetrofitHolderAP): AndroidViewModel(application) {
//
//   // companion object {
//   //   /** Surface.ROTATION_0: portrait, Surface.ROTATION_270: landscape */
//   //   const val CAMERA_ROTATION: Int = Surface.ROTATION_0
//   // }
//
//   // // private val timeUtils by lazy { timeUtils }
//   // protected val assetReader by lazy { AssetReader(app.applicationContext) }
//   //
//   // /** Controlling navigation mode */
//   // val localization = MutableStateFlow(Localization.stopped)
//   // val localizationFlow = localization.asStateFlow()
//   //
//   // // CV WINDOW: on Localization/Logging the detections are grouped per scanning window,
//   // // e.g., each window might be 5seconds.
//   // /** related to cv scan window */
//   // var currentTime : Long = 0
//   // var windowStart : Long = 0
//   //
//   // /** Detections for the localization scan-window */
//   // val detectionsLocalization: MutableStateFlow<List<Detector.Detection>> = MutableStateFlow(emptyList())
//
//   /** Last Anyplace location */
//   // val location: MutableStateFlow<LocalizationResult> = MutableStateFlow(LocalizationResult.Unset())
//   //
//   // /** Selected [Space] */
//   // var space: Space? = null
//   // /** All floors of the selected [space]*/
//   // var floors: Floors? = null
//   //
//   // /** Selected [Space] ([SpaceHelper]) */
//   // lateinit var spaceH: SpaceHelper
//   //
//   // /** floorsH of selected [spaceH] */
//   // lateinit var floorsH: FloorsHelper
//   //
//   // // TODO rebuild FLoorH and floor
//   // /** Selected floorH of [floorsH] */
//   // var floorH: FloorHelper? = null
//   //
//   // /** Selected floor/deck ([Floor]) of [space] */
//   // var floor: MutableStateFlow<Floor?> = MutableStateFlow(null)
//
//
//   /** LastVals: user last selections regarding a space.
//    * Currently not much use (for a field var), but if we have multiple
//    * lastVals for space then it would make sense. */
//   // var lastValSpaces: LastValSpaces = LastValSpaces()
//   //
//   // /** Initialized onMapReady */
//   // var markers : Markers? = null
//
//   // val floorplanFlow : MutableStateFlow<NetworkResult<Bitmap>> = MutableStateFlow(Error(null))
//   // /** Holds the functionality of a [CvMap] and can generate the [CvMapFast] */
//   // var cvMapH: CvMapHelper? = null
//
//   //// YOLO
//   // protected lateinit  var detectionProcessor: DetectionProcessor
//   // internal lateinit var detector: YoloV4Detector
//   // protected var imageConverter: ImageToBitmapConverter? = null
//
//   // GNK
//   // fun setUpDetectionProcessor(
//   //   assetManager: AssetManager,
//   //   displayMetrics: DisplayMetrics,
//   //   trackingOverlayView: TrackingOverlayView,
//   //   previewView: PreviewView) = viewModelScope.launch(Dispatchers.Main) {
//   //     // GNK
//   //   // use: prefs.expImagePadding.
//   //   // this setting is experimental anyway. It also has to be read after the preferences are initialized
//   //   // val usePadding = false
//   //   // detector = DetectorFactory.createDetector(
//   //   //   assetManager,
//   //   //   YoloConstants.DETECTION_MODEL_LOGGER,
//   //   //   YoloConstants.MINIMUM_SCORE,
//   //   //   usePadding) as YoloV4Detector
//   //   // detectionProcessor = DetectionProcessor(
//   //   //   displayMetrics = displayMetrics,
//   //   //   detector = detector,
//   //   //   trackingOverlay = trackingOverlayView)
//   //   // while (previewView.childCount == 0) { delay(200) }
//   //   // val surfaceView: View = previewView.getChildAt(0)
//   //   // while (surfaceView.height == 0) { delay(200) } // BUGFIX
//   //   // detectionProcessor.initializeTrackingLayout(
//   //   //   previewWidth = surfaceView.width,
//   //   //   previewHeight =  surfaceView.height,
//   //   //   cropSize = detector.getDetectionModel().inputSize,
//   //   //   rotation = CAMERA_ROTATION)
//   // }
//
//   // CHECK GNK specific?
//   // fun imageConvertedIsSetUpped(): Boolean { return imageConverter != null }
//   // @SuppressLint("UnsafeOptInUsageError")
//   // fun setUpImageConverter(context: Context, image: ImageProxy) {
//   //   LOG.V3(TAG, "Image size : ${image.width}x${image.height}")
//   //   imageConverter = RenderScriptImageToBitmapConverter(context, image.image!!)
//   // }
//
//   // protected fun rotateImage(bitmap: Bitmap, degrees: Float): Bitmap {
//   //   val matrix = Matrix().apply { postRotate(degrees) }
//   //   return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
//   // }
//
//   //// FLOOR PLANS
//
//   // fun getFloorplanFromRemote(FH: FloorHelper) = viewModelScope.launch { getFloorplanSafeCall(FH) }
//
//   // private fun loadFloorplanFromAsset() {
//   //   LOG.W(TAG, "loading from asset file")
//   //   val base64 = assetReader.getFloorplan64Str()
//   //   val bitmap = base64?.let { utlImg.decodeBase64(it) }
//   //   floorplanFlow.value =
//   //     when (bitmap) {
//   //       null -> Error("Cant read asset deckplan.")
//   //       else -> NetworkResult.Success(bitmap)
//   //     }
//   // }
//
//   /**
//    * Requests a floorplan from remote and publishes outcome to [floorplanFlow].
//    */
//   // private suspend fun getFloorplanSafeCall(FH: FloorHelper) {
//   //   floorplanFlow.value = NetworkResult.Loading()
//   //   // loadFloorplanFromAsset()
//   //   if (app.hasInternet()) {
//   //     val bitmap = FH.requestRemoteFloorplan()
//   //     if (bitmap != null) {
//   //       floorplanFlow.value = NetworkResult.Success(bitmap)
//   //       FH.cacheFloorplan(bitmap)
//   //     } else {
//   //       val msg ="Failed to get ${FH.spaceH.prettyFloorplan}. "
//   //       "Base URL: ${retrofitHolderAP.retrofit.baseUrl()}"
//   //       LOG.E(msg)
//   //       floorplanFlow.value = Error(msg)
//   //     }
//   //   } else {
//   //     floorplanFlow.value = Error("No Internet Connection.")
//   //   }
//   // }
//
//   // MERGE:PM are these ok?
//   //// GOOGLE MAPS
//   // maybe OK to delete..
//   // fun addMarker(latLng: LatLng, msg: String) {
//   //   markers?.addCvMarker(latLng, msg)
//   // }
//   //
//   // fun hideActiveMarkers() {
//   //   markers?.hideCvObjMarkers()
//   // }
//
//   /**
//    * Sets a new marker location on the map.
//    */
//   // fun setUserLocation(coord: Coord) {
//   //   LOG.D(TAG, "setUserLocation")
//   //  markers?.setLocationMarker(utlLoc.toLatLng(coord))
//   // }
//
//   // fun stopLocalization(mapView: MapView) { // CLR:PM
//   // }
//   // protected abstract fun prefWindowLocalizationMillis(): Int
//   /**
//    * Update [detections] that are related only to the localization window.
//    */
//   // protected fun updateDetectionsLocalization(detections: List<Detector.Detection>) {
//   //   currentTime = System.currentTimeMillis()
//   //   val appendedDetections = detectionsLocalization.value + detections
//   //
//   //   when {
//   //     currentTime-windowStart > prefWindowLocalizationMillis() -> { // window finished
//   //       localization.tryEmit(Localization.stopped)
//   //       location.value = LocalizationResult.Unset()
//   //       if (appendedDetections.isNotEmpty()) {
//   //         LOG.W(TAG_METHOD, "stop: objects: ${appendedDetections.size}")
//   //         val detectionsDedup = YoloV4Detector.NMS(appendedDetections, detector.labels, 0.01f)
//   //         detectionsLocalization.value = detectionsDedup
//   //         LOG.W(TAG_METHOD, "stop: objects: ${detectionsDedup.size} (dedup)")
//   //
//   //         location.value = LocalizationResult.Unset()
//   //         if (cvMapH == null) {
//   //           location.value = LocalizationResult.Error("No CvMap on device", "create one with object logging")
//   //         } else {  // estimate and publish position
//   //           location.value = cvMapH!!.cvMapFast.estimatePosition(
//   //                   detector.getDetectionModel(),
//   //                   detectionsLocalization.value)
//   //         }
//   //
//   //         detectionsLocalization.value = emptyList()
//   //       } else {
//   //         LOG.W(TAG_METHOD, "stopped. no detections..")
//   //         location.value = LocalizationResult.Error("Location not found.", "no objects detected")
//   //       }
//   //     } else -> {  // Within a window
//   //       detectionsLocalization.value = appendedDetections as MutableList<Detector.Detection>
//   //       LOG.D5(TAG_METHOD, "append: ${appendedDetections.size}")
//   //     }
//   //   }
//   // }
//
//   // /** Go one floor up */
//   // fun floorGoUp() {
//   //   val floorNumStr = floor.value?.floorNumber.toString()
//   //   if (floorsH.canGoUp(floorNumStr)) {
//   //     val to = floorsH.getFloorAbove(floorNumStr)
//   //     LOG.D(TAG_METHOD, "from: ${floor.value?.floorNumber} to: $to")
//   //     floor.value = to
//   //   } else {
//   //     LOG.W(TAG_METHOD, "Cannot go further up.")
//   //   }
//   // }
//
//   /** Go one floor down */
//   // fun floorGoDown() {
//   //   val floorNumStr = floor.value?.floorNumber.toString()
//   //   if (floorsH.canGoDown(floorNumStr)) {
//   //     val to = floorsH.getFloorBelow(floorNumStr)
//   //     LOG.D(TAG_METHOD, "from: ${floor.value?.floorName} to: $to")
//   //     floor.value = to
//   //   } else {
//   //     LOG.W(TAG_METHOD, "Cannot go further down.")
//   //   }
//   // }
//
//   /**
//    * Selects the first available floor, or the last floor that was picked
//    * for a particular space.
//    */
//   // fun selectInitialFloor(ctx: Context) {
//   //   LOG.V3()
//   //   if (!floorsH.hasFloors()) {  // space has no floors
//   //     val msg = "Selected ${spaceH.prettyTypeCapitalize} has no ${spaceH.prettyFloors}."
//   //     LOG.E(TAG_METHOD, msg)
//   //     Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
//   //     floor.value = null
//   //   }
//   //
//   //   // var floor : Floor? = null
//   //   // START OF: select floor: last selection or first available
//   //   // VMb.floor.value = null
//   //   if (spaceH.hasLastValuesCached()) {
//   //     val lastVal = spaceH.loadLastValues()
//   //     if (lastVal.lastFloor!=null) {
//   //       LOG.V2(TAG_METHOD, "lastVal cache: ${spaceH.prettyFloor}${lastVal.lastFloor}.")
//   //       floor.value = floorsH.getFloor(lastVal.lastFloor!!)
//   //     }
//   //     lastValSpaces = lastVal
//   //   }
//   //
//   //   if (floor.value == null)  {
//   //     LOG.D3(TAG_METHOD, "Loading first ${spaceH.prettyFloor}.")
//   //     floor.value = floorsH.getFirstFloor()
//   //   }
//   //
//   //   LOG.V2(TAG_METHOD, "Selected ${spaceH.prettyFloor}: ${floor.value!!.floorNumber}")
//   // }
//
// }