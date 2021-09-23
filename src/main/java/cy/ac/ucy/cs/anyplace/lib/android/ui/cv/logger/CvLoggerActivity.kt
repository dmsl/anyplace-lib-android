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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import cy.ac.ucy.cs.anyplace.lib.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.common.util.concurrent.ListenableFuture
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.cv.misc.Constants
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.getViewModelFactory
import cy.ac.ucy.cs.anyplace.lib.databinding.ActivityCvLoggerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class CvLoggerActivity : AppCompatActivity(), OnMapReadyCallback {
  private companion object {
    const val CAMERA_REQUEST_CODE: Int = 1
    const val CAMERA_ASPECT_RATIO: Int = AspectRatio.RATIO_4_3 // AspectRatio.RATIO_16_9 // TODO:PM setting
  }

  private lateinit var binding: ActivityCvLoggerBinding
  private val viewModel by viewModels<CvLoggerViewModel> { getViewModelFactory() }
  private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
  private lateinit var gMap: GoogleMap

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityCvLoggerBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setUpBottomSheet()

    @SuppressLint("SetTextI18n")
    binding.bottomSheet.cropInfo.text =
      "${Constants.DETECTION_MODEL.inputSize}x${Constants.DETECTION_MODEL.inputSize}"

    cameraProviderFuture = ProcessCameraProvider.getInstance(baseContext)
    requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)

    viewModel.setUpDetectionProcessor(
      assets,
      resources.displayMetrics,
      binding.tovCamera,
      binding.pvCamera
    )
    // TODO:PM make this a setting?
    // binding.switchPadding.setOnCheckedChangeListener { _, isChecked ->
    //   CvLoggerViewModel.usePadding= isChecked
    // }

    // add a map dynamically
    val mapFragment = SupportMapFragment.newInstance()
    supportFragmentManager
        .beginTransaction()
        .add(R.id.mapView, mapFragment)
        .commit()
    mapFragment.getMapAsync(this)
  }

  private fun setUpBottomSheet() {
    val sheetBehavior = BottomSheetBehavior.from(binding.bottomSheet.root)
    sheetBehavior.isHideable = false

    val callback = CvLoggerBottomSheetCallback(binding.bottomSheet.bottomSheetArrow)
    sheetBehavior.addBottomSheetCallback(callback)

    val gestureLayout = binding.bottomSheet.gestureLayout
    gestureLayout.viewTreeObserver.addOnGlobalLayoutListener {
      val height: Int = gestureLayout.measuredHeight
      sheetBehavior.peekHeight = (height/3f).roundToInt()
    }


    lifecycleScope.launch {
      delay(200)

      // binding.bottomSheet.
      //   .translationYBy(bottomSheetBehavior.getPeekHeight());
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


  @SuppressLint("UnsafeOptInUsageError")
  private fun analyzeImage(image: ImageProxy) {
    lifecycleScope.launch(Dispatchers.Default) {
      image.use {
        if (!viewModel.imageConvertedIsSetUpped()) {
          setUpImageConverter(image)
        }

        val detectionTime = viewModel.detectObjectsOnImage(image)

        withContext(Dispatchers.Main) {
          @SuppressLint("SetTextI18n")
          binding.bottomSheet.timeInfo.text = "${detectionTime}ms"
        }
      }
    }
  }

  private suspend fun setUpImageConverter(image: ImageProxy){
    withContext(Dispatchers.Main){
      @SuppressLint("SetTextI18n")
      binding.bottomSheet.frameInfo.text = "${image.width}x${image.height}"
    }

    LOG.E("FRAME: ${image.width}x${image.height}")
    viewModel.setUpImageConverter(baseContext, image)

  }
  override fun onMapReady(googleMap: GoogleMap) {
    gMap = googleMap
    LOG.V2(TAG, "map ready")

    // Add a marker in Sydney and move the camera
    val sydney = LatLng(-34.0, 151.0)
    gMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
    gMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
  }
}