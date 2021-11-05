package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.GroundOverlay
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.common.util.concurrent.ListenableFuture
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.cv.misc.Constants
import cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers.FloorHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers.FloorsHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers.SpaceHelper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.maps.Markers
import cy.ac.ucy.cs.anyplace.lib.android.maps.Overlays
import cy.ac.ucy.cs.anyplace.lib.android.maps.camera.CameraAndViewport
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerViewModel.Companion.getSecondsRounded
import cy.ac.ucy.cs.anyplace.lib.android.utils.AppInfo
import cy.ac.ucy.cs.anyplace.lib.android.utils.demo.AssetReader
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.buttonUtils.changeBackgroundButton
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.buttonUtils.changeBackgroundButtonCompat
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.buttonUtils.changeMaterialButtonIcon
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.buttonUtils.removeMaterialButtonIcon
import cy.ac.ucy.cs.anyplace.lib.databinding.ActivityCvLoggerBinding
import cy.ac.ucy.cs.anyplace.lib.models.Floor
import cy.ac.ucy.cs.anyplace.lib.models.Floors
import cy.ac.ucy.cs.anyplace.lib.models.LastValSpaces
import cy.ac.ucy.cs.anyplace.lib.models.Space
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt


@AndroidEntryPoint
class CvLoggerActivity : AppCompatActivity(), OnMapReadyCallback {
  private companion object {
    const val CAMERA_REQUEST_CODE: Int = 1
    const val CAMERA_ASPECT_RATIO: Int = AspectRatio.RATIO_4_3 // AspectRatio.RATIO_16_9
    const val OPACITY_MAP_LOGGING = 0f
    const val ANIMATION_DELAY : Long = 100
  }
  private var floormapOverlay: GroundOverlay? = null
  private lateinit var binding: ActivityCvLoggerBinding
  private lateinit var viewModel: CvLoggerViewModel
  private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
  private lateinit var map: GoogleMap
  private lateinit var statusUpdater: StatusUpdater

  /** Selected [Space] */
  private var space: Space? = null
  /** All floors of the selected [space]*/
  private var floors: Floors? = null
  /** Selected floor/deck ([Floor]) of [space] */
  private var floor: Floor? = null

  /** Selected [Space] ([SpaceHelper]) */
  private var spaceH: SpaceHelper? = null
  /** floorsH of selected [spaceH] */
  private var floorsH: FloorsHelper? = null
  /** Selected floorH of [floorsH] */
  private var floorH: FloorHelper? = null

  /** LastVals: user last selections regarding a space.
   * Currently not much use (for a field var), but if we have multiple
   * lastVals for space then it would make sense. */
  private var lastValSpaces: LastValSpaces = LastValSpaces()

  private val overlays by lazy { Overlays(applicationContext) }
  private val assetReader by lazy { AssetReader(applicationContext) }
  private val appInfo by lazy { AppInfo(applicationContext) }

  /** kept here (not in viewModel) as we want this to be reset on lifecycle updates */
  private var clearConfirm=false
  private var clickedScannedObjects=false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityCvLoggerBinding.inflate(layoutInflater)
    setContentView(binding.root)

