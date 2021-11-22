package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.ProgressBar
import androidx.camera.core.ImageProxy
import com.google.android.gms.maps.MapView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.cv.misc.Constants
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.maps.Overlays
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.CvActivityBase
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.UiActivityCvBase
import cy.ac.ucy.cs.anyplace.lib.android.utils.AppInfo
import cy.ac.ucy.cs.anyplace.lib.android.utils.uTime
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.buttonUtils
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.CvLoggerViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.Localization
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.Logging
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.TimerAnimation
import cy.ac.ucy.cs.anyplace.lib.databinding.ActivityCvLoggerBinding
import cy.ac.ucy.cs.anyplace.lib.models.CvMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Encapsulating UI operations for the [CvLoggerActivity]
 */
class UiActivityCvLogger(
  ctx: Context,
  scope: CoroutineScope,
  statusUpdater: StatusUpdater,
  overlays: Overlays,
  private val VM: CvLoggerViewModel,
  private val binding: ActivityCvLoggerBinding,
) : UiActivityCvBase(ctx, scope, statusUpdater, overlays) {

  private val appInfo by lazy { AppInfo(ctx) }
  private var clearConfirm=false
  private var clickedScannedObjects=false

  private var longClickClearCvMap=false


  /**
   * Observes [VM.windowDetections] changes and updates
   * [binding.bottomUi.buttonCameraWindow] accordingly.
   */
  fun updateCameraTimerButton() {
    val elapsed = VM.getElapsedSeconds()
    val remaining = (VM.prefs.windowLoggingSeconds.toInt()) - elapsed
    val btn = binding.bottomUi.buttonCameraTimer
    val progressBar = binding.bottomUi.progressBarTimer

    if (remaining>0) {
      val windowSecs = VM.prefs.windowLoggingSeconds.toInt()
      setupProgressBarTimerAnimation(btn, progressBar, windowSecs)
      btn.text = uTime.getSecondsRounded(remaining, windowSecs)
    } else {
      progressBar.visibility = View.INVISIBLE
      btn.text = ""
      progressBar.progress = 100

      if (!VM.detectionsLogging.value.isNullOrEmpty()) {
        buttonUtils.changeMaterialButtonIcon(btn, ctx, R.drawable.ic_objects)
      } else {   // no results, hide the timer
        buttonUtils.removeMaterialButtonIcon(btn)
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
      scope.launch {
        var progress = 0
        progressBar.progress=progress
        progressBar.visibility = View.VISIBLE
        while(progress < 100) {
          when (VM.circleTimerAnimation) {
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

  @SuppressLint("SetTextI18n")
  fun bindCvStatsImgDimensions(image: ImageProxy) { // TODO:PM: NAV COMMON shared between activities?
    binding.bottomUi.frameInfo.text = "${image.width}x${image.height}"
  }

  fun bindCvStatsText() { // TODO:PM: NAV COMMONshared between activities?
    binding.bottomUi.tvElapsedTime.text=VM.getElapsedSecondsStr()
    binding.bottomUi.tvWindowObjectsUnique.text=VM.objectsWindowUnique.toString()
    binding.bottomUi.tvCurrentWindow.text=VM.storedDetections.size.toString()
    binding.bottomUi.tvTotalObjects.text=VM.objectsTotal.toString()
  }

  fun hideClearObjectsButton() {
    clearConfirm=false
    binding.bottomUi.buttonClearObjects.fadeOut()
    scope.launch {
      delay(100)
      binding.bottomUi.buttonClearObjects.alpha = 0.5f
      binding.bottomUi.buttonClearObjects.text = "Clear"
    }
  }

  /**
   * Allowing to clear the window that was just scanned
   */
  fun setupClickCameraTimerCircleButton() {
    binding.bottomUi.buttonCameraTimer.setOnClickListener {
      if (VM.objectsWindowUnique > 0 &&!clickedScannedObjects) {
        clickedScannedObjects=true
        binding.bottomUi.buttonClearObjects.fadeIn()
        scope.launch {
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
  }

  fun setupClickClearObjectsPopup() {
    binding.bottomUi.buttonClearObjects.setOnClickListener {
      if (!clearConfirm) {
        clearConfirm = true
        binding.bottomUi.buttonClearObjects.text = "Sure ?"
        binding.bottomUi.buttonClearObjects.alpha = 1f
      } else {
        hideClearObjectsButton()
        binding.bottomUi.buttonCameraTimer.fadeOut()
        VM.resetWindow()
        statusUpdater.hideStatus()
      }
    }
  }

  fun setupClickSettingsMenuButton() {
    // // TODO:PM Settings
    // // Setups a regular button to act as a menu button
    //   binding.buttonSettings.setOnClickListener {
    //     SettingsDialog.SHOW(supportFragmentManager, SettingsDialog.FROM_CVLOGGER)
    //   }
    binding.buttonSettings.setOnLongClickListener {
      scope.launch {
        statusUpdater.showInfoAutohide("App Version: ${appInfo.version}", 1000L)
      }
      true
    }
  }

  fun handleStoreNoDetections() {
    scope.launch {
      statusUpdater.showWarningAutohide("Nothing on map to store.",
        "TIP: long-click a location to attach objects.", 3000L)
    }
  }

  /**
   * It hides any active markers from the map, and if the detections are not empty:
   * - it merges detections with the local cache
   * - it updates the weighted heatmap
   */
  fun handleStoreDetections() {
    // MATERIALIZE, STORE, and EXIT
    // TODO: STORE RESULTS..
    // TODO put an upload button and enable it..
    VM.logging.value = Logging.finished

    VM.hideActiveMarkers()

    // an extra check in case of a forced storing (long click while running or paused mode)
    if (VM.storedDetections.isEmpty()) {
      val msg = "No detections on map."
      LOG.W(TAG, msg)
      scope.launch {
        statusUpdater.showWarningAutohide(msg, "forced finish with long click..", 3000)
      }
      return
    }

    VM.storeDetections(VM.floorH)
    VM.cvMapH?.let { overlays.refreshHeatmap(it.getWeightedLocationList()) }
  }

  fun setupClickedLoggingButton() {
    binding.bottomUi.buttonLogging.setOnClickListener {
      LOG.D(TAG, "buttonStartLogging: ${VM.logging}")
      when (VM.logging.value) {
        Logging.stoppedMustStore -> {
          if (VM.storedDetections.isEmpty()) {
            handleStoreNoDetections()
          } else {
            handleStoreDetections()
          }
        }
        else ->
          VM.toggleLogging()
      }
    }

    binding.bottomUi.buttonLogging.setOnLongClickListener {
      // val btnTimer = binding.bottomUi.buttonCameraTimer
      // VM.longClickFinished = true // CLR:PM remove this variable
      // TODO hide any stuff here...
      VM.circleTimerAnimation = TimerAnimation.reset
      binding.mapView.animateAlpha(1f, CvActivityBase.ANIMATION_DELAY)
      // buttonUtils.changeBackgroundButton(btnTimer, ctx, R.color.yellowDark)

      handleStoreDetections()
      true
    }
  }

  fun startLocalization(mapView: MapView) {
    val btnDemoNav = binding.bottomUi.buttonDemoNavigation
    btnDemoNav.isEnabled = false
    VM.currentTime = System.currentTimeMillis()
    VM.windowStart = VM.currentTime
    VM.localization.value = Localization.running
    statusUpdater.setStatus("scanning..")
    btnDemoNav.visibility = View.VISIBLE
    buttonUtils.changeBackgroundButton(btnDemoNav, ctx, R.color.colorPrimary)
    mapView.alpha = 0.90f // TODO:PM no alpha..
  }

  fun endLocalization(mapView: MapView) {
    LOG.D2()
    val btnDemoNav = binding.bottomUi.buttonDemoNavigation
    statusUpdater.clearStatus()
    buttonUtils.changeBackgroundButton(btnDemoNav, ctx, R.color.darkGray)
    btnDemoNav.isEnabled = true
    mapView.alpha = 1f
    VM.localization.tryEmit(Localization.stopped)
  }

  fun setupClickDemoNavigation(mapView: MapView) {
    binding.bottomUi.buttonDemoNavigation.setOnClickListener {
      when (VM.logging.value) {
        Logging.stopped,
        Logging.stoppedMustStore -> {  // enter demo-nav mode
          VM.logging.postValue(Logging.demoNavigation)
        }
        // Logging.demoNavigation-> { // exit demo-nav mode:
        //   // stopLocalization(mapView)
        //   // VM.logging.postValue(Logging.stopped)
        // }
        else -> { // ignore click
          LOG.D(TAG_METHOD, "Ignoring Demo-Navigation. status: ${VM.logging}")
        }
      }
    }

    // CLR:PM TRIAL remove this functionality..
    binding.bottomUi.buttonDemoNavigation.setOnLongClickListener {
      if (!longClickClearCvMap) {
        scope.launch {
          statusUpdater.showWarningAutohide("Delete CvMap?", "long-click again", 2000L)
        }
        longClickClearCvMap = true
      } else {
        scope.launch {
          statusUpdater.showInfoAutohide("Deleted CvMap", 2000L)
        }
        VM.cvMapH?.clearCache()
      }

      true
    }
  }

  private fun hideBottomSheet() {
    binding.bottomUi.bottomSheetInternal.visibility = View.GONE
    binding.bottomUi.bottomSheetArrow.visibility = View.GONE
  }

  private fun showBottomSheet() {
    binding.bottomUi.bottomSheetInternal.visibility = View.VISIBLE
    binding.bottomUi.bottomSheetArrow.visibility = View.VISIBLE

    // hide developer options: TODO:PM once options are in place
    // if (viewModel.prefs.devMode) {
    binding.bottomUi.groupDevSettings.visibility = View.VISIBLE
    // }
  }

  fun setUpBottomSheet() {
    val sheetBehavior = BottomSheetBehavior.from(binding.bottomUi.root)
    sheetBehavior.isHideable = false
    // if (!forceShow && !viewModel.prefs.devMode) {
    //   hideBottomSheet()
    //   return
    // }

    showBottomSheet()

    val callback = CvLoggerBottomSheetCallback(binding.bottomUi.bottomSheetArrow)
    sheetBehavior.addBottomSheetCallback(callback)

    val gestureLayout = binding.bottomUi.gestureLayout
    gestureLayout.viewTreeObserver.addOnGlobalLayoutListener {
      sheetBehavior.peekHeight = binding.bottomUi.bottomSheetArrow.bottom + 60
      LOG.V4(TAG, "peek height: ${sheetBehavior.peekHeight}")
    }

    @SuppressLint("SetTextI18n")
    binding.bottomUi.cropInfo.text =
      "${Constants.DETECTION_MODEL.inputSize}x${Constants.DETECTION_MODEL.inputSize}"
  }


}