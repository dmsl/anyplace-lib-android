// MERGED?
// package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.gnk
// import android.Manifest
// import android.content.pm.PackageManager
// import android.graphics.Bitmap
// import android.widget.Toast
// import androidx.appcompat.app.AppCompatActivity
// import androidx.camera.core.*
// import androidx.camera.lifecycle.ProcessCameraProvider
// import androidx.camera.view.PreviewView
// import androidx.core.content.ContextCompat
// import androidx.lifecycle.LifecycleOwner
// import androidx.lifecycle.lifecycleScope
// import com.google.android.gms.maps.CameraUpdateFactory
// import com.google.android.gms.maps.GoogleMap
// import com.google.android.gms.maps.OnMapReadyCallback
// import com.google.android.gms.maps.SupportMapFragment
// import com.google.common.util.concurrent.ListenableFuture
// import cy.ac.ucy.cs.anyplace.lib.R
// import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
// import cy.ac.ucy.cs.anyplace.lib.android.utils.cv.tensorflow.legacy.gnk.utils.visualization.TrackingOverlayView
// import cy.ac.ucy.cs.anyplace.lib.android.data.models.helpers.*
// import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
// import cy.ac.ucy.cs.anyplace.lib.android.maps.Markers
// import cy.ac.ucy.cs.anyplace.lib.android.maps.Overlays
// import cy.ac.ucy.cs.anyplace.lib.android.maps.camera.CameraAndViewport
// import cy.ac.ucy.cs.anyplace.lib.android.ui.components.FloorSelector
// import cy.ac.ucy.cs.anyplace.lib.android.utils.demo.AssetReader
// // import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.gnk.CvViewModelBase
// // import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.gnk.CvViewModelBase.Companion.CAMERA_ROTATION
// import cy.ac.ucy.cs.anyplace.lib.anyplace.models.CvMap
// import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Floor
// import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Floors
// import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Space
// import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
// import dagger.hilt.android.AndroidEntryPoint
// import kotlinx.coroutines.Dispatchers
// import kotlinx.coroutines.delay
// import kotlinx.coroutines.flow.first
// import kotlinx.coroutines.launch

/**
 * Sharing code between [CvLoggerActivity] and [CvNavigatorActivity]
 * This mostly is:
 * - YOLO setup
 * - gmap setup
 * - floorplan/overlay displaying
 *
 */
