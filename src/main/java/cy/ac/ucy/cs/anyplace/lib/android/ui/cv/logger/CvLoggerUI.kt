package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.ProgressBar
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.BuildConfig
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.UiStatusUpdater
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.CvMapUi
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvLoggerViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.Logging
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.TimerAnimation
import cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs.smas.MainSmasSettingsDialog
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.UtilUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

open class CvLoggerUI(private val act: CvLoggerActivity,
                      private val scope: CoroutineScope,
                      private val VM: CvLoggerViewModel,
                      private val ui: CvMapUi) {

  fun onInferenceRan() {
    ui.onInferenceRan()
    updateCameraTimerButton()
  }

  companion object {
    const val OPACITY_MAP_LOGGING = 0f
    const val ANIMATION_DELAY : Long = 100
  }

  private val utlUi by lazy { UtilUI(act, scope) }

  /** BottomSheet for the CvLogger */
  lateinit var bottom: BottomSheetCvLoggerUI
  val ctx : Context = act

  // UI COMPONENTS:
  // CHECK: this was in bottom sheet?
  val btnSettings: MaterialButton by lazy { act.findViewById(R.id.button_settings) }

  // val btnLogging : Button = binding.bottomUi.buttonLogging
  // val btnDemoNav= binding.btnDemoNavigation // CLR:PM all demo nav references?
  // val btnTimer = binding.bottomUi.buttonCameraTimer
  // val bottom = BottomSheetCvLoggerUI(act, VM, id_bottomsheet)

  val uiStatusUpdater by lazy {
    UiStatusUpdater(
            act, scope,
            act.findViewById(R.id.tv_statusSticky),
            act.findViewById(R.id.tv_msgTitle),
            act.findViewById(R.id.tv_msgSubtitle),
            act.findViewById(R.id.view_statusBackground),
            act.findViewById(R.id.view_warning))
  }

  // fun bindCvStatsImgDimensions(image: ImageProxy) { // TODO: shared between activities?
  //   binding.bottomUi.frameInfo.text = "${image.width}x${image.height}"
  // }


  /**
   * Observes [VM.windowDetections] changes and updates
   * [binding.bottomUi.buttonCameraWindow] accordingly.
   */
  fun updateCameraTimerButton() {
    val elapsed = VM.getElapsedSeconds()
    val remaining = (VM.prefsCvLog.windowLoggingSeconds.toInt()) - elapsed

    // TODO MERGE: must go through binding.bottomUi.buttonCameraTimer
    // val btn = act.findViewById<MaterialButton>(R.id.button_cameraTimer)
    // TODO MERGE: binding.bottomUi.progressBarTimer
    // val progressBar = act.findViewById<ProgressBar>(R.id.progressBar_timer)

    if (remaining>0) {
      val windowSecs = VM.prefsCvLog.windowLoggingSeconds.toInt()
      setupProgressBarTimerAnimation(windowSecs)
      bottom.btnTimer.text = utlTime.getSecondsRounded(remaining, windowSecs)
    } else {
      bottom.progressBarTimer.visibility = View.INVISIBLE
      bottom.btnTimer.text = ""
      bottom.progressBarTimer.progress = 100

      if (!VM.objWindowLOG.value.isNullOrEmpty()) {
        utlUi.changeMaterialIcon(bottom.btnTimer, R.drawable.ic_objects)
      } else {   // no results, hide the timer
        utlUi.removeMaterialIcon(bottom.btnTimer)
        utlUi.fadeOut(bottom.btnTimer)
      }
    }
  }

  /**
   * Initiate a circular progress bar animation, inside a coroutine for
   * smooth (and independent from other threads) updates.
   * It progresses according to the window time
   */
  private fun setupProgressBarTimerAnimation(windowSecs: Int) {
    // showing timer button but not yet the progress bar
    if (bottom.btnTimer.visibility == View.VISIBLE &&
            bottom.progressBarTimer.visibility != View.VISIBLE) {
      val delayMs = (windowSecs*1000/100).toLong()
      scope.launch {
        var progress = 0
        bottom.progressBarTimer.progress=progress
        bottom.progressBarTimer.visibility = View.VISIBLE
        while(progress < 100) {
          when (VM.circleTimerAnimation) {
            TimerAnimation.reset -> { resetCircleAnimation(bottom.progressBarTimer); break }
            TimerAnimation.running -> { bottom.progressBarTimer.setProgress(++progress, true) }
            TimerAnimation.paused -> {  }
          }
          delay(delayMs)
        }
      }
    }
  }


  /**
   * Stores some measurements on the given GPS locations
   */
  fun setupOnMapLongClick() {
    ui.map.obj.setOnMapLongClickListener { location ->
      LOG.V2(TAG, "$METHOD: storing detections")
      if (VM.canStoreDetections()) {
        LOG.V3(TAG, "clicked at: $location")

        // re-center map
        ui.map.obj.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                        CameraPosition(
                                location, ui.map.obj.cameraPosition.zoom,
                                // don't alter tilt/bearing
                                ui.map.obj.cameraPosition.tilt,
                                ui.map.obj.cameraPosition.bearing))
        )

        val windowDetections = VM.objWindowLOG.value.orEmpty().size

        VM.addDetections(VM.wFloor, VM.model, location)

        // add marker
        val curPoint = VM.objOnMAP.size.toString()
        val msg = "Point: $curPoint\n\nObjects: $windowDetections\n"

        ui.map.markers.addCvMarker(location, msg)
        // VM.addMarker(location, msg) // CLR:PM

        // pause a bit, then restart logging
        scope.launch {
          restartLogging()
        }
        // binding.bottomUi.buttonCameraTimer.fadeOut() CHECK:PM old comment

      } else {
        val msg ="Not in scanning mode"
        uiStatusUpdater.showWarningAutohide(msg, 2000)
        LOG.V2("onMapLongClick: $msg")
      }
    }
  }

  /**
   * TODO:PM put in UiLogger??
   *
   * MERGE: was updateLoggingUi
   */
  @SuppressLint("SetTextI18n")
  fun refresh(status: Logging) {
    LOG.D4(TAG_METHOD, "status: $status")
    // CLR:PM comments
    // val btnLogging = binding.bottomUi.buttonLogging
    // val btnDemoNav= binding.btnDemoNavigation
    // val btnTimer = binding.bottomUi.buttonCameraTimer
    bottom.groupTutorial.visibility = View.GONE
    // binding.bottomUi.groupTutorial.visibility = View.GONE
    bottom.btnLogging.visibility = View.VISIBLE // hidden only by demo-nav

    when (status) {
      // CLR:PM these were OLD comments (before merging)
      // Logging.finished -> { // finished a scanning
      //   btnDemoNav.visibility = View.GONE
      //   btnTimer.fadeOut()
      //   btnLogging.text = "Stored"
      //   changeBackgroundButtonCompat(btnLogging, applicationContext, R.color.green)
      //   // TODO:TRIAL new logic?
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
        ui.localization.hide()  // TODO:PMX FR1
        VM.circleTimerAnimation = TimerAnimation.running
        bottom.btnLogging.text = "pause"
        utlUi.removeMaterialIcon(bottom.btnTimer)
        utlUi.changeBackgroundCompat(bottom.btnLogging, R.color.darkGray)
        utlUi.changeBackgroundDONT_USE(bottom.btnTimer, R.color.redDark)
        utlUi.fadeIn(bottom.btnTimer)
        utlUi.animateAlpha(ui.map.mapView, OPACITY_MAP_LOGGING, ANIMATION_DELAY)
      }
      Logging.stopped -> { // stopped after a pause or a store: can start logging again
        ui.localization.show()
        // clear btnTimer related components.. TODO make this a class..
        VM.circleTimerAnimation = TimerAnimation.reset
        utlUi.fadeOut(bottom.btnTimer)
        utlUi.fadeOut(bottom.progressBarTimer)
        VM.circleTimerAnimation = TimerAnimation.paused
        if (VM.previouslyPaused) {
          bottom.btnLogging.text = "resume"
        } else {
          bottom.btnLogging.text = "scan"
          bottom.groupTutorial.visibility = View.VISIBLE
        }
        utlUi.changeBackgroundCompat(bottom.btnLogging, R.color.colorPrimary)
        utlUi.animateAlpha(ui.map.mapView, 1f, ANIMATION_DELAY)
        utlUi.changeBackgroundDONT_USE(bottom.btnTimer, R.color.darkGray)
      }
      Logging.stoppedNoDetections -> { // stopped after no detections: retry a scan
        ui.localization.visibilityGone()   // TODO:PMX FR1
        VM.circleTimerAnimation = TimerAnimation.reset
        scope.launch {
          val ms = 1500L
          uiStatusUpdater.showWarningAutohide("No detections.", "trying again..", ms)
          delay(ms) // wait before restarting..
          restartLogging()
        }
      }
      Logging.stoppedMustStore -> {
        ui.localization.visibilityGone() // TODO:PMX FR1
        VM.circleTimerAnimation = TimerAnimation.reset
        bottom.btnTimer.visibility= View.VISIBLE
        LOG.D(TAG_METHOD, "stopped must store: visible")
        utlUi.animateAlpha(ui.map.mapView, 1f, ANIMATION_DELAY)
        utlUi.changeBackgroundDONT_USE(bottom.btnTimer, R.color.yellowDark)

        val storedDetections = VM.objOnMAP.size
        val noDetections = storedDetections == 0
        val title="long-click on map"
        val subtitle = if (noDetections) "nothing new attached on map yet" else "mapped locations: $storedDetections"
        val delay = if(noDetections) 7000L else 5000L
        uiStatusUpdater.showNormalAutohide(title, subtitle, delay)

        bottom.btnLogging.text = "END"
        // val loggingBtnColor = if (noDetections) R.color.darkGray else R.color.yellowDark
        // changeBackgroundButtonCompat(btnLogging, applicationContext, loggingBtnColor)
        utlUi.changeBackgroundCompat(bottom.btnLogging, R.color.darkGray)
      }
    }
  }

  private fun resetCircleAnimation(progressBar: ProgressBar) {
    progressBar.visibility = View.INVISIBLE
    progressBar.progress=0
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
    uiStatusUpdater.hideStatus()
    utlUi.removeMaterialIcon(bottom.btnTimer)
    // CHECK:PM: replaced changeBackgroundButtonDONT_USE
    utlUi.changeBackgroundCompat(bottom.btnTimer, R.color.darkGray)
    utlUi.changeBackgroundCompat(bottom.btnLogging, R.color.colorPrimary)
    utlUi.animateAlpha(ui.map.mapView, 1f, ANIMATION_DELAY)
    VM.startNewWindow()
  }

  private var clearConfirm=false
  private var clickedScannedObjects=false
  /**
   * Allowing to clear the window that was just scanned
   *
   * MERGED w/ setupClickCameraTimerCircleButton
   */
  fun setupTimerButtonClick() {
    bottom.btnTimer.setOnClickListener {
      if (VM.objWindowUnique > 0 &&!clickedScannedObjects) {
        clickedScannedObjects=true
        utlUi.fadeIn(bottom.btnClearObj)
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
    bottom.btnClearObj.setOnClickListener {
      if (!clearConfirm) {
        clearConfirm = true
        bottom.btnClearObj.text = "Sure ?"
        bottom.btnClearObj.alpha = 1f
      } else {
        hideClearObjectsButton()
        utlUi.fadeOut(bottom.btnTimer)
        VM.resetLoggingWindow()
        uiStatusUpdater.hideStatus()
      }
    }
  }

  fun hideClearObjectsButton() {
    clearConfirm=false
    utlUi.fadeOut(bottom.btnClearObj)
    scope.launch {
      delay(100)
      bottom.btnClearObj.alpha = 0.5f
      bottom.btnClearObj.text = "Clear"
    }
  }

  fun setupButtonSettings() {
    LOG.D2()

    btnSettings.setOnClickListener {
      val versionStr = BuildConfig.VERSION_CODE
      MainSmasSettingsDialog.SHOW(act.supportFragmentManager,
              MainSmasSettingsDialog.FROM_MAIN, act, versionStr)
    }

    utlUi.changeBackgroundCompat(btnSettings, R.color.yellowDark)
  }


  /**
   * When logging button is clicked and we must store, else: toggle logging
   */
  fun setupClickedLoggingButton() {
    bottom.btnLogging.setOnClickListener {
      LOG.D(TAG, "buttonStartLogging: ${VM.logging}")
      when (VM.logging.value) {
        Logging.stoppedMustStore -> {
          if (VM.objOnMAP.isEmpty())  handleStoreNoDetections()
           else handleStoreDetections(ui.map.obj)
        }
        else -> VM.toggleLogging()
      }
    }

    // CLR:PM all below comments are OLD (pre-merge)
    // CLR:PM SIMPLIFY
    // logging button long clicked: forcing store?!
    bottom.btnLogging.setOnLongClickListener {
      LOG.E(TAG, "TODO: send logs to server")

      // val btnTimer = binding.bottomUi.buttonCameraTimer
      // VM.longClickFinished = true // CLR:PM remove this variable
      // TODO hide any stuff here...
      VM.circleTimerAnimation = TimerAnimation.reset
      utlUi.animateAlpha(ui.map.mapView, 1f, ANIMATION_DELAY)
      // buttonUtils.changeBackgroundButton(btnTimer, ctx, R.color.yellowDark)

      // this needs testing?
      uiStatusUpdater.showInfoAutohide("stored ${VM.objOnMAP.size} locations", 3000)
      handleStoreDetections(ui.map.obj)
      true
    }
  }

  fun handleStoreNoDetections() {
    uiStatusUpdater.showWarningAutohide("Nothing stored.", "no objects attached on map", 5000L)
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
    ui.map.markers.hideCvObjMarkers() // CLR:PM VM.hideActiveMarkers()

    // an extra check in case of a forced storing (long click while running or paused mode)
    if (VM.objOnMAP.isEmpty()) {
      val msg = "Nothing stored."
      LOG.W(TAG, msg)
      uiStatusUpdater.showWarningAutohide(msg, 3000)
      return
    }
    val detectionsToStored = VM.objOnMAP.size
    VM.storeDetections(VM.wFloor)
    VM.cvMapH?.let { ui.map.overlays.refreshHeatmap(gmap, it.getWeightedLocationList()) }
    uiStatusUpdater.showWarningAutohide("stored $detectionsToStored locations", 3000)
  }
}