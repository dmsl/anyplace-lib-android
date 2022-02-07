package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.gnk.logger

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.camera.core.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.FloorSelector
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.gnk.CvActivityBase
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.buttonUtils.changeBackgroundButton
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.buttonUtils.changeBackgroundButtonCompat
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.buttonUtils.removeMaterialButtonIcon
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.CvLoggerViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.Localization
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.Logging
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.TimerAnimation
import cy.ac.ucy.cs.anyplace.lib.core.LocalizationResult
import cy.ac.ucy.cs.anyplace.lib.databinding.ActivityCvLoggerBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class CvLoggerActivity : CvActivityBase(), OnMapReadyCallback {
  private lateinit var binding: ActivityCvLoggerBinding
  private lateinit var VM: CvLoggerViewModel
  private lateinit var statusUpdater: StatusUpdater
  private lateinit var UI: UiActivityCvLogger

  // FRAGMENTS
  /** kept here (not in viewModel) as we want this to be reset on lifecycle updates */
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityCvLoggerBinding.inflate(layoutInflater)
    setContentView(binding.root)
    VM = ViewModelProvider(this).get(CvLoggerViewModel::class.java)
    VMB = VM

    statusUpdater = StatusUpdater(
            applicationContext,
            lifecycleScope,
            binding.tvStatusSticky,
            binding.tvMsgTitle,
            binding.tvMsgSubtitle,
            binding.viewStatusBackground,
            binding.viewWarning)

    floorSelector = FloorSelector(
            applicationContext,
            binding.groupFloorSelector,
            binding.textViewTitleFloor,
            binding.buttonSelectedFloor,
            binding.buttonFloorUp,
            binding.buttonFloorDown)

    UI = UiActivityCvLogger(
            this@CvLoggerActivity,
            supportFragmentManager,
            lifecycleScope,
            statusUpdater,
            floorSelector,
            overlays,
            VM,
            binding)
    UIB = UI

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
        VM.prefs = prefs
        // set up that depends on preferences
        UI.setUpBottomSheet()

        true
      }
    }
  }

  private fun setupComputerVision() {
    super.setupComputerVision(binding.tovCamera, binding.pvCamera)
    observeObjectDetections()
    observeLoggingStatus()

    // there is demo localization in Logger too,
    // to validate findings according to the latest CvMap
    collectLocalizationStatus()
    collectLocation()
  }

  /**
   * Observes [VM.objectDetectionsAll] changes and updates
   * [binding.bottomUi.buttonCameraTimer] accordingly.
   */
  private fun observeObjectDetections() {
    VM.objectsWindowAll.observeForever { detections ->
      binding.bottomUi.tvWindowObjectsAll.text = detections.toString()
    }
  }

  private fun observeLoggingStatus() {
    VM.logging.observeForever { status ->
      LOG.D(TAG_METHOD, "logging: $status")
      updateLoggingUi(status)
    }
  }

  private fun collectLocation() {
    lifecycleScope.launch{
      VM.location.collect { result ->
        when (result) {
          is LocalizationResult.Unset -> { }
          is LocalizationResult.Error -> {
            val msg = result.message.toString()
            val details = result.details
            if (details != null) {
              statusUpdater.showErrorAutohide(msg, details, 4000L)
            } else {
              statusUpdater.showErrorAutohide(msg, 4000L)
            }
          }
          is LocalizationResult.Success -> {
            result.coord?.let { VM.setUserLocation(it) }
            statusUpdater.showInfoAutohide("Found loc","XY: ${result.details}.", 3000L)
          }
        }
      }
    }
  }

  private fun collectLocalizationStatus() {
      lifecycleScope.launch{
      VM.localization.collect { status ->
        LOG.W(TAG_METHOD, "status: $status")
        when(status) {
          Localization.stopped -> {
            UI.endLocalization(binding.mapView)
            VM.logging.postValue(Logging.stopped)
          }
          else ->  {}
        }
      }
    }
  }

  private fun setupButtonsAndUi() {
    checkInternet()
    UI.setupClickCameraTimerCircleButton()
    UI.setupClickClearObjectsPopup()
    UI.setupClickSettingsMenuButton()
    UI.setupClickDemoNavigation(binding.mapView)

    UI.setupOnFloorSelectionClick()
  }

  @SuppressLint("SetTextI18n")
  private fun updateLoggingUi(status: Logging) {
    LOG.D4(TAG_METHOD, "status: $status")
    val btnLogging = binding.bottomUi.buttonLogging
    val btnDemoNav= binding.buttonDemoNavigation
    val btnTimer = binding.bottomUi.buttonCameraTimer
    binding.bottomUi.groupTutorial.visibility = View.GONE
    btnLogging.visibility = View.VISIBLE // hidden only by demo-nav

    when (status) {
      Logging.demoNavigation -> {
        btnLogging.visibility = View.INVISIBLE
        VM.circleTimerAnimation = TimerAnimation.reset
        UI.startLocalization(binding.mapView)
      }
      // Logging.finished -> { // finished a scanning
      //   btnDemoNav.visibility = View.GONE
      //   btnTimer.fadeOut()
      //   btnLogging.text = "Stored"
      //   changeBackgroundButtonCompat(btnLogging, applicationContext, R.color.green)
      //   // TODO:TRIAL LEFTHERE: new logic?
      //   // TODO:TRIAL onMapLongClick store on long-click
      //   // VM.circleTimerAnimation = TimerAnimation.reset
      //   // DetectionMapHelper.generate(storedDetections)
      //   // sleep a while.....
      //   // TODO show the below menu..
      //   // TODO store..
      //   // TODO show upload section..(on the below menu, with UPLOAD button)..
      //   // TODO set to stopped again
      // }
      Logging.running -> { // just started scanning
        btnDemoNav.fadeOut()
        VM.circleTimerAnimation = TimerAnimation.running
        btnLogging.text = "pause"
        removeMaterialButtonIcon(btnTimer)
        changeBackgroundButtonCompat(btnLogging, applicationContext, R.color.darkGray)
        changeBackgroundButton(btnTimer, applicationContext, R.color.redDark)
        btnTimer.fadeIn()
        binding.mapView.animateAlpha(OPACITY_MAP_LOGGING, ANIMATION_DELAY)
      }
      Logging.stopped -> { // stopped after a pause or a store: can start logging again
        btnDemoNav.fadeIn()
        // clear btnTimer related components.. TODO make this a class..
        VM.circleTimerAnimation = TimerAnimation.reset
        btnTimer.fadeOut()
        binding.bottomUi.progressBarTimer.fadeOut()
        VM.circleTimerAnimation = TimerAnimation.paused
        if (VM.previouslyPaused) {
          btnLogging.text = "resume"
        } else {
          btnLogging.text = "scan"
          binding.bottomUi.groupTutorial.visibility = View.VISIBLE
        }
        changeBackgroundButtonCompat(btnLogging, applicationContext, R.color.colorPrimary)
        binding.mapView.animateAlpha(1f, ANIMATION_DELAY)
        changeBackgroundButton(btnTimer, applicationContext, R.color.darkGray)
      }
      Logging.stoppedNoDetections -> { // stopped after no detections: retry a scan
        btnDemoNav.visibility = View.GONE
        VM.circleTimerAnimation = TimerAnimation.reset
        lifecycleScope.launch {
          val ms = 1500L
          statusUpdater.showWarningAutohide("No detections.", "trying again..", ms)
          delay(ms) // wait before restarting..
          restartLogging()
        }
      }
      Logging.stoppedMustStore -> {
        btnDemoNav.visibility = View.GONE
        VM.circleTimerAnimation = TimerAnimation.reset
        btnTimer.visibility=View.VISIBLE
        LOG.D(TAG_METHOD, "stopped must store: visible")

        binding.mapView.animateAlpha(1f, ANIMATION_DELAY)
        changeBackgroundButton(btnTimer, applicationContext, R.color.yellowDark)

        val storedDetections = VM.storedDetections.size
        val noDetections = storedDetections == 0
        val title="long-click on map"
        val subtitle = if (noDetections) "nothing new attached on map yet" else "mapped locations: $storedDetections"
        val delay = if(noDetections) 7000L else 5000L
        statusUpdater.showNormalAutohide(title, subtitle, delay)

        btnLogging.text = "END"
        // val loggingBtnColor = if (noDetections) R.color.darkGray else R.color.yellowDark
        // changeBackgroundButtonCompat(btnLogging, applicationContext, loggingBtnColor)
        changeBackgroundButtonCompat(btnLogging, applicationContext, R.color.darkGray)
      }
    }
  }

  @SuppressLint("UnsafeOptInUsageError", "SetTextI18n")
  override fun analyzeImage(image: ImageProxy) {
    lifecycleScope.launch(Dispatchers.Default) {
      image.use {
        if (!VM.imageConvertedIsSetUpped()) {
          setUpImageConverter(image)
        }

        val detectionTime = VM.detectObjectsOnImage(image)
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
   * Anything that relies on the [gmap].
   * - called by [CvActivityBase.onMapReady]
   */
  override fun onMapReadySpecialize() {
    UI.setupClickedLoggingButton(gmap)
    setupOnMapLongClick()
  }

  override fun onFloorplanLoadedSuccess() {
    LOG.W()
    // CLR this ?
  }

  /**
   * Stores some measurements on the given GPS locations
   */
  fun setupOnMapLongClick() {
    gmap.setOnMapLongClickListener { location ->
      if (VM.canStoreDetections()) {
        LOG.V3(TAG, "clicked at: $location")

        // re-center map
        gmap.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                        CameraPosition(
                                location, gmap.cameraPosition.zoom,
                                // don't alter tilt/bearing
                                gmap.cameraPosition.tilt,
                                gmap.cameraPosition.bearing)
                )
        )

        val windowDetections = VM.detectionsLogging.value.orEmpty().size
        VM.addDetections(location)

        // add marker
        val curPoint = VM.storedDetections.size.toString()
        val msg = "Point: $curPoint\n\nObjects: $windowDetections\n"
        VM.addMarker(location, msg)

        // pause a bit, then restart logging
        lifecycleScope.launch {
          restartLogging()
        }
        // binding.bottomUi.buttonCameraTimer.fadeOut()

      } else {
        val msg ="Not in scanning mode"
        statusUpdater.showWarningAutohide(msg, 2000)
        LOG.V2("onMapLongClick: $msg")
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
    LOG.D()
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
    VM.startNewWindow()
  }

}