// @AndroidEntryPoint
// @Deprecated("")
// abstract class CvActivityBaseRM : AppCompatActivity(),
//         OnMapReadyCallback {
  // companion object {
  //   const val CAMERA_REQUEST_CODE: Int = 1
  //   const val CAMERA_ASPECT_RATIO: Int = AspectRatio.RATIO_4_3 // AspectRatio.RATIO_16_9
  //   const val OPACITY_MAP_LOGGING = 0f
  //   const val ANIMATION_DELAY : Long = 100
  // }

  /** Base [ViewModel] class: [CvViewModelBase] */
  // protected lateinit var VMB: CvViewModelBase
  // protected lateinit var UIB: UiActivityCvBase
  // protected lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
  // protected lateinit var gmap: GoogleMap
  // protected val overlays by lazy { Overlays(applicationContext, lifecycleScope) }
  // protected val assetReader by lazy { AssetReader(applicationContext) }

  // private lateinit var tovCamera: TrackingOverlayView
  // private lateinit var pvCamera: PreviewView
  // protected lateinit var floorSelector: FloorSelector

  // override fun onResume() { // MERGED
  //   super.onResume()
  //   LOG.D3(TAG, "onResume")
  //   readPrefsAndContinueSetup()
  // }

  // /** MERGED
  //  * Read [dsCv] preferences
  //  * TODO load CvModel from here?
  //  */
  // private fun readPrefsAndContinueSetup() {
  //   lifecycleScope.launch {
  //     dsCv.read.first { prefs->
  //       if (prefs.reloadCvMaps) {
  //         LOG.W(TAG_METHOD, "Reloading CvMaps and caches.")
  //         // refresh CvMap+Heatmap only when needed
  //         // TODO do something similar with floorplans when necessary as well
  //         loadCvMapAndHeatmap()
  //         dsCv.setReloadCvMaps(false)
  //       }
  //
  //       true
  //     }
  //   }
  // }

  // MERGED (SKIPPED)
  /** Setups the Computer Vision processor using
   * - a [TrackingOverlayView] that shows the detected objects
   * - a [PreviewView] that shows the camera
   *
   * ** Part of YOLO setup **
   */
  // protected fun setupComputerVision(_tovCamera: TrackingOverlayView, _pvCamera: PreviewView) {
  //   tovCamera = _tovCamera
  //   pvCamera = _pvCamera // must be set before requesting permissions
  //   cameraProviderFuture = ProcessCameraProvider.getInstance(baseContext)
  //   requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
  //
  //   VMB.setUpDetectionProcessor(
  //           assets,
  //           resources.displayMetrics,
  //           tovCamera,
  //           pvCamera)
  // }
  /**
   * Binds the camera preview once the camera permission was granted
   *
   * ** Part of Yolo Setup **
   */
  // fun bindPreview() {
  //   val preview: Preview = Preview.Builder()
  //           .setTargetAspectRatio(CAMERA_ASPECT_RATIO)
  //           .setTargetRotation(CAMERA_ROTATION)
  //           .build()
  //
  //   val cameraSelector: CameraSelector = CameraSelector.Builder()
  //           .requireLensFacing(CameraSelector.LENS_FACING_BACK)
  //           .build()
  //
  //   preview.setSurfaceProvider(pvCamera.surfaceProvider)
  //
  //   val imageAnalysis = ImageAnalysis.Builder()
  //           .setTargetAspectRatio(CAMERA_ASPECT_RATIO)
  //           .setTargetRotation(CAMERA_ROTATION)
  //           .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
  //           .build()
  //
  //   imageAnalysis.setAnalyzer(
  //           ContextCompat.getMainExecutor(applicationContext),
  //           this::analyzeImage)
  //
  //   val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
  //   cameraProvider.bindToLifecycle(
  //           this as LifecycleOwner,
  //           cameraSelector,
  //           imageAnalysis,
  //           preview)
  // }
  // protected abstract fun analyzeImage(image: ImageProxy)
  // override fun onRequestPermissionsResult(
  //         requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
  //   when (requestCode) {
  //     CAMERA_REQUEST_CODE -> {
  //       val indexOfCameraPermission = permissions.indexOf(Manifest.permission.CAMERA)
  //       if (grantResults[indexOfCameraPermission] == PackageManager.PERMISSION_GRANTED) {
  //         cameraProviderFuture.addListener(
  //                 this@CvActivityBaseRM::bindPreview, ContextCompat.getMainExecutor(baseContext))
  //       } else {
  //         val msg = "Permissions not granted."
  //         Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
  //         finish()
  //       }
  //     }
  //   }
  //   super.onRequestPermissionsResult(requestCode, permissions, grantResults)
  // }

  // fun setUpImageConverter(image: ImageProxy) {
  //   LOG.D2("Frame: ${image.width}x${image.height}")
  //   VMB.setUpImageConverter(baseContext, image)
  // }
  /**
   * Attaches a map dynamically
   */
  // protected fun setupMap() {
  //   val mapFragment = SupportMapFragment.newInstance()
  //   supportFragmentManager
  //           .beginTransaction()
  //           .add(R.id.mapView, mapFragment)
  //           .commit()
  //   mapFragment.getMapAsync(this)
  // }

  // override fun onMapReady(googleMap: GoogleMap) {
  //   LOG.D(TAG, "onMapReady")
  //   gmap = googleMap
  //
  //
  //   // MERGED: GmapWrapper
  //   // VMB.markers = Markers(applicationContext, gmap)
  //   // val maxZoomLevel = gmap.maxZoomLevel // may be different from device to device
  //   // map.addMarker(MarkerOptions().position(latLng).title("Ucy Building"))
  //   // map.moveCamera(CameraUpdateFactory.newLatLng(latLng))
  //
  //   // (maybe using Bundle is easier/better)
  //   // loadSpaceAndFloor()
  //
  //   // along with Space/Floors loading (that also needs implementation).
  //   // lifecycleScope.launch(Dispatchers.IO) { VMB.floorsH.fetchAllFloorplans() }
  //
  //   // place some restrictions on the map
  //   // gmap.moveCamera(CameraUpdateFactory.newCameraPosition(
  //   //         CameraAndViewport.loggerCamera(VMB.spaceH.latLng(), maxZoomLevel)))
  //   // gmap.setMinZoomPreference(maxZoomLevel-3)
  //
  //   // restrict screen to current bounds.
  //   // lifecycleScope.launch {
  //   //   delay(500) // CHECK does this fix it?
  //   //
  //   //   if (VMB.floorH != null) {
  //   //     val bounds = VMB.floorH!!.bounds()
  //   //     gmap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0))
  //   //     val floorOnScreenBounds = gmap.projection.visibleRegion.latLngBounds
  //   //     LOG.D2("bounds: ${floorOnScreenBounds.center}")
  //   //     gmap.setLatLngBoundsForCameraTarget(bounds)
  //   //   } else {
  //   //     LOG.E(TAG, "Bounds were null. Space: ${VMB.space?.name} Floor: ${VMB.floorH?.prettyFloorName()}")
  //   //   }
  //   // }
  //
  //   // gmap.uiSettings.apply {
  //   //   isZoomControlsEnabled = true
  //   //   isMapToolbarEnabled = false
  //   //   isTiltGesturesEnabled = false
  //   //   isCompassEnabled = false
  //   //   isIndoorLevelPickerEnabled = true
  //   // }
  //   // onMapReadySpecialize()
  // }


  ////// MERGED ////////////////////////////////////////////////////////////////


  /**
   * callback for additional functionality to the [CvActivityBaseRM]-based classes
   */
  // protected abstract fun onMapReadySpecialize()

  // private fun loadSpaceAndFloorFromAssets() : Boolean { // MERGED
  //   LOG.V2()
  //   VMB.space = assetReader.getSpace()
  //   VMB.floors = assetReader.getFloors()
  //
  //   if (VMB.space == null || VMB.floors == null) {
  //     showError(VMB.space, VMB.floors)
  //     return false
  //   }
  //
  //   VMB.spaceH = SpaceHelper(applicationContext, VMB.repoAP, VMB.space!!)
  //   VMB.floorsH = FloorsHelper(VMB.floors!!, VMB.spaceH)
  //   val prettySpace = VMB.spaceH.prettyTypeCapitalize
  //   val prettyFloors= VMB.spaceH.prettyFloors
  //
  //   LOG.D3(TAG_METHOD, "$prettySpace: ${VMB.space!!.name} " +
  //           "(has ${VMB.floors!!.floors.size} $prettyFloors)")
  //
  //   return true
  // }

  /**
   * Loads from assets the Space and the Space's Floors
   * Then it loads the floorplan for [selectedFloorPlan].
   */
  // @Deprecated("MERGED") // MERGED
  // fun loadSpaceAndFloor() {
  //   // TODO: accept this as a bundle!
  //   if(!loadSpaceAndFloorFromAssets()) return
  //
  //   VMB.selectInitialFloor(applicationContext)
  //
  //   observeFloorChanges()
  //   observeFloorplanChanges()
  // }

  /**
   * Observe when [VMB.floor] changes and react accordingly:
   * - update [floorSelector] UI (the up/down buttons)
   * - store the last floor selection (for the relevant [Space])
   * - loads the floor
   */
  // @Deprecated("MERGED") // MERGED
  // protected fun observeFloorChanges() {
  //   LOG.W()
  //   lifecycleScope.launch{
  //     VMB.floor.collect { selectedFloor ->
  //
  //       // update FloorHelper & FloorSelector
  //       VMB.floorH = if (selectedFloor != null) FloorHelper(selectedFloor, VMB.spaceH) else null
  //       floorSelector.updateFloorSelector(selectedFloor, VMB.floorsH)
  //       LOG.D3(TAG, "observeFloorChanges: -> loadFloor: ${selectedFloor?.floorNumber}")
  //       if (selectedFloor != null) {
  //
  //         updateAndCacheLastFloor(VMB.floor.value)
  //         LOG.D2(TAG, "observeFloorChanges: -> loadFloor: ${selectedFloor.floorNumber}")
  //         lazilyChangeFloor()
  //       }
  //     }
  //   }
  // }


  /**
   *
   */
  // @Deprecated("MERGED") // MERGED
  // private fun lazilyChangeFloor() {
  //   LOG.W()
  //   if (floorChangeRequestTime == 0L) {
  //     floorChangeRequestTime = System.currentTimeMillis()
  //     loadFloor(VMB.floorH!!)
  //     return
  //   }
  //
  //   floorChangeRequestTime = System.currentTimeMillis()
  //   LOG.W(TAG_METHOD, "isChanging: $isLazilyChangingFloor")
  //
  //   lifecycleScope.launch(Dispatchers.IO) {
  //     if (!isLazilyChangingFloor) {
  //       LOG.W(LOG.TAG, "will change to floor: ${VMB.floorH!!.prettyFloorName()}")
  //       isLazilyChangingFloor = true
  //       do {
  //         val curTime = System.currentTimeMillis()
  //         val diff = curTime-floorChangeRequestTime
  //         LOG.D(LOG.TAG, "delay: $diff")
  //         delay(100)
  //       } while(diff < DELAY_CHANGE_FLOOR)
  //
  //       LOG.W(LOG.TAG, "changing to floor: ${VMB.floorH!!.prettyFloorName()} (after delay)")
  //
  //       loadFloor(VMB.floorH!!)
  //       isLazilyChangingFloor = false
  //     } else {
  //       LOG.E(LOG.TAG, "skipping floor: ${VMB.floorH!!.prettyFloorName()}")
  //     }
  //   }
  // }

  // /**
  //  * Stores in cache the last selected floor in [VMB.lastValSpaces] (for the relevant [Space])
  //  */
  // @Deprecated("MERGED") // MERGED
  // private fun updateAndCacheLastFloor(floor: Floor?) {
  //   LOG.V2(TAG_METHOD, floor?.floorNumber.toString())
  //   if (floor != null) {
  //     VMB.lastValSpaces.lastFloor=floor.floorNumber
  //     VMB.spaceH.cacheLastValues(VMB.lastValSpaces)
  //   }
  // }

  // @Deprecated("MERGED") // MERGED
  // protected fun showError(space: Space?, floors: Floors?, floor: Floor? = null, floorNum: Int = 0) {
  //   var msg = ""
  //   when {
  //     space == null -> msg = "No space selected."
  //     floors == null -> msg = "Failed to get ${VMB.spaceH.prettyFloors}."
  //     floor == null -> msg = "Failed to get ${VMB.spaceH.prettyFloor} $floorNum."
  //   }
  //   LOG.E(msg)
  //   Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
  // }
  //
  // @Deprecated("MERGED") // MERGED
  // private var floorChangeRequestTime : Long = 0
  // @Deprecated("MERGED") // MERGED
  // private var isLazilyChangingFloor = false
  // @Deprecated("MERGED") // MERGED
  // private val DELAY_CHANGE_FLOOR = 300

  /**
   * Loads a floor into the UI
   * Reads a floorplan (from cache or remote) using the [VMB] and a [FloorHelper]
   * Once it's read, then it is loaded it is posted on [VMB.floorplanFlow],
   * and through an observer it is loaded on the map.
   *
   * Must be called each time wee want to load a floor.
  //  */
  // @Deprecated("MERGED") // MERGED
  // private fun loadFloor(FH: FloorHelper) {
  //   LOG.W(TAG_METHOD, FH.prettyFloorName())
  //   lifecycleScope.launch {
  //     if (FH.hasFloorplanCached()) {
  //       readFloorplanFromCache(FH)
  //     } else {
  //       LOG.D2(TAG, "readFloorplan: remote")
  //       VMB.getFloorplanFromRemote(FH)
  //     }
  //   }
  // }

  // /**
  //  * Reads a floorplan image form the devices cache
  //  */
  // @Deprecated("MERGED") // MERGED
  // protected fun readFloorplanFromCache(FH: FloorHelper) {
  //   LOG.D(TAG_METHOD, FH.prettyFloorName())
  //   val localResult =
  //           when (val bitmap = FH.loadFromCache()) {
  //             null -> NetworkResult.Error("Failed to load from local cache")
  //             else -> NetworkResult.Success(bitmap)
  //           }
  //   VMB.floorplanFlow.value = localResult
  // }


  /**
   * Observes when a floorplan changes ([VMB.floorplanFlow]) and loads
   * the new image, and the relevant heatmap
   *
   * This has to be separate
   */
  // @Deprecated("MERGED") // MERGED
  // protected fun observeFloorplanChanges() {
  //   lifecycleScope.launch {
  //     VMB.floorplanFlow.collect { response ->
  //       when (response) {
  //         is NetworkResult.Loading -> {
  //           LOG.W("Loading ${VMB.spaceH.prettyFloorplan}")
  //         }
  //         is NetworkResult.Error -> {
  //           val msg = "Failed to fetch ${VMB.spaceH.prettyType}: ${VMB.space?.name}"
  //           LOG.E(msg)
  //           Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
  //         }
  //         is NetworkResult.Success -> {
  //           if (VMB.floorH == null) {
  //             val msg = "No selected floor/deck."
  //             LOG.W(msg)
  //             Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
  //           } else {
  //             renderFloorplan(response.data, VMB.floorH!!)
  //             loadCvMapAndHeatmap()
  //           }
  //         }
  //       }
  //     }
  //   }
  // }

  /** Called when a floorplan was successfully loaded */
  // abstract fun onFloorplanLoadedSuccess()

  /**
   * Reads the [CvMap] from cache and if it exists it:
   * - parses it into the optimized [CvMapFast] structure
   * - it renders a heatmap of the detections
   */
  // fun loadCvMapAndHeatmap() { // MERGED
  //   LOG.E()
  //   val model = VMB.detector.getDetectionModel()
  //   if (VMB.floorH==null) return
  //   val FH = VMB.floorH!!
  //   val cvMap = if (FH.hasFloorCvMap(model)) VMB.floorH?.loadCvMapFromCache(model) else null
  //   UIB.removeHeatmap()
  //   when {
  //     !FH.hasFloorCvMap(model) -> { LOG.W(TAG, "No local CvMap") }
  //     cvMap == null -> { LOG.W(TAG, "Can't load CvMap") }
  //     cvMap.schema < CvMap.SCHEMA -> {
  //       LOG.W(TAG, "CvMap outdated: version: ${cvMap.schema} (current: ${CvMap.SCHEMA}")
  //       LOG.E(TAG, "outdated cv-map")
  //       FH.clearCacheCvMaps()
  //     }
  //     else -> { // all is good, render.
  //       VMB.cvMapH = CvMapHelper(cvMap, VMB.detector.labels, FH)
  //       VMB.cvMapH?.generateCvMapFast()
  //       UIB.renderHeatmap(gmap, VMB.cvMapH)
  //     }
  //   }
  // }

  // @Deprecated("") // MERGED
  // protected fun renderFloorplan(bitmap: Bitmap?, FH: FloorHelper) {
  //   LOG.D()
  //   overlays.drawFloorplan(bitmap, gmap, FH.bounds())
  // }

  // @Deprecated("") // MERGED
  // protected fun checkInternet() {
  //   if (!app.hasInternet()) {
  //     // TODO method that updates ui based on internet connectivity: gray out settings button
  //     Toast.makeText(applicationContext, "No internet connection.", Toast.LENGTH_LONG).show()
  //   }
  // }
// }