package cy.ac.ucy.cs.anyplace.lib.android.ui.cv

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.common.util.concurrent.ListenableFuture
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.visualization.TrackingOverlayView
import cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers.FloorHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers.FloorsHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers.SpaceHelper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.maps.Markers
import cy.ac.ucy.cs.anyplace.lib.android.maps.Overlays
import cy.ac.ucy.cs.anyplace.lib.android.maps.camera.CameraAndViewport
import cy.ac.ucy.cs.anyplace.lib.android.utils.demo.AssetReader
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.CvViewModelBase
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.CvViewModelBase.Companion.CAMERA_ROTATION
import cy.ac.ucy.cs.anyplace.lib.models.Coord
import cy.ac.ucy.cs.anyplace.lib.models.Floor
import cy.ac.ucy.cs.anyplace.lib.models.Floors
import cy.ac.ucy.cs.anyplace.lib.models.Space
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Sharing code between [CvLoggerActivity] and [CvNavigatorActivity]
 * This mostly is:
 * - YOLO setup
 * - gmap setup
 * - floorplan/overlay displaying
 *
 */
@AndroidEntryPoint
abstract class CvActivityBase : AppCompatActivity(), OnMapReadyCallback {
  companion object {
    const val CAMERA_REQUEST_CODE: Int = 1
    const val CAMERA_ASPECT_RATIO: Int = AspectRatio.RATIO_4_3 // AspectRatio.RATIO_16_9
    const val OPACITY_MAP_LOGGING = 0f
    const val ANIMATION_DELAY : Long = 100
  }

  /** Base [ViewModel] class: [CvViewModelBase] */
  protected lateinit var VMb: CvViewModelBase
  protected lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
  protected lateinit var gmap: GoogleMap
  protected val overlays by lazy { Overlays(applicationContext) }
  protected val assetReader by lazy { AssetReader(applicationContext) }

  private lateinit var tovCamera: TrackingOverlayView
  private lateinit var pvCamera: PreviewView

  /** Setups the Computer Vision processor using
   * - a [TrackingOverlayView] that shows the detected objects
   * - a [PreviewView] that shows the camera
   *
   * ** Part of YOLO setup **
   */
  protected fun setupComputerVision(_tovCamera: TrackingOverlayView, _pvCamera: PreviewView) {
    tovCamera = _tovCamera
    pvCamera = _pvCamera // must be set before requesting permissions
    cameraProviderFuture = ProcessCameraProvider.getInstance(baseContext)
    requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)

