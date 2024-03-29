/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.hardware.Camera.PreviewCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.R
import android.view.WindowManager
import android.media.Image.Plane
import android.content.pm.PackageManager
import android.hardware.Camera
import android.widget.Toast
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraAccessException
import android.media.ImageReader
import android.os.*
import android.util.Size
import android.view.Surface
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.env.ImageUtils
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.DetectorViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception

/**
 * Logic related to:
 * 1. The Camera
 * 2. The Basic BottomSheet binding:
 *    - it sets it up, and
 *
 * It knows nothing about:
 *  - about detection (what camera will be used)
 *  - the content and structure of the [BottomSheet]
 */
@AndroidEntryPoint
abstract class CameraActivity : AppCompatActivity(),
        ImageReader.OnImageAvailableListener,
        PreviewCallback {

  companion object {
    private const val PERMISSIONS_REQUEST = 1
    private const val PERMISSION_CAMERA = Manifest.permission.CAMERA

    private fun allPermissionsGranted(grantResults: IntArray): Boolean {
      for (result in grantResults) {
        if (result != PackageManager.PERMISSION_GRANTED) return false
      }
      return true
    }
  }

  lateinit var bottomSheetLayout: ConstraintLayout
  lateinit var gestureLayout: ConstraintLayout
  lateinit var sheetBehavior: BottomSheetBehavior<ConstraintLayout>

  private var rgbBytes: IntArray? = null

  var previewWidth = 0
  var previewHeight = 0
  val isDebug = false
  private var handler: Handler? = null
  private var handlerThread: HandlerThread? = null
  private var useCamera2API = false
  private var isProcessingFrame = false
  private val yuvBytes = arrayOfNulls<ByteArray>(3)
  protected var luminanceStride = 0
  private var postInferenceCallback: Runnable? = null
  private var imageConverter: Runnable? = null

  ////////////
  // OVERRIDES
  ///// LAYOUT
  protected abstract val layout_activity: Int
  protected abstract val layout_camera_fragment: Int
  protected abstract val id_bottomsheet: Int
  protected abstract val id_gesture_layout: Int
  //// CAMERA METHODS
  protected abstract fun processImage()
  /** Runs after the inference, once we had the objects detected */
  protected abstract fun onInferenceRan(detections: MutableList<Classifier.Recognition>)
  protected abstract fun onProcessImageFinished()
  protected abstract fun onPreviewSizeChosen(size: Size?, rotation: Int)
  //// OPTIONS
  protected abstract val desiredPreviewFrameSize: Size?
  protected abstract fun setNumThreads(numThreads: Int)
  protected abstract fun setUseNNAPI(isChecked: Boolean)
  /** Called by [CameraActivity]'s [onCreate] */
  protected abstract fun postResume()
  abstract val view_model_class: Class<DetectorViewModel>
  protected lateinit var _vm: ViewModel
  // end of OVERRIDES
  ///////////////////

  val layoutCamera: FrameLayout by lazy { findViewById(R.id.container) }

  lateinit var VMD: DetectorViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    LOG.V()
    super.onCreate(null)

    setupBaseUi()
    checkPermissionsAndConnectCamera()

    _vm = ViewModelProvider(this)[view_model_class]
    VMD= _vm as DetectorViewModel

  }


  @Synchronized
  public override fun onResume() {
    LOG.V3()
    super.onResume()
    handlerThread = HandlerThread("inference")
    handlerThread!!.start()
    handler = Handler(handlerThread!!.looper)

    postResume()
  }


  fun hideBottomSheet() {
    lifecycleScope.launch(Dispatchers.IO) {
      bottomSheetLayout.isVisible = false
    }
  }
  fun showBottomSheet() {
    lifecycleScope.launch(Dispatchers.IO) {
      bottomSheetLayout.isVisible = true
    }
  }

  /**
   * Base UI includes at least:
   * - the layout that is set from an overridden getter
   * - a bottom sheet
   */
  private fun setupBaseUi() {
    // bind overridden UI elements
    setContentView(layout_activity)
    bottomSheetLayout = findViewById(id_bottomsheet)
    gestureLayout = findViewById(id_gesture_layout)

    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  }

  fun checkPermissionsAndConnectCamera() {
    if (hasCameraPermission()) {
      setFragment()
    } else {
      requestCameraPermission()
    }
  }

  protected fun getRgbBytes(): IntArray? {
    imageConverter!!.run()
    return rgbBytes
  }

  protected val luminance: ByteArray? get() = yuvBytes[0]

  /** Callback for android.hardware.Camera API  */
  override fun onPreviewFrame(bytes: ByteArray, camera: Camera) {
    if (isProcessingFrame) {
      LOG.V3(TAG_METHOD, "Dropping frame!")
      return
    }

    try {
      // Initialize the storage bitmaps once when the resolution is known.
      if (rgbBytes == null) {
        val previewSize = camera.parameters.previewSize
        previewHeight = previewSize.height
        previewWidth = previewSize.width
        rgbBytes = IntArray(previewWidth * previewHeight)
        onPreviewSizeChosen(Size(previewSize.width, previewSize.height), 90)
      }
    } catch (e: Exception) {
      LOG.E(e)
      return
    }
    isProcessingFrame = true
    yuvBytes[0] = bytes
    luminanceStride = previewWidth
    imageConverter = Runnable {
      ImageUtils.convertYUV420SPToARGB8888(
              bytes,
              previewWidth,
              previewHeight,
              rgbBytes)
    }
    postInferenceCallback = Runnable {
      camera.addCallbackBuffer(bytes)
      isProcessingFrame = false
    }
    processImage()
  }

  /** Callback for Camera2 API  */
  override fun onImageAvailable(reader: ImageReader) {
    // We need wait until we have some size from onPreviewSizeChosen
    if (previewWidth == 0 || previewHeight == 0) {
      return
    }

    if (rgbBytes == null) {
      rgbBytes = IntArray(previewWidth * previewHeight)
    }
    try {
      val image = reader.acquireLatestImage() ?: return
      if (isProcessingFrame) {
        image.close()
        return
      }

      isProcessingFrame = true
      // Trace.beginSection("imageAvailable")
      val planes = image.planes
      fillBytes(planes, yuvBytes)
      luminanceStride = planes[0].rowStride
      val uvRowStride = planes[1].rowStride
      val uvPixelStride = planes[1].pixelStride
      imageConverter = Runnable {
        ImageUtils.convertYUV420ToARGB8888(
                yuvBytes[0],
                yuvBytes[1],
                yuvBytes[2],
                previewWidth,
                previewHeight,
                luminanceStride,
                uvRowStride,
                uvPixelStride,
                rgbBytes
        )
      }
      postInferenceCallback = Runnable {
        image.close()
        isProcessingFrame = false
      }
      processImage()
    } catch (e: Exception) {
      LOG.E(e)
      // Trace.endSection()
      return
    }
    // Trace.endSection()
  }


  @Synchronized
  public override fun onPause() {
    LOG.V3()
    handlerThread!!.quitSafely()
    try {
      handlerThread!!.join()
      handlerThread = null
      handler = null
    } catch (e: InterruptedException) {
      LOG.E(TAG, "Exception: " + e.message)
    }
    super.onPause()
  }

  @Synchronized
  public override fun onStop() {
    LOG.V3()
    super.onStop()
  }

  @Synchronized
  public override fun onDestroy() {
    LOG.V3()
    super.onDestroy()
  }

  @Synchronized
  protected fun runInBackground(r: Runnable?) {
    if (handler != null) {
      handler!!.post(r!!)
    }
  }

  override fun onRequestPermissionsResult(
          code: Int, perms: Array<String>, result: IntArray) {
    super.onRequestPermissionsResult(code, perms, result)
    if (code == PERMISSIONS_REQUEST) {
      if (allPermissionsGranted(result)) {
        setFragment()
      } else {
        requestCameraPermission()
      }
    }
  }

  private fun hasCameraPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED
    } else true
  }

  private fun requestCameraPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
        Toast.makeText(
                this@CameraActivity,
                "Camera permission is required for this demo",
                Toast.LENGTH_LONG
        )
                .show()
      }
      requestPermissions(arrayOf(PERMISSION_CAMERA), PERMISSIONS_REQUEST)
    }
  }

  // Returns true if the device supports the required hardware level, or better.
  private fun isHardwareLevelSupported(
          characteristics: CameraCharacteristics, requiredLevel: Int
  ): Boolean {
    val deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)!!
    return if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
      requiredLevel == deviceLevel
    } else requiredLevel <= deviceLevel
    // deviceLevel is not LEGACY, can use numerical sort
  }

  private fun chooseCamera(): String? {
    val manager = getSystemService(CAMERA_SERVICE) as CameraManager
    try {
      for (cameraId in manager.cameraIdList) {
        val characteristics = manager.getCameraCharacteristics(cameraId)

        // We don't use a front facing camera in this sample.
        val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
        if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) { continue }
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?: continue

        // Fallback to camera1 API for internal cameras that don't have full support.
        // This should help with legacy situations where using the camera2 API causes
        // distorted or otherwise broken previews.
        useCamera2API = (facing == CameraCharacteristics.LENS_FACING_EXTERNAL
                || isHardwareLevelSupported(
                characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL))
        LOG.I(TAG, "Camera API lv2?: $useCamera2API")
        return cameraId
      }
    } catch (e: CameraAccessException) {
      LOG.E(TAG ,"Not allowed to access camera: " + e.message)
    }
    return null
  }

  protected fun setFragment() {
    val cameraId = chooseCamera()
    val fragment: androidx.fragment.app.Fragment
    if (useCamera2API) {
      val connectionCallback = CameraConnectionFragment.ConnectionCallback {
        size, rotation ->
          previewHeight = size!!.height
          previewWidth = size.width
          onPreviewSizeChosen(size, rotation)
      }

      val camera2Fragment = CameraConnectionFragment.newInstance(
              connectionCallback, this,
              layout_camera_fragment,
              desiredPreviewFrameSize!!)
      camera2Fragment.setCamera(cameraId)
      fragment = camera2Fragment
    } else {
      fragment = LegacyCameraConnectionFragment(this, layout_camera_fragment, desiredPreviewFrameSize!!)
    }

    supportFragmentManager.beginTransaction().replace(R.id.container, fragment).commit()
  }

  protected fun fillBytes(planes: Array<Plane>, yuvBytes: Array<ByteArray?>) {
    // Because of the variable row stride it's not possible to know in
    // advance the actual necessary dimensions of the yuv planes.
    for (i in planes.indices) {
      val buffer = planes[i].buffer
      if (yuvBytes[i] == null) {
        LOG.V3(TAG, String.format("Initializing buffer %d at size %d", i, buffer.capacity()))
        yuvBytes[i] = ByteArray(buffer.capacity())
      }
      buffer[yuvBytes[i]]
    }
  }

  protected fun readyForNextImage() {  postInferenceCallback?.run() }

  protected val screenOrientation: Int
    get() = when (windowManager.defaultDisplay.rotation) {
      Surface.ROTATION_270 -> 270
      Surface.ROTATION_180 -> 180
      Surface.ROTATION_90 -> 90
      else -> 0
    }
}