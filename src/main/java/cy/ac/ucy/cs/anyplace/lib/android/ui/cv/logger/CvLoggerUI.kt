package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger

import android.content.Context
import android.widget.Toast
import androidx.constraintlayout.widget.Group
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.BuildConfig
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.CvUI
import cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs.ConfirmActionDialog
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvLoggerViewModel
import cy.ac.ucy.cs.anyplace.lib.android.ui.settings.MainSettingsDialog
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.UtilUI
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.LoggingMode
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Coord
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.UserCoordinates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

open class CvLoggerUI(private val act: CvLoggerActivity,
                      private val scope: CoroutineScope,
                      private val VM: CvLoggerViewModel,
                      private val ui: CvUI) {
  private val TG = "ui-cv-logger"
  private val app = act.app
  private val notify = app.notify

  fun onInferenceRan() {
    val MT = ::onInferenceRan.name
    LOG.V2(TG, MT)
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
  val btnSettings: MaterialButton by lazy { act.findViewById(R.id.button_settings) }

  val groupUpload: Group by lazy { act.findViewById(R.id.group_uploadOffline) }
  val btnUpload: MaterialButton by lazy { act.findViewById(R.id.btn_upload) }
  val btnUploadDiscard: MaterialButton by lazy { act.findViewById(R.id.btn_uploadDiscard) }

  /**
   * Stores some detections (if not empty) to local cache.
   */
  fun setupOnMapLongClick() {
    val MT = ::setupOnMapLongClick.name
    LOG.E(TG, "$MT: setup: (long-click)")

    scope.launch(Dispatchers.IO) {
      delay(200) // why is this? workaround for getting last objects filled in?
      scope.launch(Dispatchers.Main) {
        ui.map.obj.setOnMapLongClickListener { location ->

          scope.launch(Dispatchers.IO) {
            LOG.I(TG, "$MT: storing detections (long-click)")
            if (VM.canStoreDetections()) {
              LOG.V3(TG, "clicked at: $location")
              handleStoreDetections(location)
            } else {

              val msg = "Scan some objects first!"
              if (app.dsMisc.showTutorialLoggerMapLongPress()) {
                val msgTut= "LOGGER LONG-PRESS:\nused only to assign objects on the map.\nResult: $msg"
                notify.TUTORIAL(scope, msgTut)
              } else {
                notify.long(scope, msg)
              }
              utlUi.attentionZoom(bottom.logging.btn)
            }
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
    val MT = ::showLocalizationButton.name
    LOG.D2(TG,"$MT:")
    if (!hasFingerprints && canPerformLocalization()) {
      LOG.W(TG,"$MT: showing!")
     ui.localization.show()
    }
  }

  // CHECK: PMX: UPL: in future: shouldn't be invisible?
  fun canPerformLocalization() =
          VM.statusLogging.value == LoggingMode.stopped
          // groupUpload.isVisible || VM.statusLogging.value == LoggingStatus.stopped

  fun setupButtonSettings() {
    val MT = ::setupButtonSettings.name
    LOG.D2(TG, MT)
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
    val MT = ::handleStoreDetections.name
    LOG.E(TG, "$MT")

    val windowDetections = VM.objWindowLOG.value.orEmpty().size

    if (app.wLevel==null) {
      app.showToast(scope, "Cannot store detections. (null floor)", Toast.LENGTH_LONG)
      resetLogging()
      return
    }

    val LW = app.wLevel!!
    val SW = LW.wSpace

    // find the floor
    val userCoord = UserCoordinates(SW.obj.buid,
            LW.obj.number.toInt(),
            location.latitude, location.longitude)

    VM.cacheDetectionsLocally(userCoord, location)
    checkForUploadCache(true)

    bottom.logging.showUploadBtn()

    // add marker
    val curPoint = VM.objOnMAP.size.toString()
    val msg = "Scan: $curPoint"
    val snippet="Objects: $windowDetections\n${LW.prettyFloorCapitalize}: ${LW.obj.number}"
    val coord = Coord(userCoord.lat, userCoord.lon, userCoord.level)
    ui.map.markers.addScanMarker(coord, msg, snippet)
    ui.map.moveIfOutOufBounds(location)

    resetLogging()
  }

  fun resetLogging() {
    val MT = ::resetLogging.name
    LOG.D(TG, MT)
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
    val MT = ::checkForUploadCache.name
    scope.launch(Dispatchers.IO) {
      if (VM.cache.hasFingerprints()) {
        // wait for the bottom sheet to be initialized
        while(!uiBottomLazilyInited) delay(100)

        bottom.logging.showUploadBtn()
        // val msg = "Please upload local fingerprints to the cloud."
        // app.showToast(scope, msg, Toast.LENGTH_LONG)
      } else {
        utlUi.fadeOut(groupUpload)

        // only when required (as markers are lazily initialized)
        if(clearMarkers) ui.map.markers.hideScanMarkers()

        LOG.E(TG, "call: showLocalizationButton checkForUploadCache..")
        showLocalizationButton()  // show again localization button
      }
    }
  }

  var uploadButtonInit = false
  fun setupUploadBtn() {
    val MT = ::setupUploadBtn.name
    uploadButtonInit = true

    LOG.D(TG, "$MT: setup upload button")
    btnUpload.setOnClickListener {
      LOG.W(TG, "$MT: clicked upload")
      scope.launch(Dispatchers.IO) { // TODO:PMX UPL OK?
        utlUi.disable(groupUpload)
        utlUi.disable(btnUpload)
        utlUi.disable(btnUploadDiscard)
        utlUi.text(btnUpload, ctx.getString(R.string.uploading))
        utlUi.changeMaterialIcon(btnUpload, R.drawable.ic_cloud_sync)
        VM.nwCvFingerprintSend.uploadFromCache(this@CvLoggerUI)
      }
    }

    btnUploadDiscard.setOnClickListener {
      val mgr = act.supportFragmentManager
      val title = "Discard local scans"
      val num = VM.cache.countFingerprintsCacheLines()
      val msg = "Will delete $num scans that have not been uploaded yet to the backend.\n"

      ConfirmActionDialog.SHOW(mgr, title, msg, cancellable = true, isImportant = true) { // on confirmed
        VM.cache.deleteFingerprintsCache()
        checkForUploadCache(true)
      }
    }
  }
}