    VMb.setUpDetectionProcessor(
      assets,
      resources.displayMetrics,
      tovCamera,
      pvCamera)
  }


  /**
   * Binds the camera preview once the camera permission was granted
   *
   * ** Part of Yolo Setup **
   */
  fun bindPreview() {
    val preview: Preview = Preview.Builder()
        .setTargetAspectRatio(CAMERA_ASPECT_RATIO)
        .setTargetRotation(CAMERA_ROTATION)
        .build()

    val cameraSelector: CameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
        .build()

    preview.setSurfaceProvider(pvCamera.surfaceProvider)

    val imageAnalysis = ImageAnalysis.Builder()
        .setTargetAspectRatio(CAMERA_ASPECT_RATIO)
        .setTargetRotation(CAMERA_ROTATION)
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()

    imageAnalysis.setAnalyzer(
      ContextCompat.getMainExecutor(applicationContext),
      this::analyzeImage)

    val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
    cameraProvider.bindToLifecycle(
      this as LifecycleOwner,
      cameraSelector,
      imageAnalysis,
      preview)
  }

  protected abstract fun analyzeImage(image: ImageProxy)

  override fun onRequestPermissionsResult(
    requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    when (requestCode) {
      CAMERA_REQUEST_CODE -> {
        val indexOfCameraPermission = permissions.indexOf(Manifest.permission.CAMERA)
        if (grantResults[indexOfCameraPermission] == PackageManager.PERMISSION_GRANTED) {
          cameraProviderFuture.addListener(
            this@CvActivityBase::bindPreview, ContextCompat.getMainExecutor(baseContext))
        } else {
          val msg = "Permissions not granted."
          Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
          finish()
        }
      }
    }
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
  }

   fun setUpImageConverter(image: ImageProxy) {
    LOG.D2("Frame: ${image.width}x${image.height}")
    VMb.setUpImageConverter(baseContext, image)
  }

  /**
   * Attaches a map dynamically
   */
  protected fun setupMap() {
    val mapFragment = SupportMapFragment.newInstance()
    supportFragmentManager
        .beginTransaction()
        .add(R.id.mapView, mapFragment)
        .commit()
    mapFragment.getMapAsync(this)
  }

  override fun onMapReady(googleMap: GoogleMap) {
    LOG.D(TAG, "onMapReady")
    gmap = googleMap
    VMb.markers = Markers(applicationContext, gmap)

    val maxZoomLevel = gmap.maxZoomLevel // may be different from device to device
    // map.addMarker(MarkerOptions().position(latLng).title("Ucy Building"))
    // map.moveCamera(CameraUpdateFactory.newLatLng(latLng))

    // TODO Space has to be sent to this activity (SafeArgs?) using a previous "Select Space" activity.
    // (maybe using Bundle is easier/better)
    loadSpaceAndFloorFromAssets()

    // TODO:PM this must be moved to earlier activity
    // along with Space/Floors loading (that also needs implementation).
    lifecycleScope.launch(Dispatchers.IO) { VMb.floorsH?.fetchAllFloorplans() }

    // place some restrictions on the map
    gmap.moveCamera(CameraUpdateFactory.newCameraPosition(
      CameraAndViewport.loggerCamera(VMb.spaceH!!.latLng(), maxZoomLevel)))
    gmap.setMinZoomPreference(maxZoomLevel-3)

    // restrict screen to current bounds.
    lifecycleScope.launch {
      delay(500) // CHECK does this fix it?
      // delay(500) // CLR:PM
      // if (floorH == null) { // BUGFIX CLR:PM
      //   LOG.E("floorH is null")
      // }

      gmap.moveCamera(CameraUpdateFactory.newLatLngBounds(VMb.floorH?.bounds(), 0))
      val floorOnScreenBounds = gmap.projection.visibleRegion.latLngBounds
      LOG.D2("bounds: ${floorOnScreenBounds.center}")
      gmap.setLatLngBoundsForCameraTarget(VMb.floorH?.bounds())
      readFloorplan(VMb.floorH!!)
    }

    gmap.uiSettings.apply {
      isZoomControlsEnabled = false
      isMapToolbarEnabled = false
      isTiltGesturesEnabled = false
      isCompassEnabled = false
      isIndoorLevelPickerEnabled = true
    }
    onMapReadySpecialize()
  }

  /**
   * callback for additional functionality to the [CvActivityBase]-based classes
   */
  protected abstract fun onMapReadySpecialize()

  /**
   * Loads from assets the Space and the Space's Floors
   * Then it loads the floorplan for [selectedFloorPlan].
   *
   * TODO Implement this from network (in an earlier activity), and
   * pass it here through [SafeArgs] or [Bundle]
   */
  fun loadSpaceAndFloorFromAssets() {
    LOG.V2()
    VMb.space = assetReader.getSpace()
    VMb.floors = assetReader.getFloors()

    if (VMb.space == null || VMb.floors == null) {
      showError(VMb.space, VMb.floors)
      return
    }

    VMb.spaceH = SpaceHelper(applicationContext, VMb.repository, VMb.space!!)
    VMb.floorsH = FloorsHelper(VMb.floors!!, VMb.spaceH!!)

    LOG.D(TAG_METHOD, "Selected ${VMb.spaceH?.prettyType}: ${VMb.space!!.name}")
    LOG.D(TAG_METHOD, "${VMb.spaceH!!.prettyType} has ${VMb.floors!!.floors.size} ${VMb.spaceH!!.prettyFloors}.")

    if (!VMb.floorsH!!.hasFloors()) {
      val msg = "Selected ${VMb.spaceH!!.prettyType} has no ${VMb.spaceH!!.prettyFloors}."
      LOG.E(TAG_METHOD, msg)
      Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
      return
    }

    // TODO:PM put a floor selector...
    // START OF: select floor: last selection or first available
    var floor : Floor? = null
    if (VMb.spaceH!!.hasLastValuesCached()) {
      val lv = VMb.spaceH!!.loadLastValues()
      if (lv.lastFloor!=null) {
        LOG.D2(TAG_METHOD, " lastVal: cached ${VMb.spaceH!!.prettyFloor}${lv.lastFloor}.")
        floor = VMb.floorsH!!.getFloor(lv.lastFloor!!)
      }
      VMb.lastValSpaces = lv
    }

    if (floor == null)  {
      LOG.D2(TAG_METHOD, "Loading first ${VMb.spaceH!!.prettyFloor}.")
      floor = VMb.floorsH!!.getFirstFloor()
    }
    // END OF: select floor: last selection or first available

    val selectedFloorNum=floor.floorNumber.toInt()
    // FIXED SELECTION:
    // val selectedFloorNum=3
    // floor = floorsH!!.getFloor(selectedFloorNum)
    if (floor == null) {
      showError(VMb.space, VMb.floors, floor, selectedFloorNum)
      return
    }

    LOG.D(TAG_METHOD, "Selected ${VMb.spaceH?.prettyFloor}: ${floor?.floorName}")

    VMb.floorH = FloorHelper(floor, VMb.spaceH!!)
    updateAndCacheLastFloor(floor) // TODO:PM ONLY when changing floor..
  }

  private fun updateAndCacheLastFloor(floor: Floor) {
    VMb.lastValSpaces.lastFloor=floor.floorNumber
    VMb.spaceH?.cacheLastValues(VMb.lastValSpaces)
  }

  protected fun showError(space: Space?, floors: Floors?, floor: Floor? = null, floorNum: Int = 0) {
    var msg = ""
    when {
      space == null -> msg = "No space selected."
      floors == null -> msg = "Failed to get ${VMb.spaceH?.prettyFloors}."
      floor == null -> msg = "Failed to get ${VMb.spaceH?.prettyFloor} $floorNum."
    }
    LOG.E(msg)
    Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
  }

  /**
   * Reads a floorplan (from cache or remote) using the [VM] and a [FloorHelper]
   * Once it's read, then it is loaded it is posted on [VMb.floorplanResp],
   * and through an observer it is loaded on the map.
   *
   * Must be called each time wee want to load a floor.
   */
  protected fun readFloorplan(FH: FloorHelper) {
    LOG.D(TAG, "readFloorplan")
    lifecycleScope.launch {
      if (FH.hasFloorplanCached()) {
        LOG.D(TAG, "readFloorplan: cache")
        val localResult =
          when (val bitmap = FH.loadFromCache()) {
            null -> NetworkResult.Error("Failed to load from local cache")
            else -> NetworkResult.Success(bitmap)
          }
        VMb.floorplanResp.postValue(localResult)
      } else {
        LOG.D2(TAG, "readFloorplan: remote")
        VMb.getFloorplan(FH)
      }
    }

    VMb.floorplanResp.observeForever { response ->
      when (response) {
        is NetworkResult.Loading -> {
          LOG.W("Loading ${VMb.spaceH?.prettyFloorplan}")
        }
        is NetworkResult.Error -> {
          val msg = "Failed to get $VMb.space"
          LOG.W(msg)
          Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        }
        is NetworkResult.Success -> {
          if (VMb.floorH == null) {
            val msg = "No selected floor/deck."
            LOG.W(msg)
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
          } else {
            renderFloorplan(response.data, VMb.floorH!!)
          }
        }
      }
    }
  }

  protected fun renderFloorplan(bitmap: Bitmap?, FH: FloorHelper) {
    LOG.D("renderFloorplan:")
    overlays.addFloorplan(bitmap, gmap, FH.bounds())
  }

  protected fun checkInternet() {
    if (!app.hasInternetConnection()) {
      // TODO method that updates ui based on internet connectivity: gray out settings button
      Toast.makeText(applicationContext, "No internet connection.", Toast.LENGTH_LONG).show()
    }
  }

}