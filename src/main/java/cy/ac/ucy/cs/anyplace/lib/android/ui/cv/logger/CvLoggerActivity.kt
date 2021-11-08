package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.camera.core.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.CvActivityBase
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.buttonUtils.changeBackgroundButton
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.buttonUtils.changeBackgroundButtonCompat
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.buttonUtils.removeMaterialButtonIcon
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.CvLoggerViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.CvViewModelBase
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.Logging
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.TimerAnimation
import cy.ac.ucy.cs.anyplace.lib.databinding.ActivityCvLoggerBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class CvLoggerActivity : CvActivityBase(), OnMapReadyCallback {

  // private var floormapOverlay: GroundOverlay? = null CLR: PM
  // private val overlays by lazy { Overlays(applicationContext) }
  // private val assetReader by lazy { AssetReader(applicationContext) }
  // private lateinit var map: GoogleMap

  private lateinit var binding: ActivityCvLoggerBinding
  private lateinit var viewModel: CvLoggerViewModel

  private lateinit var statusUpdater: StatusUpdater

  private lateinit var UI: UiActivityCvLogger

  /** kept here (not in viewModel) as we want this to be reset on lifecycle updates */

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityCvLoggerBinding.inflate(layoutInflater)
    setContentView(binding.root)
    viewModel = ViewModelProvider(this).get(CvLoggerViewModel::class.java)
    VMbase = viewModel

    statusUpdater = StatusUpdater(
      binding.tvStatusTitle,
      binding.tvStatusSubtitle,
      binding.viewStatusBackground,
      binding.viewWarning,
      applicationContext
    )

    UI = UiActivityCvLogger(
      applicationContext,
      lifecycleScope,
      statusUpdater,
      viewModel,
      binding
    )

    setupComputerVision()
    setupMap()
    setupButtonsAndUi()
  }

  override fun onResume() {
    super.onResume()
    LOG.D3(TAG, "onResume")
    readPrefsAndContinueSetup()
  }

  /**
   * Read [dataStoreCvLogger] preferences, as some UI elements depend on them:
   * - Developer Stats -> [binding.bottomUi]
   */
  private fun readPrefsAndContinueSetup() {
    LOG.D4(TAG, "readPrefsAndSetupBottomSheet")
    lifecycleScope.launch {
      dataStoreCvLogger.read.first { prefs ->
        viewModel.prefs = prefs
        // set up that depends on preferences
        UI.setUpBottomSheet()

        true
      }
    }
  }

  /**
   * Observes [viewModel.objectDetectionsAll] changes and updates
   * [binding.bottomUi.buttonCameraTimer] accordingly.
   */
  private fun observeObjectDetections() {
    viewModel.objectsWindowAll.observeForever { detections ->
      binding.bottomUi.tvWindowObjectsAll.text = detections.toString()
    }
  }

  private fun observeLoggingStatus() {
    viewModel.status.observeForever { status ->
      LOG.D("logging: $status")
      updatedLoggingUI(status)
    }
  }

  private fun setupComputerVision() {
    super.setupComputerVision(binding.tovCamera, binding.pvCamera)
    observeObjectDetections()
    observeLoggingStatus()
  }

  private fun setupButtonsAndUi() {
    checkInternet()
    UI.setupClickedLoggingButton()
    UI.setupClickCameraTimerCircleButton()
    UI.setupClickClearObjectsPopup()
    UI.setupClickSettingsMenuButton()
  }

  @SuppressLint("SetTextI18n")
  private fun updatedLoggingUI(status: Logging) {
    LOG.D4("updateScanningButton: $status")
    val btnLogging = binding.bottomUi.buttonLogging
    val btnTimer = binding.bottomUi.buttonCameraTimer

    binding.bottomUi.groupTutorial.visibility = View.GONE

    when (status) {
      Logging.finished -> { // finished a scanning
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
        viewModel.circleTimerAnimation = TimerAnimation.running
        btnLogging.text = "PAUSE"
        removeMaterialButtonIcon(btnTimer)
        changeBackgroundButtonCompat(btnLogging, applicationContext, R.color.darkGray)
        changeBackgroundButton(btnTimer, applicationContext, R.color.redDark)
        btnTimer.fadeIn()
        binding.mapView.animateAlpha(OPACITY_MAP_LOGGING, ANIMATION_DELAY)
      }
      Logging.stopped -> { // stopped after a pause or a store: can start logging again
        viewModel.circleTimerAnimation = TimerAnimation.paused
        if (viewModel.previouslyPaused) {
          btnLogging.text = "RESUME"
        } else {
          btnLogging.text = "START"
          binding.bottomUi.groupTutorial.visibility = View.VISIBLE
        }
        changeBackgroundButtonCompat(btnLogging, applicationContext, R.color.colorPrimary)
        binding.mapView.animateAlpha(1f, ANIMATION_DELAY)
        changeBackgroundButton(btnTimer, applicationContext, R.color.darkGray)
      }
      Logging.stoppedNoDetections -> { // stopped after no detections: retry a scan
        viewModel.circleTimerAnimation = TimerAnimation.reset
        lifecycleScope.launch {
          statusUpdater.showWarningAutohide("No detections.", "trying again..", 2000L)
          restartLogging()
        }

      }
      Logging.stoppedMustStore -> {
        viewModel.circleTimerAnimation = TimerAnimation.reset
        btnLogging.text = "FINISH"
        binding.mapView.animateAlpha(1f, ANIMATION_DELAY)
        changeBackgroundButton(btnTimer, applicationContext, R.color.yellowDark)

        val nothingStored = viewModel.storedDetections.isEmpty()
        val loggingBtnColor = if (nothingStored) R.color.darkGray else R.color.yellowDark
        changeBackgroundButtonCompat(btnLogging, applicationContext, loggingBtnColor)
      }
    }
  }

  @SuppressLint("UnsafeOptInUsageError", "SetTextI18n")
  override fun analyzeImage(image: ImageProxy) {
    lifecycleScope.launch(Dispatchers.Default) {
      image.use {
        if (!viewModel.imageConvertedIsSetUpped()) {
          setUpImageConverter(image)
        }

        val detectionTime = viewModel.detectObjectsOnImage(image)
        withContext(Dispatchers.Main) {
          UI.bindCvStatsImgDimensions(image) // TODO put bindCvStatsText
          binding.bottomUi.timeInfo.text = "${detectionTime}ms"
          UI.updateCameraTimerButton()
          UI.bindCvStatsText()
        }
      }
    }
  }


  /**
   * Stores some measurements on the given GPS locations
   */
  override fun setupOnMapLongClick() {
    map.setOnMapLongClickListener { location ->
      if (viewModel.canStoreObjects()) {
        LOG.D("clicked at: $location")

        // re-center map
        map.animateCamera(
          CameraUpdateFactory.newCameraPosition(
            CameraPosition(
              location, map.cameraPosition.zoom,
              // don't alter tilt/bearing
              map.cameraPosition.tilt,
              map.cameraPosition.bearing
            )
          )
        )

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
        val msg = "Not in logging mode"
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
  private suspend fun restartLogging(delayMs: Long = 500) {
    delay(delayMs)
    statusUpdater.hideStatus()
    removeMaterialButtonIcon(binding.bottomUi.buttonCameraTimer)
    changeBackgroundButton(binding.bottomUi.buttonCameraTimer, applicationContext, R.color.darkGray)
    changeBackgroundButtonCompat(
      binding.bottomUi.buttonLogging,
      applicationContext,
      R.color.colorPrimary
    )
    binding.mapView.animateAlpha(1f, ANIMATION_DELAY)
    viewModel.startNewWindow()
  }

}