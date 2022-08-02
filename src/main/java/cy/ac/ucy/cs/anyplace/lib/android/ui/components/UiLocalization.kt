package cy.ac.ucy.cs.anyplace.lib.android.ui.components

import android.annotation.SuppressLint
import android.view.View
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

  fun setup() {
    setupBtnLocalization()
    setupButtonWhereAmI()
  }

  var btnLocalizationBtnSetup = false
  fun setupBtnLocalization() {
    val MT = ::setupBtnLocalization.name

    if (btnLocalizationBtnSetup) return
    btnLocalizationBtnSetup = true
    setupLocalizationMode()
    setupTrackingMode()
  }

  private fun setupLocalizationMode() {
    val MT = ::setupLocalizationMode.name

    btn.setOnClickListener{
      scope.launch(Dispatchers.IO) {
        if (VM.localizationMode.first() == LocalizationMode.running ||
                VM.trackingMode.first() == TrackingMode.on) {
          LOG.E(TG, "$MT: ignoring.. (localizing or tracking already..)")
          return@launch
        }

        if (isDisabled()) {
          if (disabledUserAction) {
            notify.INF(scope, disabledCause)
          } else {
            notify.short(scope, disabledCause)
          }
          if (disabledAttentionViews.isNotEmpty()) {
            disabledAttentionViews.forEach { utlUi.attentionZoom(it) }
          }
        } else {
          VM.localizationMode.update { LocalizationMode.running }
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
        LOG.E(TG, "$MT: LONG CLICK will setup")

        val tvSubtitle: TextView = act.findViewById(R.id.tvSubtitle)
        collectTrackingDetections(tvSubtitle)

        btn.setOnLongClickListener {
          scope.launch(Dispatchers.IO) {

            // when tracking mode is on, or localization mode is on (but not through tracking),
            // ignore it
            if (VM.localizationMode.first() == LocalizationMode.running) {
              LOG.E(TG, "$MT: ignoring (already logging..)")
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

  var collectingTrackingDetections=false
  @SuppressLint("SetTextI18n")
  private fun collectTrackingDetections(tv: TextView) {
    val MT = ::collectTrackingDetections.name
    if (collectingTrackingDetections) return
    collectingTrackingDetections=true

    val txt="Location Tracking"
    scope.launch(Dispatchers.IO) {
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

  var btnWhereAmISetup = false
  private fun setupButtonWhereAmI() {
    btnWhereAmI.setOnClickListener {
      if (!DBG.WAI) return@setOnClickListener

      val lr = app.locationSmas.value
      if (lr is LocalizationResult.Success) {
        val coord = lr.coord!!
        val curFloor = app.wLevel?.levelNumber()

        if (coord.level != curFloor) {
          app.wLevels.moveToFloor(VM, coord.level)
        }

        LOG.E(TG, "whereami click")
        VM.ui.map.markers.clearAllInfoWindow()
        VM.ui.map.moveToLocation(coord.toLatLng())
      } else {
        val msg = "For Where-Am-I, localize first or\nset location manually (long-press map)"
        notify.INF(VM.viewModelScope, msg)
        utlUi.attentionZoom(VM.ui.localization.btn)

        VM.ui.map.moveToLocation(app.wLevel!!.bounds().center)
      }
    }
    btnWhereAmISetup=true
  }

  var collecting = false

  /**
   * Collect localization status
   */
  fun collectStatus() {
    val MT = ::collectStatus.name
    if (collecting) return
    collecting =true

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
          else ->  {}
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
    if (isDisabled()) {
      enableLocalizationBtn()
    } else {
      utlUi.fadeIn(btn)
    }
  }

  var disabledCause = ""
  var disabledUserAction: Boolean = false
  var disabledAttentionViews = mutableListOf<View>()
  fun disableLocalizationBtn(cause: String, requireUserAction: Boolean, attentionViews: List<View>) {
    disabledCause=cause
    disabledUserAction=requireUserAction
    disabledAttentionViews.clear()
    disabledAttentionViews.addAll(attentionViews)
    utlUi.animateAlpha(btn, 0.5f)
  }

  private fun enableLocalizationBtn() {
    disabledCause=""
    disabledUserAction=false
    disabledAttentionViews.clear()
    utlUi.animateAlpha(btn, 1f)
  }

  fun isDisabled() = disabledCause.isNotEmpty()

  var imuButtonInited = false
  lateinit var btnImu : MaterialButton
  fun setupButtonImu(btnImu: MaterialButton) {
    val MT = ::setupButtonImu.name
    if (imuButtonInited) return
    if (!DBG.uim) return

    this.btnImu=btnImu
    scope.launch(Dispatchers.IO) {
      if (!app.hasDevMode()) {
        utlUi.gone(btnImu)
        return@launch
      }

      LOG.W(TG, MT)
      imuButtonInited=true

      utlUi.visible(btnImu)

      btnImu.setOnClickListener {
        VM.imuEnabled=!VM.imuEnabled

        if (VM.imuEnabled) imuEnable() else imuDisable()

        when {
          !act.initedGmap -> {
            notify.long(scope, "Cannot start IMU: map not ready yet")
            imuDisable()
          }

          app.locationSmas.value.coord == null -> {
            notify.shortDEV(scope, "IMU needs an initial location.")
            imuDisable()
          }

          VM.imuEnabled -> { VM.mu.start() }
        }
      }
    }
  }

  /**
   * IMU cannot be used (at least for now) in conjuction with localization.
   * It is an experimental feature (proof of concept)
   */
  fun imuEnable() {
    utlUi.changeBackgroundMaterial(btnImu, R.color.colorPrimary)
    utlUi.flashingLoop(btnImu)
    VM.imuEnabled=true
    notify.warn(scope, "IMU mode ON! [experimental]")
  }

  fun imuDisable() {
    utlUi.changeBackgroundMaterial(btnImu, R.color.darkGray)
    utlUi.clearAnimation(btnImu)
    VM.imuEnabled=false
  }

}