    viewModel = ViewModelProvider(this).get(CvLoggerViewModel::class.java)
    setupComputerVision()
    setupMap()
    setupButtonsAndUi()
  }

  override fun onResume() {
    super.onResume()
    LOG.D3(TAG, "onResume")
    readPrefsAndContinueSetup()
  }

  private fun readPrefsAndContinueSetup() {
    LOG.D4(TAG, "readPrefsAndSetupBottomSheet")
    lifecycleScope.launch {
      dataStoreCvLogger.read.first { prefs ->
        viewModel.prefs = prefs
        // set up that depends on preferences
        setUpBottomSheet()

        true
      }
    }
  }

  /**
   * Observes [viewModel.windowDetections] changes and updates
   * [binding.bottomUi.buttonCameraWindow] accordingly.
   */
  private fun updateCameraTimerButton() {
    val elapsed = viewModel.getElapsedSeconds()
    val remaining = (viewModel.prefs.windowSeconds.toInt()) - elapsed
    val btn = binding.bottomUi.buttonCameraTimer
    val progressBar = binding.bottomUi.progressBarTimer

    if (remaining>0) {
      val windowSecs = viewModel.prefs.windowSeconds.toInt()
      setupProgressBarTimerAnimation(btn, progressBar, windowSecs)
      btn.text = getSecondsRounded(remaining, windowSecs)
    } else {
      progressBar.visibility = View.INVISIBLE
      btn.text = ""
      progressBar.progress = 100
      if (!viewModel.storedDetections.values.isEmpty()) {
        changeMaterialButtonIcon(btn, applicationContext, R.drawable.ic_objects)
      } else {   // no results, hide the timer
        removeMaterialButtonIcon(btn)
        btn.fadeOut()
      }
    }
  }

  /**
   * Initiate a circular progress bar animation, inside a coroutine for
   * smooth (and independent from other threads) updates.
   * It progresses according to the window time
   */
  private fun setupProgressBarTimerAnimation(
    btnTimer: MaterialButton,
    progressBar: ProgressBar,
    windowSecs: Int) {
    // showing timer button but not yet the progress bar
    if (btnTimer.visibility == View.VISIBLE &&
      progressBar.visibility != View.VISIBLE) {
      val delayMs = (windowSecs*1000/100).toLong()
      lifecycleScope.launch {
        var progress = 0
        progressBar.progress=progress
        progressBar.visibility = View.VISIBLE
        while(progress < 100) {
          when (viewModel.circleTimerAnimation) {
            TimerAnimation.reset -> { resetCircleAnimation(progressBar); break }
            TimerAnimation.running -> { progressBar.setProgress(++progress, true) }
            TimerAnimation.paused -> {  }
          }
          delay(delayMs)
        }
      }
    }
  }

  private fun resetCircleAnimation(progressBar: ProgressBar) {
    progressBar.visibility = View.INVISIBLE
    progressBar.progress=0
  }

  /**
   * Observes [viewModel.objectDetectionsAll] changes and updates
   * [binding.bottomUi.buttonCameraTimer] accordingly.
   */
  private fun observeObjectDetections() {
    viewModel.objectsWindowAll.observeForever { detections ->
      binding.bottomUi.tvWindowObjectsAll.text=detections.toString()
    }
  }

  private fun setupComputerVision() {
    cameraProviderFuture = ProcessCameraProvider.getInstance(baseContext)
    requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)

    viewModel.setUpDetectionProcessor(
      assets,
      resources.displayMetrics,
      binding.tovCamera,
      binding.pvCamera
    )

    observeObjectDetections()
    observeLoggingStatus()
  }

  private fun setupButtonsAndUi() {
    statusUpdater = StatusUpdater(binding.tvStatusTitle,
      binding.viewStatusBackground,
      binding.viewWarning,
      applicationContext)

    checkInternet()

    binding.bottomUi.buttonLogging.setOnClickListener {
      LOG.D(TAG, "buttonStartLogging: ${viewModel.status}")
      when (viewModel.status.value) {
        Logging.stoppedMustStore -> {
          // TODO LEFTHERE
          // TODO:PM long click on existing point: update existing measurements..
          // SHOW MSG: This will ignore the last  'window'
          // MATERIALIZE, STORE, and EXIT
          // val msg = "TODO: store locally.."
          viewModel.status.value = Logging.finished
          setUpBottomSheet(true)
          // TODO: STORE RESULTS..
          // TODO put an upload button and enable it..

          viewModel.hideActiveMarkers()
          // TODO:PM heatmap saved results

          // statusUpdater.showWarning(msg)
        }
        else ->
          viewModel.toggleLogging()
      }
    }

    binding.bottomUi.buttonCameraTimer.setOnClickListener {
      if (viewModel.objectsWindowUnique > 0 &&!clickedScannedObjects) {
        clickedScannedObjects=true
        binding.bottomUi.buttonClearObjects.fadeIn()
        lifecycleScope.launch {
          delay(5000)
          clickedScannedObjects=false
          if(clearConfirm) {
            clickedScannedObjects=true
            delay(5000) // an extra delay
            clearConfirm=false
            clickedScannedObjects=false
          }
          hideClearObjectsButton()
        }
      }
    }

    binding.bottomUi.buttonClearObjects.setOnClickListener {
      if (!clearConfirm) {
        clearConfirm = true
        binding.bottomUi.buttonClearObjects.text = "Sure ?"
        binding.bottomUi.buttonClearObjects.alpha = 1f
      } else {
        hideClearObjectsButton()
        binding.bottomUi.buttonCameraTimer.fadeOut()
        viewModel.resetWindow()
        statusUpdater.hideStatus()
      }
    }

    // // TODO:PM Settings
    // // Setups a regular button to act as a menu button
    //   binding.buttonSettings.setOnClickListener {
    //     SettingsDialog.SHOW(supportFragmentManager, SettingsDialog.FROM_CVLOGGER)
    //   }

    binding.buttonSettings.setOnLongClickListener {
      Toast.makeText(applicationContext,"App version: ${appInfo.version}", Toast.LENGTH_SHORT).show()
      true
    }
  }

  private fun checkInternet() {
    if (!app.hasInternetConnection()) {
      // TODO method that updates ui based on internet connectivity: gray out settings button
      Toast.makeText(applicationContext, "No internet connection.", Toast.LENGTH_LONG).show()
    }
  }

  private fun hideBottomSheet() {
    binding.bottomUi.bottomSheetInternal.visibility = View.GONE
    binding.bottomUi.bottomSheetArrow.visibility = View.GONE
  }

  private fun showBottomSheet() {
    binding.bottomUi.bottomSheetInternal.visibility = View.VISIBLE
    binding.bottomUi.bottomSheetArrow.visibility = View.VISIBLE
  }

  private fun setUpBottomSheet(forceShow : Boolean = false) {
    val sheetBehavior = BottomSheetBehavior.from(binding.bottomUi.root)
    sheetBehavior.isHideable = false
    // TODO: dev mode hides other elements of bottom sheeet
    if (!forceShow && !viewModel.prefs.devMode) {
      hideBottomSheet()
      return
    }

    showBottomSheet()

    val callback = CvLoggerBottomSheetCallback(binding.bottomUi.bottomSheetArrow)
    sheetBehavior.addBottomSheetCallback(callback)

    val gestureLayout = binding.bottomUi.gestureLayout
    gestureLayout.viewTreeObserver.addOnGlobalLayoutListener {
      val height: Int = gestureLayout.measuredHeight
      sheetBehavior.peekHeight = (height/2f).roundToInt()+50
      LOG.V5(TAG, "peek height: ${sheetBehavior.peekHeight}")
    }

    @SuppressLint("SetTextI18n")
    binding.bottomUi.cropInfo.text =
      "${Constants.DETECTION_MODEL.inputSize}x${Constants.DETECTION_MODEL.inputSize}"
  }

  private fun hideClearObjectsButton() {
    clearConfirm=false
    binding.bottomUi.buttonClearObjects.fadeOut()
    lifecycleScope.launch {
      delay(100)
      binding.bottomUi.buttonClearObjects.alpha = 0.5f
      binding.bottomUi.buttonClearObjects.text = "Clear"
    }
  }

  private fun observeLoggingStatus() {
    viewModel.status.observeForever {  status ->
      LOG.D("logging: $status")
      updatedLoggingUI(status)
    }
  }

  @SuppressLint("SetTextI18n")
  private fun updatedLoggingUI(status: Logging) {
    LOG.D4("updateScanningButton: $status")
    val btnLogging = binding.bottomUi.buttonLogging
    val btnTimer= binding.bottomUi.buttonCameraTimer


    when (status) {
      Logging.finished-> { // finished a scanning
        btnTimer.fadeOut()
        btnLogging.text = "TODO: store local"
        changeBackgroundButtonCompat(btnLogging, applicationContext, R.color.green)

        // sleep a while.....
        // TODO show the below menu..
        // TODO store..
        // TODO show upload section..(on the below menu, with UPLOAD button)..

        // TODO:PM heatmap?
        // TODO set to stopped again
      }
      Logging.started -> { // just started scanning
        viewModel.circleTimerAnimation=TimerAnimation.running
        btnLogging.text = "Pause"
        removeMaterialButtonIcon(btnTimer)
        changeBackgroundButtonCompat(btnLogging, applicationContext, R.color.darkGray)
        changeBackgroundButton(btnTimer, applicationContext, R.color.redDark)
        btnTimer.fadeIn()
        binding.mapView.animateAlpha(OPACITY_MAP_LOGGING, ANIMATION_DELAY)
      }
      Logging.stopped -> { // stopped after a pause or a store: can start logging again
        viewModel.circleTimerAnimation=TimerAnimation.paused
        if (viewModel.previouslyPaused) {
          btnLogging.text = "resume"
        } else {
          btnLogging.text = "start"
        }
        changeBackgroundButtonCompat(btnLogging, applicationContext, R.color.colorPrimary)
        binding.mapView.animateAlpha(1f, ANIMATION_DELAY)
        changeBackgroundButton(btnTimer, applicationContext, R.color.darkGray)
      }
      Logging.stoppedNoDetections -> { // stopped after no detections: retry a scan
        viewModel.circleTimerAnimation=TimerAnimation.reset
        lifecycleScope.launch {
          statusUpdater.showWarningAutohide("No detections.")
          restartLogging(250)
        }

      }
      Logging.stoppedMustStore -> { // stopped with detections: must store
        viewModel.circleTimerAnimation=TimerAnimation.reset
        btnLogging.text = "store"
        binding.mapView.animateAlpha(1f, ANIMATION_DELAY)
        changeBackgroundButtonCompat(btnLogging, applicationContext, R.color.yellowDark)
        changeBackgroundButton(btnTimer, applicationContext, R.color.yellowDark)
      }
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    when (requestCode) {
      CAMERA_REQUEST_CODE -> {
        val indexOfCameraPermission = permissions.indexOf(Manifest.permission.CAMERA)
        if (grantResults[indexOfCameraPermission] == PackageManager.PERMISSION_GRANTED) {
          cameraProviderFuture.addListener(
            this::bindPreview,
            ContextCompat.getMainExecutor(baseContext)
          )
        } else {
          Toast.makeText(
            baseContext,
            "Permissions not granted by the user.",
            Toast.LENGTH_SHORT
          ).show()
          finish()
        }
      }
    }

    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
  }

  private fun bindPreview() {
    val preview: Preview = Preview.Builder()
        .setTargetAspectRatio(CAMERA_ASPECT_RATIO)
        .setTargetRotation(CvLoggerViewModel.CAMERA_ROTATION)
        .build()

    val cameraSelector: CameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
        .build()

    preview.setSurfaceProvider(binding.pvCamera.surfaceProvider)

    val imageAnalysis = ImageAnalysis.Builder()
        .setTargetAspectRatio(CAMERA_ASPECT_RATIO)
        .setTargetRotation(CvLoggerViewModel.CAMERA_ROTATION)
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()

    imageAnalysis.setAnalyzer(
      ContextCompat.getMainExecutor(baseContext),
      this@CvLoggerActivity::analyzeImage
    )

    val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
    cameraProvider.bindToLifecycle(
      this as LifecycleOwner,
      cameraSelector,
      imageAnalysis,
      preview
    )
  }


  @SuppressLint("UnsafeOptInUsageError", "SetTextI18n")
  private fun analyzeImage(image: ImageProxy) {
    lifecycleScope.launch(Dispatchers.Default) {
      image.use {
        if (!viewModel.imageConvertedIsSetUpped()) {
          setUpImageConverter(image)
        }

        val detectionTime = viewModel.detectObjectsOnImage(image)
        withContext(Dispatchers.Main) {
          binding.bottomUi.timeInfo.text = "${detectionTime}ms"
          updateCameraTimerButton()
          binding.bottomUi.tvElapsedTime.text=viewModel.getElapsedSecondsStr()
          binding.bottomUi.tvWindowObjectsUnique.text=viewModel.objectsWindowUnique.toString()
          binding.bottomUi.tvCurrentWindow.text=viewModel.storedDetections.size.toString()
          binding.bottomUi.tvTotalObjects.text=viewModel.objectsTotal.toString()
        }
      }
    }
  }

  private suspend fun setUpImageConverter(image: ImageProxy) {
    withContext(Dispatchers.Main){
      @SuppressLint("SetTextI18n")
      binding.bottomUi.frameInfo.text = "${image.width}x${image.height}"
    }

    LOG.D2("Frame: ${image.width}x${image.height}")
    viewModel.setUpImageConverter(baseContext, image)
  }

  private fun setupMap() {
    // add a map dynamically
    val mapFragment = SupportMapFragment.newInstance()
    supportFragmentManager
        .beginTransaction()
        .add(R.id.mapView, mapFragment)
        .commit()
    mapFragment.getMapAsync(this)
  }

  override fun onMapReady(googleMap: GoogleMap) {
    LOG.D(TAG, "onMapReady")
    map = googleMap
    viewModel.markers = Markers(applicationContext, map)

    val maxZoomLevel = map.maxZoomLevel // may be different from device to device
    // map.addMarker(MarkerOptions().position(latLng).title("Ucy Building"))
    // map.moveCamera(CameraUpdateFactory.newLatLng(latLng))

    // TODO Space has to be sent to this activity (SafeArgs?) using a previous "Select Space" activity.
    // (maybe using Bundle is easier/better)
    loadSpaceAndFloorFromAssets()

    // TODO:PM this must be moved to earlier activity
    // along with Space/Floors loading (that also needs implementation).
    lifecycleScope.launch { floorsH?.fetchAllFloorplans() }

    // place some restrictions on the map
    map.moveCamera(CameraUpdateFactory.newCameraPosition(
      CameraAndViewport.loggerCamera(spaceH!!.latLng(), maxZoomLevel)))
    map.setMinZoomPreference(maxZoomLevel-3)

    // restrict screen to current bounds.
    lifecycleScope.launch {
      // delay(500) // CLR:PM

      // if (floorH == null) { // BUGFIX CLR:PM
      //   LOG.E("floorH is null!!!")
      // }
      map.moveCamera(CameraUpdateFactory.newLatLngBounds(floorH?.bounds(), 0))
      val floorOnScreenBounds = map.projection.visibleRegion.latLngBounds
      LOG.D2("bounds: ${floorOnScreenBounds.center}")
      map.setLatLngBoundsForCameraTarget(floorH?.bounds())
      readFloorplan(floorH!!)
    }

    map.uiSettings.apply {
      isZoomControlsEnabled = false
      isMapToolbarEnabled = false
      isTiltGesturesEnabled = false
      isCompassEnabled = false
      isIndoorLevelPickerEnabled = true
    }
    setupOnMapLongClick()
  }

  fun loadFloorplansAll() {
    // val bitmap = FH.requestRemoteFloorplan()
  }


  /**
   * Loads from assets the Space and the Space's Floors
   * Then it loads the floorplan for [selectedFloorPlan].
   *
   * TODO Implement this from network (in an earlier activity), and
   * pass it here through [SafeArgs] or [Bundle]
   */
  fun loadSpaceAndFloorFromAssets() {
    space = assetReader.getSpace()
    floors = assetReader.getFloors()

    if (space == null || floors == null) {
      showError(space, floors)
      return
    }

    spaceH = SpaceHelper(applicationContext, viewModel.repository, space!!)
    floorsH = FloorsHelper(floors!!, spaceH!!)

    LOG.D("Selected ${spaceH?.prettyType}: ${space!!.name}")
    LOG.D("${spaceH!!.prettyType} has ${floors!!.floors.size} ${spaceH!!.prettyFloors}.")

    if (!floorsH!!.hasFloors()) {
      val msg = "Selected ${spaceH!!.prettyType} has no ${spaceH!!.prettyFloors}."
      LOG.E(msg)
      Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
      return
    }

    // TODO:PM put a floor selector...
    var floor : Floor? = null
    if (spaceH!!.hasLastValuesCached()) {
      val lv = spaceH!!.loadLastValues()
      if (lv.lastFloor!=null) {
        LOG.D2(TAG, "LastVal: cached ${spaceH!!.prettyFloor}${lv.lastFloor}.")
        floor = floorsH!!.getFloor(lv.lastFloor!!)
      }
      lastValSpaces = lv
    }

    if (floor == null)  {
      LOG.D2(TAG, "Loading first ${spaceH!!.prettyFloor}.")
      floor = floorsH!!.getFirstFloor()
    }

    val selectedFloorNum=floor.floorNumber.toInt()
    // FIXED SELECTION:
    // val selectedFloorNum=3
    // floor = floorsH!!.getFloor(selectedFloorNum)
    if (floor == null) {
      showError(space, floors, floor, selectedFloorNum)
      return
    }

    LOG.D("Selected ${spaceH?.prettyFloor}: ${floor?.floorName}")

    floorH = FloorHelper(floor, spaceH!!)
    updateAndCacheLastFloor(floor) // TODO:PM when floor changing..
  }

  private fun updateAndCacheLastFloor(floor: Floor) {
    lastValSpaces.lastFloor=floor.floorNumber
    spaceH?.cacheLastValues(lastValSpaces)
  }

  private fun showError(space: Space?, floors: Floors?, floor: Floor? = null, floorNum: Int = 0) {
    var msg = ""
    when {
      space == null -> msg = "No space selected."
      floors == null -> msg = "Failed to get ${spaceH?.prettyFloors}."
      floor == null -> msg = "Failed to get ${spaceH?.prettyFloor} $floorNum."
    }
    LOG.E(msg)
    Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
  }

  /**
   * Reads a floorplan (from cache or remote) using the [viewModel] and a [FloorHelper]
   * Once it's read, then it is loaded it is posted on [viewModel.floorplanResp],
   * and through an observer it is loaded on the map.
   *
   * Must be called each time wee want to load a floor.
   */
  private fun readFloorplan(FH: FloorHelper) {
    LOG.D(TAG, "readFloorplan")
    lifecycleScope.launch {
      if (FH.hasFloorplanCached()) {
        LOG.D(TAG, "readFloorplan: cache")
        val localResult =
          when (val bitmap = FH.loadFromCache()) {
            null -> NetworkResult.Error("Failed to load from local cache")
            else -> NetworkResult.Success(bitmap)
          }
        viewModel.floorplanResp.postValue(localResult)
      } else {
        LOG.D2(TAG, "readFloorplan: remote")
        // FH.clearCache() on settings?
        viewModel.getFloorplan(FH)
      }
    }

    viewModel.floorplanResp.observeForever { response ->
      when (response) {
        is NetworkResult.Loading -> {
          LOG.W("Loading ${spaceH?.prettyFloorplan}")
        }
        is NetworkResult.Error -> {
          val msg = "Failed to get $space"
          LOG.W(msg)
          Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        }
        is NetworkResult.Success -> {
          if (floorH == null) {
            val msg = "No selected floor/deck."
            LOG.W(msg)
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
          } else {
            renderFloorplan(response.data, floorH!!)
          }
        }
      }
    }
  }

  fun renderFloorplan(bitmap: Bitmap?, FH: FloorHelper) {
    LOG.D("renderFloorplan:")
    // TODO:PM remove the previous floorplan.
    floormapOverlay = overlays.addSpaceOverlay(bitmap, map, FH.bounds())
  }

  /**
   * Stores some measurements on the given GPS locations
   */
  private fun setupOnMapLongClick() {
    map.setOnMapLongClickListener { location ->
      if (viewModel.canStoreObjects()) {
        LOG.D("clicked at: $location")

        // re-center map
        map.animateCamera(CameraUpdateFactory.newCameraPosition(
          CameraPosition(location, map.cameraPosition.zoom,
            // don't alter tilt/bearing
            map.cameraPosition.tilt,
            map.cameraPosition.bearing)
        ))

        val windowDetections = viewModel.windowDetections.value.orEmpty().size
        viewModel.storeDetections(location)

        // add marker
        val curPoint = viewModel.storedDetections.size.toString()
        val msg = "Point: $curPoint\n\nObjects: $windowDetections\n"
        viewModel.addMarker(location, msg)

        // pause a bit, then restart logging
        lifecycleScope.launch {
          restartLogging()
        }
        // binding.bottomUi.buttonCameraTimer.fadeOut()

      } else {
        val msg ="Not in logging mode"
        LOG.V2("onMapLongClick: $msg")
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
      }
    }
  }

  /**
   * Pauses a bit, then it restarts logging
   * Used when:
   * - detections were stored on the map
   * - no detections found
   *
   * Actions taken:
   * - hides the top status
   * - removes the camera timer (will be added again when reseted?) and makes it gray,
   * - removes the logging button
   * - shows the map
   * - stars a new window
   */
  suspend private fun restartLogging(delayMs: Long = 500) {
    delay(delayMs)
    statusUpdater.hideStatus()
    removeMaterialButtonIcon(binding.bottomUi.buttonCameraTimer)
    changeBackgroundButton(binding.bottomUi.buttonCameraTimer, applicationContext, R.color.darkGray)
    changeBackgroundButtonCompat(binding.bottomUi.buttonLogging, applicationContext, R.color.colorPrimary)
    binding.mapView.animateAlpha(1f, ANIMATION_DELAY)
    viewModel.startNewWindow()
  }

}