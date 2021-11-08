package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.camera.core.ImageProxy
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.cv.misc.Constants
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.fadeIn
import cy.ac.ucy.cs.anyplace.lib.android.extensions.fadeOut
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.UiActivityCvBase
import cy.ac.ucy.cs.anyplace.lib.android.utils.AppInfo
import cy.ac.ucy.cs.anyplace.lib.android.utils.uTime
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.buttonUtils
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.CvLoggerViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.Logging
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.TimerAnimation
import cy.ac.ucy.cs.anyplace.lib.databinding.ActivityCvLoggerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Encapsulating UI operations for the [CvLoggerActivity]
 */
class UiActivityCvLogger(
  ctx: Context,
  scope: CoroutineScope,
  statusUpdater: StatusUpdater,
  private val viewModel: CvLoggerViewModel,
  private val binding: ActivityCvLoggerBinding,
) : UiActivityCvBase(ctx, scope, statusUpdater) {

  private val appInfo by lazy { AppInfo(ctx) }
  private var clearConfirm=false
  private var clickedScannedObjects=false

  /**
   * Observes [viewModel.windowDetections] changes and updates
   * [binding.bottomUi.buttonCameraWindow] accordingly.
   */
  fun updateCameraTimerButton() {
    val elapsed = viewModel.getElapsedSeconds()
    val remaining = (viewModel.prefs.windowSeconds.toInt()) - elapsed
    val btn = binding.bottomUi.buttonCameraTimer
    val progressBar = binding.bottomUi.progressBarTimer

    if (remaining>0) {
      val windowSecs = viewModel.prefs.windowSeconds.toInt()
      setupProgressBarTimerAnimation(btn, progressBar, windowSecs)
      btn.text = uTime.getSecondsRounded(remaining, windowSecs)
    } else {
      progressBar.visibility = View.INVISIBLE
      btn.text = ""
      progressBar.progress = 100
      if (!viewModel.storedDetections.values.isEmpty()) {
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

  @SuppressLint("SetTextI18n")
  fun bindCvStatsImgDimensions(image: ImageProxy) { // TODO:PM: NAV COMMON shared between activities?
    binding.bottomUi.frameInfo.text = "${image.width}x${image.height}"
  }

  fun bindCvStatsText() { // TODO:PM: NAV COMMONshared between activities?
    binding.bottomUi.tvElapsedTime.text=viewModel.getElapsedSecondsStr()
    binding.bottomUi.tvWindowObjectsUnique.text=viewModel.objectsWindowUnique.toString()
    binding.bottomUi.tvCurrentWindow.text=viewModel.storedDetections.size.toString()
    binding.bottomUi.tvTotalObjects.text=viewModel.objectsTotal.toString()
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
      if (viewModel.objectsWindowUnique > 0 &&!clickedScannedObjects) {
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
        viewModel.resetWindow()
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
      Toast.makeText(ctx,"App Version: ${appInfo.version}", Toast.LENGTH_SHORT).show()
      true
    }
  }

  fun setupClickedLoggingButton() {
    binding.bottomUi.buttonLogging.setOnClickListener {
      LOG.D(TAG, "buttonStartLogging: ${viewModel.status}")
      when (viewModel.status.value) {
        Logging.stoppedMustStore -> {

          if (viewModel.storedDetections.isEmpty()) {
            scope.launch {
              statusUpdater.showWarningAutohide("Nothing on map to store.",
                "TIP: long-click a location to attach objects.", 3000L)
            }

          } else {
            // TODO hide tutorial once started....

            // TODO LEFTHERE
            // TODO:PM long click on existing point: update existing measurements..
            // SHOW MSG: This will ignore the last  'window'
            // MATERIALIZE, STORE, and EXIT
            // val msg = "TODO: store locally.."
            viewModel.status.value = Logging.finished
            // setUpBottomSheet()
            // TODO: STORE RESULTS..
            // TODO put an upload button and enable it..

            viewModel.hideActiveMarkers()
            // TODO:PM heatmap saved results

            // statusUpdater.showWarning(msg)
          }
        }
        else ->
          viewModel.toggleLogging()
      }
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
      val height: Int = gestureLayout.measuredHeight
      sheetBehavior.peekHeight = (height/2f).roundToInt()+50
      LOG.V5(TAG, "peek height: ${sheetBehavior.peekHeight}")
    }

    @SuppressLint("SetTextI18n")
    binding.bottomUi.cropInfo.text =
      "${Constants.DETECTION_MODEL.inputSize}x${Constants.DETECTION_MODEL.inputSize}"
  }


}