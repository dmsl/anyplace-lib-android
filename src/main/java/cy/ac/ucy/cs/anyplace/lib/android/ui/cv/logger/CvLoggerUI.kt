package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.BuildConfig
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.appSmas
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.CvCommonUI
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvLoggerViewModel
import cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs.smas.MainSmasSettingsDialog
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.UtilUI
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.LoggingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

open class CvLoggerUI(private val act: CvLoggerActivity,
                      private val scope: CoroutineScope,
                      private val VM: CvLoggerViewModel,
                      private val ui: CvCommonUI) {

  private val app = act.app

  fun onInferenceRan() {
    LOG.D2(TAG, "$METHOD: CvLoggerUI")

    bottom.timer.render()
    ui.onInferenceRan()
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
  val btnUpload: MaterialButton by lazy { act.findViewById(R.id.button_upload) }

  /**
   * Stores some detections (if not empty) to local cache.
   */
  fun setupOnMapLongClick() {
    ui.map.obj.setOnMapLongClickListener { location ->
      LOG.E(TAG, "$METHOD: storing detections (long-click)")
      if (VM.canStoreDetections()) {
        LOG.V3(TAG, "clicked at: $location")
        handleStoreDetections(location)
      } else {
        app.showToast(scope, "Scan some objects first!")
      }
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
   * It hides any active markers from the map, and if the detections are not empty:
   * - it merges detections with the local cache
   * - it updates the weighted heatmap
   */
  fun handleStoreDetections(location: LatLng) {
    LOG.E(TAG, "$METHOD")

    val windowDetections = VM.objWindowLOG.value.orEmpty().size
    VM.cacheDetectionsLocally(location)
    utlUi.fadeIn(btnUpload) // show upload button

    // add marker
    val curPoint = VM.objOnMAP.size.toString()
    val msg = "Point: $curPoint\n\nObjects: $windowDetections\n"

    ui.map.recenterCamera(location)
    ui.map.markers.addCvMarker(location, msg)

    VM.resetLoggingWindow()
    bottom.timer.reset()
  }

  fun checkForUploadCache(clearMarkers: Boolean=false) {
    scope.launch(Dispatchers.IO) {
      if (VM.cache.hasFingerprints()) {
        utlUi.fadeIn(btnUpload)
        val msg = "Please upload cached fingerprints to the cloud."
        app.showToast(scope, msg, Toast.LENGTH_LONG)
      } else {
        utlUi.fadeOut(btnUpload)
        // only when required (as markers are lazily initialized)
        if(clearMarkers) ui.map.markers.hideCvObjMarkers()
      }
    }

    utlUi.enable(btnUpload)
  }

  fun setupUploadBtn() {
    btnUpload.setOnClickListener {
      LOG.E(TAG, "$METHOD: clicked upload")
      scope.launch(Dispatchers.IO) {
        utlUi.disable(btnUpload)
        VM.nwCvFingerprintSend.uploadFromCache(this@CvLoggerUI)
      }
    }
  }


  /**
   *
   * DEPRECATED:
   * - used to store locally the detections in an optimized radio format.
   *
   * Everything is handled by the backend now..
   *
   * It stores detections using [VM], and updates the UI:
   * - shows warning when no detections captured
   * otherwise:
   * - clears the [gmap] markers
   * - updates the heatmap
   */
  @Deprecated("NOT USED - old offline cv-map storing code")
  fun storeDetectionsAndUpdateUI(gmap: GoogleMap) {
    ui.map.markers.hideCvObjMarkers()

    // an extra check in case of a forced storing (long click while running or paused mode)
    if (VM.objOnMAP.isEmpty()) {
      val msg = "Nothing stored."
      LOG.W(TAG, msg)
      return
    }
    // val detectionsToStored = VM.objOnMAP.size
    // VM.storeDetectionsLOCAL(VM.wFloor)
    // VM.cvMapH?.let { ui.map.overlays.refreshHeatmap(gmap, it.getWeightedLocationList()) }
  }
  /** CLR:PM after done?
   */
  @SuppressLint("SetTextI18n")
  @Deprecated("CLR")
  fun updateUi(status: LoggingStatus) {
    LOG.E(TAG, "$METHOD: status: $status")
    bottom.groupTutorial.visibility = View.GONE
    bottom.logging.btn.visibility = View.VISIBLE // hidden only by demo-nav

    when (status) {
      // MERGE:
      // LoggingStatus.running -> { // just started scanning
      //   ui.localization.hide()
      //   VM.circleTimerAnimation = TimerAnimation.running
      //   bottom.logging.btn.text = "pause (cancel?)"
      //   utlUi.removeMaterialIcon(bottom.btnTimer)
      //   utlUi.changeBackgroundCompat(bottom.logging.btn, R.color.darkGray)
      //   utlUi.changeBackgroundDONT_USE(bottom.btnTimer, R.color.redDark)
      //   utlUi.fadeIn(bottom.btnTimer)
      //   utlUi.animateAlpha(ui.map.mapView, OPACITY_MAP_LOGGING, ANIMATION_DELAY)
      // }

      // MERGE:
      // LoggingStatus.stopped -> { // stopped after a pause or a store: can start logging again
      // }
      // Logging.stoppedNoDetections -> { // stopped after no detections: retry a scan
      //   ui.localization.visibilityGone()   // TODO:PMX FR1
      //   VM.circleTimerAnimation = TimerAnimation.reset
      //   scope.launch {
      //     val ms = 1500L
      //     delay(ms) // wait before restarting..
      //     restartLogging()
      //   }
      // }
      // Logging.stoppedMustStore -> {
      //   ui.localization.visibilityGone() // TODO:PMX FR1
      //   VM.circleTimerAnimation = TimerAnimation.reset
      //   bottom.btnTimer.visibility= View.VISIBLE
      //   LOG.D(TAG_METHOD, "stopped must store: visible")
      //   utlUi.animateAlpha(ui.map.mapView, 1f, ANIMATION_DELAY)
      //   utlUi.changeBackgroundDONT_USE(bottom.btnTimer, R.color.yellowDark)
      //
      //   val storedDetections = VM.objOnMAP.size
      //   val noDetections = storedDetections == 0
      //   val title="long-click on map"
      //   val subtitle = if (noDetections) "nothing new attached on map yet" else "mapped locations: $storedDetections"
      //   val delay = if(noDetections) 7000L else 5000L
      //
      //   bottom.btnLogging.text = "END"
      //   // val loggingBtnColor = if (noDetections) R.color.darkGray else R.color.yellowDark
      //   // changeBackgroundButtonCompat(btnLogging, applicationContext, loggingBtnColor)
      //   utlUi.changeBackgroundCompat(bottom.btnLogging, R.color.darkGray)
      // }
    }
  }
}