package cy.ac.ucy.cs.anyplace.lib.android.ui.components

import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.viewModelScope
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.CvMapActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.GmapWrapper
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.toLatLng
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.UtilUI
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.LocalizationStatus
import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

  var btnLocalizationBtnSetup= false
  fun setupBtnLocalization() {
    if (btnLocalizationBtnSetup) return
    btnLocalizationBtnSetup = true
    btn.setOnClickListener {

      if (isDisabled()) {
        if (disabledUserAction) {
          app.snackbarInf(scope, disabledCause)
        } else {
          app.snackbarShort(scope, disabledCause)
        }

        if (disabledAttentionViews.isNotEmpty()) {
          disabledAttentionViews.forEach { utlUi.attentionZoom(it) }
        }


      } else {
        VM.statusLocalization.update { LocalizationStatus.running }
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
        val curFloor = app.wFloor?.floorNumber()

        if (coord.level != curFloor) {
          app.wFloors.moveToFloor(VM, coord.level)
        }

        LOG.E(TAG, "whereami click")
        VM.ui.map.markers.clearAllInfoWindow()
        VM.ui.map.animateToLocation(coord.toLatLng())
      } else {
        val msg = "For Where-Am-I, localize first or\nset location manually (long-press map)"
        app.snackbarInf(VM.viewModelScope, msg)
        utlUi.attentionZoom(VM.ui.localization.btn)
      }
    }
    btnWhereAmISetup=true
  }

  var collecting = false
  fun collectStatus() {
    if (collecting) return
    collecting =true

    scope.launch{
    VM.statusLocalization.collect { status ->
      LOG.W(TAG_METHOD, "status: $status")
      when(status) {
        LocalizationStatus.running -> {
          VM.onLocalizationStarted()

          if (!app.cvUtils.isModelInited()) {
            app.showToast(scope, C.ERR_NO_CV_CLASSES)
            return@collect
          }

          startLocalization()
        }
        LocalizationStatus.stopped -> {
          VM.onLocalizationEnded()
          endLocalization()
        }
        else ->  {}
      }
    }
    }
  }

  fun endLocalization() {
    VM.disableCvDetection()
    LOG.D2(TAG, "$METHOD")
    btn.isEnabled = true
    wMap.mapView.alpha = 1f
    VM.statusLocalization.tryEmit(LocalizationStatus.stopped)

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
    VM.enableCvDetection()
    LOG.D2(TAG, "$METHOD")
    btn.isEnabled=false
    VM.currentTime = System.currentTimeMillis()
    VM.windowStart = VM.currentTime
    VM.statusLocalization.value = LocalizationStatus.running
    btn.visibility = View.VISIBLE
    val mapAlpha = VM.prefsCvMap.mapAlpha.toFloat()/100
    wMap.mapView.alpha = mapAlpha


    // TODO: PMX: CHECK IN LOGGER
    if (btnWhereAmI.isVisible) {
      whereAmIWasVisible=true
      utlUi.fadeOut(btnWhereAmI)
    }
    utlUi.disable(act.btnSettings)
    act.uiBottom.hideBottomSheet()
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
   if (imuButtonInited) return
    if (!DBG.uim) return

    this.btnImu=btnImu
    scope.launch(Dispatchers.IO) {
      if (!app.hasDevMode()) {
        utlUi.gone(btnImu)
        return@launch
      }

      LOG.W(TAG, METHOD)
      imuButtonInited=true

      utlUi.visible(btnImu)

      btnImu.setOnClickListener {
        VM.imuEnabled=!VM.imuEnabled

        if (VM.imuEnabled) imuEnable() else imuDisable()

        when {
          !act.initedGmap -> {
            app.snackbarShort(scope, "Cannot start IMU: map not ready yet")
            imuDisable()
          }

          app.locationSmas.value.coord == null -> {
            app.snackbarShort(scope, "IMU needs an initial location.")
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
    VM.imuEnabled=true
    app.snackbarShort(scope, "IMU mode ON (experimental)!")
  }

  fun imuDisable() {
    utlUi.changeBackgroundMaterial(btnImu, R.color.darkGray)
    VM.imuEnabled=false
  }

}