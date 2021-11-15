package cy.ac.ucy.cs.anyplace.lib.android.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.DisplayMetrics
import android.view.Surface
import android.view.View
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.cv.misc.Constants
import cy.ac.ucy.cs.anyplace.lib.android.cv.misc.DetectionProcessor
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.DetectorFactory
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.YoloV4Detector
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.utils.ImageToBitmapConverter
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.utils.RenderScriptImageToBitmapConverter
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.visualization.TrackingOverlayView
import cy.ac.ucy.cs.anyplace.lib.android.data.Repository
import cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers.FloorHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers.FloorsHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers.SpaceHelper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.maps.Markers
import cy.ac.ucy.cs.anyplace.lib.android.utils.ImgUtils
import cy.ac.ucy.cs.anyplace.lib.android.utils.demo.AssetReader
import cy.ac.ucy.cs.anyplace.lib.android.utils.network.RetrofitHolder
import cy.ac.ucy.cs.anyplace.lib.models.Floor
import cy.ac.ucy.cs.anyplace.lib.models.Floors
import cy.ac.ucy.cs.anyplace.lib.models.LastValSpaces
import cy.ac.ucy.cs.anyplace.lib.models.Space
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Sharing functionality between [CvLoggerViewModel] and TODO:PM [CvNavigatorViewModel], like:
 * - YOLO-related setup
 * - floorplan fetching
 * - gmap markers
 */
// @HiltViewModel
abstract class CvViewModelBase constructor(
  // [application] is not an [AnyplaceApp], hence it is not a field.
  // [AnyplaceApp] can be used within the class as app through an Extension function
  application: Application,
  val repository: Repository,
  val retrofitHolder: RetrofitHolder): AndroidViewModel(application) {

  companion object {
    /** Surface.ROTATION_0: portrait, Surface.ROTATION_270: landscape */
    const val CAMERA_ROTATION: Int = Surface.ROTATION_0
  }

  // private val timeUtils by lazy { timeUtils }
  protected val assetReader by lazy { AssetReader(app.applicationContext) }

  /** Selected [Space] */
  var space: Space? = null
  /** All floors of the selected [space]*/
  var floors: Floors? = null
  /** Selected floor/deck ([Floor]) of [space] */
  var floor: Floor? = null
  /** Selected [Space] ([SpaceHelper]) */
  var spaceH: SpaceHelper? = null
  /** floorsH of selected [spaceH] */
  var floorsH: FloorsHelper? = null
  /** Selected floorH of [floorsH] */
  var floorH: FloorHelper? = null

  /** LastVals: user last selections regarding a space.
   * Currently not much use (for a field var), but if we have multiple
   * lastVals for space then it would make sense. */
  var lastValSpaces: LastValSpaces = LastValSpaces()

  /** Initialized onMapReady */
  var markers : Markers? = null
  val floorplanResp: MutableLiveData<NetworkResult<Bitmap>> = MutableLiveData()

  //// YOLO
  protected lateinit  var detectionProcessor: DetectionProcessor
  internal lateinit var detector: YoloV4Detector
  protected var imageConverter: ImageToBitmapConverter? = null // TODO:PM LATE INIT?

  fun setUpDetectionProcessor(
    assetManager: AssetManager,
    displayMetrics: DisplayMetrics,
    trackingOverlayView: TrackingOverlayView,
    previewView: PreviewView) = viewModelScope.launch(Dispatchers.Main) {
    // use: prefs.expImagePadding.
    // this setting is experimental anyway. It also has to be read after the preferences are initialized
    val usePadding = false

    detector = DetectorFactory.createDetector(
      assetManager,
      Constants.DETECTION_MODEL,
      Constants.MINIMUM_SCORE,
      usePadding) as YoloV4Detector

    detectionProcessor = DetectionProcessor(
      displayMetrics = displayMetrics,
      detector = detector,
      trackingOverlay = trackingOverlayView)

    while (previewView.childCount == 0) { delay(200) }
    val surfaceView: View = previewView.getChildAt(0)
    while (surfaceView.height == 0) { delay(200) } // BUGFIX

    detectionProcessor.initializeTrackingLayout(
      previewWidth = surfaceView.width,
      previewHeight =  surfaceView.height,
      cropSize = detector.getDetectionModel().inputSize,
      rotation = CAMERA_ROTATION)
  }

  fun imageConvertedIsSetUpped(): Boolean { return imageConverter != null }

  @SuppressLint("UnsafeOptInUsageError")
  fun setUpImageConverter(context: Context, image: ImageProxy) {
    LOG.V3(TAG, "Image size : ${image.width}x${image.height}")
    imageConverter = RenderScriptImageToBitmapConverter(context, image.image!!)
  }

  protected fun rotateImage(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
  }

  //// FLOOR PLANS

  fun getFloorplan(FH: FloorHelper) = viewModelScope.launch { getFloorplanSafeCall(FH) }

  private fun loadFloorplanFromAsset() {
    LOG.W(TAG, "loading from asset file")
    val base64 = assetReader.getFloorplan64Str()
    val bitmap = base64?.let { ImgUtils.stringToBitmap(it) }
    floorplanResp.value =
      when (bitmap) {
        null -> NetworkResult.Error("Cant read asset deckplan.")
        else -> NetworkResult.Success(bitmap)
      }
  }

  /**
   * Requests a floorplan from remote and publishes outcome to [floorplanResp].
   */
  private suspend fun getFloorplanSafeCall(FH: FloorHelper) {
    floorplanResp.value = NetworkResult.Loading()
    // loadFloorplanFromAsset()
    if (app.hasInternetConnection()) {
      val bitmap = FH.requestRemoteFloorplan()
      if (bitmap != null) {
        floorplanResp.value = NetworkResult.Success(bitmap)
        FH.cacheFloorplan(bitmap)
      } else {
        val msg ="Failed to get ${FH.spaceH.prettyFloorplan}. "
        "Base URL: ${retrofitHolder.retrofit.baseUrl()}"
        LOG.E(msg)
        floorplanResp.value = NetworkResult.Error(msg)
      }
    } else {
      floorplanResp.value = NetworkResult.Error("No Internet Connection.")
    }
  }

  //// GOOGLE MAPS
  fun addMarker(latLng: LatLng, msg: String) {
    markers?.addCvMarker(latLng, msg)
  }

  fun hideActiveMarkers() {
    markers?.hideActiveMakers()
  }

}