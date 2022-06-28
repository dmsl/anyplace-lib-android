package cy.ac.ucy.cs.anyplace.lib.android.ui.components

import android.app.Activity
import android.view.View
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.GmapWrapper
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.UtilUI
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.LocalizationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


/**
 * UI Localization Button
 */
class UiLocalization(
        private val act: Activity,
        private val VM: CvViewModel,
        val scope: CoroutineScope,
        private val wMap: GmapWrapper,
        private val button_id: Int) {

  private val ctx = act.applicationContext

  private val utlButton by lazy { UtilUI(act, scope) }

  val btn: MaterialButton by lazy { act.findViewById(button_id) }

  fun setupClick() {
    btn.setOnClickListener {
      VM.statusLocalization.update { LocalizationStatus.running }
    }
  }

  fun collectStatus() {
    scope.launch{
      VM.statusLocalization.collect { status ->
        LOG.W(TAG_METHOD, "status: $status")
        when(status) {
          LocalizationStatus.running -> {  startLocalization()  }
          LocalizationStatus.stopped -> {  endLocalization() }
          else ->  {}
        }
      }
    }
  }


  fun endLocalization() {
    VM.disableCvDetection()
    LOG.D2(TAG, "$METHOD")
    // utlButton.changeBackgroundDONT_USE(btn, R.color.darkGray)
    btn.isEnabled = true
    wMap.mapView.alpha = 1f
    VM.statusLocalization.tryEmit(LocalizationStatus.stopped)
  }

  fun startLocalization() {
    VM.enableCvDetection()
    LOG.D2(TAG, "$METHOD")
    btn.isEnabled=false
    VM.currentTime = System.currentTimeMillis()
    VM.windowStart = VM.currentTime
    VM.statusLocalization.value = LocalizationStatus.running
    btn.visibility = View.VISIBLE
    val mapAlpha = VM.prefsCvNav.mapAlpha.toFloat()/100
    wMap.mapView.alpha = mapAlpha
  }


  fun hide() = utlButton.fadeOut(btn)

  fun show() = utlButton.fadeIn(btn)

  fun visibilityGone() = utlButton.gone(btn)

  // fun disable() {
  //   btn.isEnabled = false
  // }

  // fun enable() {
  //   btn.isEnabled = true
  // }
  //
  // fun visible() {
  //   btn.visibility = View.VISIBLE
  // }

    // CLR:PM not needed functionality? on long click: remove Json CV-Maps
    // private var longClickClearCvMap=false
    // btnDemoNav.setOnLongClickListener {
    //   if (!longClickClearCvMap) {
    //     scope.launch {
    //       statusUpdater.showWarningAutohide("Delete CvMap?", "long-click again", 2000L)
    //     }
    //     longClickClearCvMap = true
    //   } else {
    //     scope.launch {
    //       statusUpdater.showInfoAutohide("Deleted CvMap", 2000L)
    //     }
    //     VM.cvMapH?.clearCache()
    //   }
    //
    //   true
    // }
    // }

}