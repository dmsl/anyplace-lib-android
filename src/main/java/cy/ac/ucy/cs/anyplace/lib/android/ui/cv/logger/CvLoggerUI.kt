package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger

import android.content.Context
import android.widget.Toast
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.BuildConfig
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.CvUI
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvLoggerViewModel
import cy.ac.ucy.cs.anyplace.lib.android.ui.settings.MainSettingsDialog
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.UtilUI
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.LoggingStatus
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.UserCoordinates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

open class CvLoggerUI(private val act: CvLoggerActivity,
                      private val scope: CoroutineScope,
                      private val VM: CvLoggerViewModel,
                      private val ui: CvUI) {

  private val app = act.app

  fun onInferenceRan() {
    LOG.V2(TAG, "$METHOD: CvLoggerUI")
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
  var uiBottomLazilyInited = false
  val ctx : Context = act

  // UI COMPONENTS:
  // CHECK: this was in bottom sheet?
  val btnSettings: MaterialButton by lazy { act.findViewById(R.id.button_settings) }
  val btnUpload: MaterialButton by lazy { act.findViewById(R.id.button_upload) }

  /**
   * Stores some detections (if not empty) to local cache.
   */
  fun setupOnMapLongClick() {
    val method = METHOD
    val tag = TAG
    LOG.E(tag, "$method: setup: (long-click)")

    scope.launch(Dispatchers.IO) {
      delay(200)
      scope.launch(Dispatchers.Main) {
        ui.map.obj.setOnMapLongClickListener { location ->
          LOG.W(tag, "$method: storing detections (long-click)")
          if (VM.canStoreDetections()) {
            LOG.V3(tag, "clicked at: $location")
            handleStoreDetections(location)
          } else {
            app.showToast(scope, "Scan some objects first!")
          }
        }
      }
    }
  }


  /**
   * Optionally passing whether there are fingerprints stored
   * (due to async, checking whether the button is visible is not enough)
   *
   * probably this logic needs to be reworked..
   */
  fun showLocalizationButton(hasFingerprints: Boolean = false)  {
    LOG.D2(TAG,"$METHOD:")
    if (!hasFingerprints && canPerformLocalization()) {
      LOG.W(TAG,"$METHOD: showing!")
     ui.localization.show()
    }
  }

  // CHECK: TODO: PMX: UPL: in future: shouldn't be invisible?
  fun canPerformLocalization() =
          VM.statusLogging.value == LoggingStatus.stopped
          // btnUpload.isVisible || VM.statusLogging.value == LoggingStatus.stopped

  fun setupButtonSettings() {
    LOG.D2()
    btnSettings.setOnClickListener {
      val versionStr = BuildConfig.VERSION_CODE
      MainSettingsDialog.SHOW(act.supportFragmentManager,
              MainSettingsDialog.FROM_MAIN, act, versionStr)
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

    if (app.wFloor==null) {
      app.showToast(scope, "Cannot store detections. (null floor)", Toast.LENGTH_LONG)
      resetLogging()
      return
    }

    val FW = app.wFloor!!
    val SW = FW.spaceH

    // find the floor
    val userCoord = UserCoordinates(SW.obj.id,
            FW.obj.floorNumber.toInt(),
            location.latitude, location.longitude)

    VM.cacheDetectionsLocally(userCoord, location)
    checkForUploadCache(true) // TODO:PMX UPL: CLR this one?
    // bottom.logging.showUploadBtn() / /TODO:PMX UPL

    // add marker
    val curPoint = VM.objOnMAP.size.toString()
    val msg = "Point: $curPoint"
    // val snippet="$windowDetections D: ${FW.obj.floorNumber}" // TODO:PMX FR10
    val snippet="Objects: $windowDetections\n${FW.prettyFloor}: ${FW.obj.floorNumber}" // TODO:PMX FR10

    ui.map.markers.addCvMarker(location, msg, snippet)
    ui.map.recenterCamera(location)

    resetLogging()
  }


  fun resetLogging() {
    VM.resetLoggingWindow()
    bottom.timer.reset()
  }


  /**
   * Updates UI according to the offline cache
   * if there is cache:
   * - hide demo localization button (in loggeronly makes sense to test latest fingerprint)
   * - shows upload button and a prompted
   * else:
   * - shows localization button
   * - clears map markers
   * - hides upload button
   */
  fun checkForUploadCache(clearMarkers: Boolean=false) {
    scope.launch(Dispatchers.IO) {
      if (VM.cache.hasFingerprints()) {
        // wait for the bottom sheet to be initialized
        while(!uiBottomLazilyInited) delay(100)

        bottom.logging.showUploadBtn()
        // val msg = "Please upload local fingerprints to the cloud."
        // app.showToast(scope, msg, Toast.LENGTH_LONG)
      } else {
        utlUi.fadeOut(btnUpload)
        // only when required (as markers are lazily initialized)
        // TODO:PMX CVM
        // if(clearMarkers) ui.map.markers.hideCvObjMarkers()

        LOG.E(TAG, "call: showLocalizationButton checkForUploadCache..")
        showLocalizationButton()  // show again localization button
      }
    }
  }

  var uploadButtonInit = false
  fun setupUploadBtn() {
    uploadButtonInit = true

    LOG.E(TAG, "$METHOD: setup upload button")
    // btnUpload.setOnClickListener {
    //   LOG.E(TAG, "$METHOD: clicked upload")
    //   scope.launch(Dispatchers.IO) { // TODO:PMX UPL
    //     utlUi.disable(btnUpload)
    //     utlUi.text(btnUpload, ctx.getString(R.string.uploading))
    //     utlUi.changeMaterialIcon(btnUpload, R.drawable.ic_cloud_sync)
    //     VM.nwCvFingerprintSend.uploadFromCache(this@CvLoggerUI)
    //   }
    // }
  }
}