package cy.ac.ucy.cs.anyplace.lib.android.ui.components

import android.app.Activity
import android.view.View
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.GmapWrapper
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.utlButton
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvLoggerViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.LocalizingStatus
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.Logging
import kotlinx.coroutines.CoroutineScope

/**
 * UI Localization Button
 */
class UiLocalization(
        private val act: Activity,
        private val VM: CvLoggerViewModel,
        val scope: CoroutineScope,
        private val wMap: GmapWrapper,
        private val button_id: Int,
        private val uiUiStatusUpdater: UiStatusUpdater,) {

  private val ctx = act.applicationContext
  private val btn: MaterialButton by lazy { act.findViewById(button_id) }

  fun endLocalization() {
    LOG.D2(TAG, "$METHOD")

    uiUiStatusUpdater.clearStatus()
    utlButton.changeBackgroundButtonDONT_USE(btn, ctx, R.color.darkGray)
    btn.isEnabled = true
    wMap.mapView.alpha = 1f
    VM.stateLocalizing.tryEmit(LocalizingStatus.stopped)
  }

  fun startLocalization() {
    LOG.D2(TAG, "$METHOD")
    // val btnDemoNav = binding.btnDemoNavigation CLR
    btn.isEnabled=false
    VM.currentTime = System.currentTimeMillis()
    VM.windowStart = VM.currentTime
    VM.stateLocalizing.value = LocalizingStatus.running
    uiUiStatusUpdater.setStatus("scanning..")
    btn.visibility = View.VISIBLE
    utlButton.changeBackgroundButtonDONT_USE(btn, ctx, R.color.colorPrimary)
    val mapAlpha = VM.prefsCvNav.mapAlpha.toFloat()/100
    wMap.mapView.alpha = mapAlpha
  }


  fun hide() {
    btn.fadeOut()
  }

  fun show() {
    btn.fadeIn()
  }

  fun visibilityGone() {
    btn.visibility = View.GONE
  }

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

  fun setupClick() {
    btn.setOnClickListener {
      when (VM.logging.value) {
        Logging.stopped,
        Logging.stoppedMustStore -> {  // enter demo-nav mode
          VM.logging.postValue(Logging.demoNavigation)
        }
        // CHECK:PM ??
        // Logging.demoNavigation-> { // exit demo-nav mode:
        //   // stopLocalization(mapView)
        //   // VM.logging.postValue(Logging.stopped)
        // }
        else -> { // ignore click
          LOG.D(TAG_METHOD, "$METHOD: Ignoring Demo-Navigation. status: ${VM.logging}")
        }
      }
    }

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
  }

}