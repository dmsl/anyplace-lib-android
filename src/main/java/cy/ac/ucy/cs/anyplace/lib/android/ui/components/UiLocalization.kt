package cy.ac.ucy.cs.anyplace.lib.android.ui.components

import android.app.Activity
import android.view.View
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.GmapWrapper
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.utlButton
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
// import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.LocalizingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


/**
 * Localization is generally an one-time call. It gets a list of objects from the camera,
 * and calculates the user location.
 *
 * However, YOLO (and it's camera-related components) operate asynchronously in the background,
 * and store detection lists in 'scanning windows'.
 * Therefore we need the below states.
 */
enum class LocalizationStatus {
  running,
  stopped,
  stoppedNoDetections,
}

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
  val btn: MaterialButton by lazy { act.findViewById(button_id) }

  fun setupClick() {
    btn.setOnClickListener {
      VM.statusLocalization.update { LocalizationStatus.running }
      // when (VM.statusLocalization.value) {
        // TODO:PM all other stauses?
      //   Logging.stopped,
      //   Logging.stoppedMustStore -> {  // enter demo-nav mode
      //     // VM.logging.postValue(Logging.demoNavigation)
      //   }
      //   // CHECK:PM ??
      //   // Logging.demoNavigation-> { // exit demo-nav mode:
      //   //   // stopLocalization(mapView)
      //   //   // VM.logging.postValue(Logging.stopped)
      //   // }
      //   else -> { // ignore click
      //     LOG.D(TAG_METHOD, "$METHOD: Ignoring Demo-Navigation. status: ${VM.logging}")
      //   }
      // }
    }
  }

  fun collectStatus() {
    scope.launch{
      VM.statusLocalization.collect { status ->
        LOG.W(TAG_METHOD, "status: $status")
        when(status) {
          LocalizationStatus.running -> {
            // CLR:PM there were CV SPECIFIC:
            // bottom.btnLogging.visibility = View.INVISIBLE
            // VM.circleTimerAnimation = TimerAnimation.reset
            startLocalization()
          }

          LocalizationStatus.stopped -> {
            endLocalization()
            // CLR:PM LOGGER specific
            // VM.logging.postValue(Logging.stopped)
          }
          else ->  {}
        }
      }
    }
  }


  fun endLocalization() {
    LOG.D2(TAG, "$METHOD")
    utlButton.changeBackgroundButtonDONT_USE(btn, ctx, R.color.darkGray)
    btn.isEnabled = true
    wMap.mapView.alpha = 1f
    VM.statusLocalization.tryEmit(LocalizationStatus.stopped)
  }

  fun startLocalization() {
    LOG.D2(TAG, "$METHOD")
    // val btnDemoNav = binding.btnDemoNavigation CLR
    btn.isEnabled=false
    VM.currentTime = System.currentTimeMillis()
    VM.windowStart = VM.currentTime
    VM.statusLocalization.value = LocalizationStatus.running
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