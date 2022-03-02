package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.gnk.logger

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.ProgressBar
import androidx.camera.core.ImageProxy
import androidx.fragment.app.FragmentManager
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.maps.Overlays
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.FloorSelector
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.StatusUpdater
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.gnk.CvActivityBase
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.gnk.UiActivityCvBase
import cy.ac.ucy.cs.anyplace.lib.android.ui.settings.SettingsCvLoggerActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.AppInfo
import cy.ac.ucy.cs.anyplace.lib.android.utils.uTime
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.buttonUtils
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.*
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.gnk.*
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.gnk.Localization
import cy.ac.ucy.cs.anyplace.lib.databinding.ActivityCvLoggerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Encapsulating UI operations for the [CvLoggerActivity]
 */
class UiActivityCvLogger(
        activity: Activity,
        fragmentManager: FragmentManager,
        scope: CoroutineScope,
        statusUpdater: StatusUpdater,
        floorSelector: FloorSelector,
        overlays: Overlays,
        private val VM: CvLoggerViewModel,
        private val binding: ActivityCvLoggerBinding) :
        UiActivityCvBase(activity,
                fragmentManager,
                VM as CvViewModelBase,
                scope, statusUpdater, overlays,
                floorSelector) {

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

  fun bindCvStatsText() { // TODO:PM: NAV COMMON shared between activities?
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
        VM.resetLoggingWindow()
        statusUpdater.hideStatus()
      }
    }
  }

  fun setupClickSettingsMenuButton() {
    LOG.D2()

    // Setups a regular button to act as a menu button
    binding.buttonSettings.setOnClickListener { // TODO:PM
      val intent = Intent(activity, SettingsCvLoggerActivity::class.java)
      intent.putExtra(SettingsCvLoggerActivity.ARG_SPACE, VM.spaceH.toString())
      intent.putExtra(SettingsCvLoggerActivity.ARG_FLOORS, VM.floorsH.toString())
      intent.putExtra(SettingsCvLoggerActivity.ARG_FLOOR, VM.floorH.toString())
      activity.startActivity(intent)
    }

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
    statusUpdater.showWarningAutohide("Nothing stored.", "no objects attached on map", 5000L)
    VM.resetLoggingWindow()
    VM.logging.value = Logging.stopped
  }

  /**
   * It hides any active markers from the map, and if the detections are not empty:
   * - it merges detections with the local cache
   * - it updates the weighted heatmap
   */
  fun handleStoreDetections(gmap: GoogleMap) {
    storeDetectionsAndUpdateUI(gmap)
    VM.logging.value = Logging.stopped
  }

  /**
   * It stores detections using [VM], and updates the UI:
   * - shows warning when no detections captured
   * otherwise:
   * - clears the [gmap] markers
   * - updates the heatmap
   */
  fun storeDetectionsAndUpdateUI(gmap: GoogleMap) {
    // TODO show/enable an upload button
    VM.hideActiveMarkers()
    // an extra check in case of a forced storing (long click while running or paused mode)
    if (VM.storedDetections.isEmpty()) {
      val msg = "Nothing stored."
      LOG.W(TAG, msg)
      statusUpdater.showWarningAutohide(msg, 3000)
      return
    }

    val detectionsToStored = VM.storedDetections.size
    VM.storeDetections(VM.floorH)
    VM.cvMapH?.let { overlays.refreshHeatmap(gmap, it.getWeightedLocationList()) }
    statusUpdater.showWarningAutohide("stored $detectionsToStored locations", 3000)
  }

  fun setupClickedLoggingButton(gmap: GoogleMap) {
    // when logging button is clicked and we must store,
    // else: toggle logging
    binding.bottomUi.buttonLogging.setOnClickListener {
      LOG.D(TAG, "buttonStartLogging: ${VM.logging}")
      when (VM.logging.value) {
        Logging.stoppedMustStore -> {
          if (VM.storedDetections.isEmpty()) {
            handleStoreNoDetections()
          } else {
            handleStoreDetections(gmap)
          }
        }
        else ->
          VM.toggleLogging()
      }
    }

    // CHECK:PM CLR:PM SIMPLIFY
    //logging button long clicked: forcing store?!
    binding.bottomUi.buttonLogging.setOnLongClickListener {
      // val btnTimer = binding.bottomUi.buttonCameraTimer
      // VM.longClickFinished = true // CLR:PM remove this variable
      // TODO hide any stuff here...
      VM.circleTimerAnimation = TimerAnimation.reset
      binding.mapView.animateAlpha(1f, CvActivityBase.ANIMATION_DELAY)
      // buttonUtils.changeBackgroundButton(btnTimer, ctx, R.color.yellowDark)

      // LEFTHERE: test this
      statusUpdater.showInfoAutohide("stored ${VM.storedDetections.size} locations", 3000)
      handleStoreDetections(gmap)
      true
    }
  }

  fun startLocalization(mapView: MapView) {
    val btnDemoNav = binding.buttonDemoNavigation
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
    val btnDemoNav = binding.buttonDemoNavigation
    statusUpdater.clearStatus()
    buttonUtils.changeBackgroundButton(btnDemoNav, ctx, R.color.darkGray)
    btnDemoNav.isEnabled = true
    mapView.alpha = 1f
    VM.localization.tryEmit(Localization.stopped)
  }

  fun setupClickDemoNavigation(mapView: MapView) {
    binding.buttonDemoNavigation.setOnClickListener {
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
    binding.buttonDemoNavigation.setOnLongClickListener {
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

    val model = VMb.detector.getDetectionModel()
    @SuppressLint("SetTextI18n")
    binding.bottomUi.cropInfo.text = "${model.inputSize}x${model.inputSize}"
  }


}