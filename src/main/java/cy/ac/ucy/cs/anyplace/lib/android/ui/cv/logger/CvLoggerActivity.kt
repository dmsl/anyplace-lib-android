package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import cy.ac.ucy.cs.anyplace.lib.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.common.util.concurrent.ListenableFuture
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.cv.misc.Constants
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.maps.camera.CameraAndViewport
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.buttonUpdater.changeBackgroundCompatButton
import cy.ac.ucy.cs.anyplace.lib.databinding.ActivityCvLoggerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class CvLoggerActivity : AppCompatActivity(), OnMapReadyCallback {
  private companion object {
    const val CAMERA_REQUEST_CODE: Int = 1
    const val CAMERA_ASPECT_RATIO: Int = AspectRatio.RATIO_4_3 // AspectRatio.RATIO_16_9
  }

  private lateinit var binding: ActivityCvLoggerBinding
  private val viewModel by viewModels<CvLoggerViewModel> { getViewModelFactory() }
  private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
  private lateinit var map: GoogleMap


  /** kept here (not in viewModel0 as we want this to be reset on lifecycle updates */
  private var clearConfirm=false
  private var clickedScannedObjects=false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityCvLoggerBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setUpBottomSheet()
    setupMap()

    cameraProviderFuture = ProcessCameraProvider.getInstance(baseContext)
    requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)

    viewModel.setUpDetectionProcessor(
      assets,
      resources.displayMetrics,
      binding.tovCamera,
      binding.pvCamera
    )

    observeFrameDetections()
    observeLoggingStatus()

    // TODO:PM make this a setting?
    // binding.switchPadding.setOnCheckedChangeListener { _, isChecked ->
    //   CvLoggerViewModel.usePadding= isChecked
    // }
  }

  /**
   * Observes [viewModel.windowDetections] changes and updates
   * [binding.bottomSheet.buttonScannedObjects] accordingly.
   */
  private fun observeFrameDetections() {
    viewModel.windowDetections.observeForever { detections ->
      LOG.D3(TAG, "Detected: ${detections.size}")
      val detectionNum = detections.size
      binding.bottomSheet.buttonScannedObjects.text = detectionNum.toString() // TODO
      if (detectionNum == 0) {
        binding.bottomSheet.buttonScannedObjects.fadeOut()
      } else if (!binding.bottomSheet.buttonScannedObjects.isVisible) {
        binding.bottomSheet.buttonScannedObjects.fadeIn()
      }
    }
  }

  private fun setUpBottomSheet() {
    val sheetBehavior = BottomSheetBehavior.from(binding.bottomSheet.root)
    sheetBehavior.isHideable = false

    val callback = CvLoggerBottomSheetCallback(binding.bottomSheet.bottomSheetArrow)
    sheetBehavior.addBottomSheetCallback(callback)

    val gestureLayout = binding.bottomSheet.gestureLayout
    gestureLayout.viewTreeObserver.addOnGlobalLayoutListener {
      val height: Int = gestureLayout.measuredHeight
      sheetBehavior.peekHeight = (height/2f).roundToInt()+20
      LOG.W(TAG, "peek height: ${sheetBehavior.peekHeight}")
    }

    lifecycleScope.launch {
      delay(200)
      // binding.bottomSheet.
      //   .translationYBy(bottomSheetBehavior.getPeekHeight());
    }

    // TODO:PM XXX:PM CHECK:PM if 0 objects detected: then continue...
    // never get into pause..

    binding.bottomSheet.buttonLogging.setOnClickListener {
      LOG.D(TAG, "buttonStartLogging: ${viewModel.status}")

      when (viewModel.status.value) {
        Logging.stoppedMustStore -> {
            val msg = "Long click on a map location to store objects"
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
            binding.viewWarning.flashView(500)
          }
      }

      viewModel.toggleLogging(applicationContext)
      // TODO: draw red borders on map and pv (camera and detections) ?
    }

    binding.bottomSheet.buttonScannedObjects.setOnClickListener {
      if (!clickedScannedObjects) {
        clickedScannedObjects=true
        binding.bottomSheet.buttonClearObjects.fadeIn()
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

    binding.bottomSheet.buttonClearObjects.setOnClickListener {
      if (!clearConfirm) {
        clearConfirm = true
        binding.bottomSheet.buttonClearObjects.text = "Sure ?"
        binding.bottomSheet.buttonClearObjects.alpha = 1f

      } else {
        hideClearObjectsButton()
        viewModel.resetWindow()
      }
    }

    @SuppressLint("SetTextI18n")
    binding.bottomSheet.cropInfo.text =
      "${Constants.DETECTION_MODEL.inputSize}x${Constants.DETECTION_MODEL.inputSize}"
  }

  private fun hideClearObjectsButton() {
    clearConfirm=false
    binding.bottomSheet.buttonClearObjects.fadeOut()
    lifecycleScope.launch {
      delay(100)
      binding.bottomSheet.buttonClearObjects.alpha = 0.5f
      binding.bottomSheet.buttonClearObjects.text = "Clear"
    }
  }

  private fun observeLoggingStatus() {
    viewModel.status.observeForever {  status ->
      LOG.D("logging: $status")
      updateButtonLogging(status)
    }

  }

  @SuppressLint("SetTextI18n")
  private fun updateButtonLogging(status: Logging) {
    LOG.D4("updateScanningButton: $status")
    val btn = binding.bottomSheet.buttonLogging

    // TODO: draw red, gray, invisible borders..
    when (status) {
      Logging.started -> { // just started logging
        btn.text = "Pause"
        changeBackgroundCompatButton(btn, applicationContext, R.color.darkGray)
        binding.mapView.animateAlpha(0.5f, 100)
      }
      Logging.stopped -> { // start logging
        btn.text = "Start"
        changeBackgroundCompatButton(btn, applicationContext, R.color.colorPrimary)
        binding.mapView.animateAlpha(1f, 100)
        // binding.mapView.alpha=1f
      }
      Logging.stoppedMustStore -> {
        btn.text = "Store"
        binding.mapView.animateAlpha(1f, 100)
        changeBackgroundCompatButton(btn, applicationContext, R.color.yellowDark)
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
      this::analyzeImage
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
          binding.bottomSheet.timeInfo.text = "${detectionTime}ms"
          binding.bottomSheet.tvElapsedTime.text=viewModel.getElapsedSecondsStr()
          binding.bottomSheet.tvCurrentWindow.text=viewModel.storedDetections.size.toString()
          binding.bottomSheet.tvWindowObjects.text=viewModel.objectsWindow.toString()
          binding.bottomSheet.tvTotalObjects.text=viewModel.objectsTotal.toString()
        }
      }
    }
  }

  private suspend fun setUpImageConverter(image: ImageProxy) {
    withContext(Dispatchers.Main){
      @SuppressLint("SetTextI18n")
      binding.bottomSheet.frameInfo.text = "${image.width}x${image.height}"
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

    val latLng = CameraAndViewport.latLng
    val maxZoomLevel = map.maxZoomLevel // may be different from device to device

    // map.addMarker(MarkerOptions().position(latLng).title("Ucy Building"))
    // map.moveCamera(CameraUpdateFactory.newLatLng(latLng))

    // place some restrictions on the map
    map.moveCamera(CameraUpdateFactory.newCameraPosition(
      CameraAndViewport.loggerCamera(latLng, maxZoomLevel)))
    map.setMinZoomPreference(maxZoomLevel-2)

    // restrict screen to current bounds.
    lifecycleScope.launch {
      delay(500)
      val spaceBounds = map.projection.visibleRegion.latLngBounds
      LOG.D("bounds: ${spaceBounds.center}")
      map.moveCamera(CameraUpdateFactory.newLatLngBounds(spaceBounds, 0))
      map.setLatLngBoundsForCameraTarget(spaceBounds)
    }

    map.uiSettings.apply {
      isZoomControlsEnabled = false
      isMapToolbarEnabled = false
    }

    setupOnMapLongClick()
  }

  private fun setupOnMapLongClick() {
    map.setOnMapLongClickListener { location ->
      if (viewModel.canStoreObjects()) {
        LOG.D("clicked at: $location")

        // center map
        map.animateCamera(CameraUpdateFactory.newCameraPosition(
          CameraPosition(location, map.cameraPosition.zoom, 0f, 0f)))

        val windowDetections = viewModel.windowDetections.value.orEmpty().size
        viewModel.storeDetections(location)

        val curPoint = viewModel.storedDetections.size.toString()
        val msg = "Point $curPoint|$windowDetections"
        map.addMarker(MarkerOptions().position(location).title(msg))
      } else {
        val msg ="Not in logging mode"
        LOG.V2("onMapLongClick: $msg")
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
      }
    }
  }

}