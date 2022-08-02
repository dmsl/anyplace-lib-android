package cy.ac.ucy.cs.anyplace.lib.android.ui.components

import android.annotation.SuppressLint
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.viewModelScope
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.CvMapActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.GmapWrapper
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.SmasMainActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.toLatLng
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.UtilUI
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.LocalizationMode
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.TrackingMode
import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


/**
 * UI Localization Button
 */
class UiLocalization(
        private val act: CvMapActivity,
        private val app: AnyplaceApp,
        private val VM: CvViewModel,
        val scope: CoroutineScope,
        private val wMap: GmapWrapper,
        private val button_id_localization: Int,
        private val button_id_whereami: Int) {
  private val TG = "ui-cv-localization"
  private val notify = app.notify

  private val ctx = act.applicationContext
  private val C by lazy { CONST(ctx) }
  private val utlUi by lazy { UtilUI(act, scope) }
  val btn: MaterialButton by lazy { act.findViewById(button_id_localization) }
  val btnWhereAmI: MaterialButton by lazy { act.findViewById(button_id_whereami) }
  val hasImuButton = false

  var set = false
  fun setup() {
    val MT = ::setup.name; if (set) return; set = true
    LOG.W(TG, MT)

    setupLocalizationMode()
    setupTrackingMode()
    setupButtonWhereAmI()
  }

  private fun setupLocalizationMode() {
    val MT = ::setupLocalizationMode.name
    btn.setOnClickListener{
      scope.launch(Dispatchers.IO) {
        if (VM.localizationMode.first() == LocalizationMode.running ||
                VM.trackingMode.first() == TrackingMode.on) {
          LOG.W(TG, "$MT: ignoring.. (localizing or tracking already..)")
          return@launch
        }

        VM.localizationMode.update { LocalizationMode.running }
        if (app.dsMisc.showTutorialNavLocalize()) {
         notify.TUTORIAL(scope, "LOCALIZATION: Camera opens to recognize objects and derive user location")
        }

      }
    }
  }

  /**
   * Tracking mode.
   * - only available in SMAS (not in logger)
   */
  private fun setupTrackingMode() {
    val MT = ::setupTrackingMode.name

    when (act) {
      is SmasMainActivity -> {
        LOG.W(TG, "$MT: LONG CLICK will setup")

        val tvSubtitle: TextView = act.findViewById(R.id.tvSubtitle)
        collectTrackingDetections(tvSubtitle)

        btn.setOnLongClickListener {
          scope.launch(Dispatchers.IO) {

            // when tracking mode is on, or localization mode is on (but not through tracking), ignore it
            if (VM.localizationMode.first() == LocalizationMode.running) {
              LOG.D(TG, "$MT: ignoring (already logging..)")
              return@launch
            }
            toggleTracking(VM.trackingMode.first(), tvSubtitle)
          }
          true
        }
      }
    }
  }

  private fun toggleTracking(mode: TrackingMode, tv: TextView) {
    val MT = ::toggleTracking.name
    LOG.E(TG, "$MT: mode: $mode")
    when (mode) {
      TrackingMode.off -> { startTrackingLoop(tv) }
      TrackingMode.on -> { endTrackingLoop(tv) }
    }
  }

  var setTrckDets=false
  @SuppressLint("SetTextI18n")
  private fun collectTrackingDetections(tv: TextView) {
    val MT = ::collectTrackingDetections.name
    if (setTrckDets) return; setTrckDets=true

    val txt="Location Tracking"
    scope.launch(Dispatchers.IO) {

      if (app.dsMisc.showTutorialNavTracking()) {
        val msg ="TRACKING: repeatedly performing localization\nLong-press again to disable" +
                "It automatically stops if many scan windows are empty."
        notify.TUTORIAL(scope, msg)
      }

      VM.detectionsTracking.collect {
        val detections = it.det
        LOG.E(TG, "$MT: detections: $detections")
        if (detections > 0) {
          utlUi.text(tv, "$txt ($detections)")
          VM.trackingEmptyWindowsConsecutive=0
        } else {
          utlUi.text(tv, txt)
          VM.trackingEmptyWindowsConsecutive++
          LOG.W(TG, "$MT: ${VM.trackingEmptyWindowsConsecutive} consecutive empty windows")
          if (VM.trackingEmptyWindowsConsecutive > VM.TRACKING_MAX_EMPTY_WINDOWS) {
            notify.WARN(scope, "Tracking auto-disabled.\n"+
                    "Reason: ${VM.TRACKING_MAX_EMPTY_WINDOWS} consecutive empty scans.")
            endTrackingLoop(tv)
          }
        }
      }
    }
  }

  private fun endTrackingLoop(tv: TextView) {
    val MT = ::endTrackingLoop.name
    utlUi.clearAnimation(btn)
    utlUi.clearAnimation(tv)
    utlUi.invisible(tv)
    VM.trackingMode.update { TrackingMode.off }
    VM.localizationMode.update { LocalizationMode.stopped }
  }

  private val TRACKING_DELAY_MS = 5000L
  fun startTrackingLoop(tv: TextView) {
    val MT = ::startTrackingLoop.name
    VM.trackingEmptyWindowsConsecutive=0
    VM.trackingMode.update { TrackingMode.on }

    scope.launch(Dispatchers.IO) {
      utlUi.flashingLoop(btn)
      utlUi.visible(tv)
      utlUi.flashingLoop(tv)

      val localizationWindow=app.dsCvMap.read.first().windowLocalizationMs
      val totalDelay=TRACKING_DELAY_MS+localizationWindow.toLong()

      while (VM.trackingMode.first() == TrackingMode.on)  {
        LOG.W(TG, "tracking loop..")
        VM.localizationMode.update { LocalizationMode.runningForTracking }
        delay(totalDelay)
      }
    }
  }

  var initedWhereAmI = false
  private fun setupButtonWhereAmI() {
    val MT = ::setupButtonWhereAmI.name
    LOG.E(TG, "$MT")
    btnWhereAmI.setOnClickListener {
      scope.launch {
        if (!DBG.WAI) return@launch

        val showTutorial = app.dsMisc.showTutorialNavWhereAmI()
        val lr = app.locationSmas.value
        var msg = ""
        if (lr is LocalizationResult.Success) {
          LOG.E(TG, "$MT: success")
          val coord = lr.coord!!
          val curFloor = app.wLevel?.levelNumber()

          if (coord.level != curFloor) {
            app.wLevels.moveToFloor(VM, coord.level)
          }

          VM.ui.map.markers.clearAllInfoWindow()
          VM.ui.map.moveToLocation(coord.toLatLng())
        } else {
          msg = "Localize or set location manually (long-press map)"
          if (!showTutorial) {
            notify.short(VM.viewModelScope, msg)
          }

          utlUi.attentionZoom(VM.ui.localization.btn)
          VM.ui.map.moveToLocation(app.wLevel!!.bounds().center)
        }

        if (showTutorial) {
          var tutMsg = "WHERE AM I:\ncenters map to your location.\n" +
                  "If unknown, it centers on the ${app.wSpace.prettyLevel}."
          if (msg.isNotEmpty()) tutMsg+="\nResult: $msg"

          notify.TUTORIAL(VM.viewModelScope, tutMsg)
        }

      }
    }
      initedWhereAmI = true
  }

  var collecting = false

  /**
   * Collect localization status
   */
  fun collectStatus() {
    val MT = ::collectStatus.name
    if (collecting) return; collecting =true

    scope.launch{
      VM.localizationMode.collect { status ->
        LOG.W(TG, "$MT: status: $status")
        when(status) {
          LocalizationMode.runningForTracking,
          LocalizationMode.running -> {
            VM.onLocalizationStarted()

            if (!app.cvUtils.isModelInited()) {
              notify.long(scope, C.ERR_NO_CV_CLASSES)
              return@collect
            }

            startLocalization()
          }
          LocalizationMode.stopped -> {
            VM.onLocalizationEnded()
            endLocalization()
          }
        }
      }
    }
  }

  fun endLocalization() {
    val MT = ::endLocalization.name
    VM.disableCvDetection()
    LOG.D2(TG, MT)
    utlUi.changeBackgroundMaterial(btn, R.color.gray)
    wMap.mapView.alpha = 1f
    VM.localizationMode.tryEmit(LocalizationMode.stopped)

    if (whereAmIWasVisible) {
      utlUi.fadeIn(btnWhereAmI)
      whereAmIWasVisible=false
    }

    if (VM.uiLoaded() && act.uiBottom.bottomSheetEnabled) {
      act.uiBottom.showBottomSheet()
    }

    utlUi.enable(act.btnSettings)
  }

  var whereAmIWasVisible=false
  fun startLocalization() {
    val MT = ::startLocalization.name

    LOG.D2(TG, MT)
    scope.launch(Dispatchers.IO) {
      VM.enableCvDetection()

      val tracking = VM.trackingMode.first() == TrackingMode.on
      VM.currentTime = System.currentTimeMillis()
      VM.windowStart = VM.currentTime
      VM.localizationMode.update {
        if (tracking) LocalizationMode.runningForTracking else LocalizationMode.running
      }
      utlUi.visible(btn)
      utlUi.changeBackgroundMaterial(btn, R.color.colorPrimary)

      if (!tracking) {
        val mapAlpha = VM.prefsCvMap.mapAlpha.toFloat()/100
        utlUi.alpha(wMap.mapView, mapAlpha)
      }

      if (btnWhereAmI.isVisible) {
        whereAmIWasVisible=true
        utlUi.fadeOut(btnWhereAmI)
      }
      utlUi.disable(act.btnSettings)
      act.uiBottom.hideBottomSheet()
    }
  }

  fun hide() = utlUi.fadeOut(btn)
  fun show() {
    // CLR:PM in this file...
    // if (isDisabled()) {
    //   enableLocalizationBtn()
    // } else {
    // }
    utlUi.fadeIn(btn)
  }

  // var disabledCause = ""
  // var disabledUserAction: Boolean = false
  // var disabledAttentionViews = mutableListOf<View>()
  // fun disableLocalizationBtn(cause: String, requireUserAction: Boolean, attentionViews: List<View>) {
  //   disabledCause=cause
  //   disabledUserAction=requireUserAction
  //   disabledAttentionViews.clear()
  //   disabledAttentionViews.addAll(attentionViews)
  //   utlUi.animateAlpha(btn, 0.5f)
  // // }
  // private fun enableLocalizationBtn() {
  //   disabledCause=""
  //   disabledUserAction=false
  //   disabledAttentionViews.clear()
  //   utlUi.animateAlpha(btn, 1f)
  // }
  // fun isDisabled() = disabledCause.isNotEmpty()

  var imuButtonInited = false
  lateinit var btnImu : MaterialButton
  fun setupButtonImu(btnImu: MaterialButton) {
    val MT = ::setupButtonImu.name
    if (imuButtonInited) return
    if (!DBG.uim) return

    this.btnImu=btnImu
    scope.launch(Dispatchers.IO) {
      // if (!app.hasDevMode()) {  // ONLY DEV MODE?!
      //   utlUi.gone(btnImu)
      //   return@launch
      // }

      LOG.W(TG, MT)
      imuButtonInited=true

      utlUi.visible(btnImu)


      btnImu.setOnClickListener {
        scope.launch(Dispatchers.IO) {
          if (app.dsMisc.showTutorialNavImu()) {
            notify.TUTORIAL(scope, "IMU Mode (experimental):\n"+
                    "It uses the accelerometer and compass to navigate.\nToggled with a long-click")
            return@launch
          }
        }
      }

      btnImu.setOnLongClickListener {
        scope.launch(Dispatchers.Main) {
          VM.imuEnabled=!VM.imuEnabled

          if (VM.imuEnabled) imuEnable() else imuDisable()

          when {
            !act.initedGmap -> {
              notify.long(scope, "Cannot start IMU: map not ready yet")
              imuDisable()
            }

            app.locationSmas.value.coord == null -> {
              notify.warn(scope, "IMU needs an initial location.")
              imuDisable()
            }

            VM.imuEnabled -> { VM.mu.start() }
          }
        }
        return@setOnLongClickListener true
      }
    }
  }

  /**
   * IMU cannot be used (at least for now) in conjunction with localization.
   * It is an experimental feature (proof of concept)
   */
  fun imuEnable() {
    scope.launch(Dispatchers.IO) {
      utlUi.changeBackgroundMaterial(btnImu, R.color.colorPrimary)
      utlUi.flashingLoop(btnImu)
      VM.imuEnabled=true
      notify.short(scope, "IMU enabled [experimental]")
    }
  }

  fun imuDisable() {
    scope.launch(Dispatchers.IO) {
      utlUi.changeBackgroundMaterial(btnImu, R.color.darkGray)
      utlUi.clearAnimation(btnImu)
      VM.imuEnabled=false
    }
  }

}