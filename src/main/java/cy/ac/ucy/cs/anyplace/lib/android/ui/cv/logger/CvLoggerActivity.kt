package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger

import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.OnMapReadyCallback
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.appSmas
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.CvMapActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.Classifier
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.SmasLoginActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.DetectorViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvLoggerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 *
 */
@AndroidEntryPoint
class CvLoggerActivity: CvMapActivity(), OnMapReadyCallback {
  // PROVIDE TO BASE CLASS [CameraActivity]:
  override val layout_activity: Int get() = R.layout.activity_cv_logger
  override val id_bottomsheet: Int get() = R.id.bottom_sheet_layout
  val id_btn_logging: Int get() = R.id.button_logging

  override val id_gesture_layout: Int get() = R.id.gesture_layout
  override val id_gmap: Int get() = R.id.mapView

  // private lateinit var binding: ActivityCvLoggerBinding
  // private lateinit var VM: CvLoggerViewModel
  // MERGE: all UI elements to abstract CVMapActivity
  // private lateinit var UI: UiActivityCvLogger

  @Suppress("UNCHECKED_CAST")
  override val view_model_class: Class<DetectorViewModel> =
          CvLoggerViewModel::class.java as Class<DetectorViewModel>

  // VIEW MODELS
  /** extends [CvMapViewModel] */
  private lateinit var VM: CvLoggerViewModel

  private val uiLog: CvLoggerUI by lazy {
    CvLoggerUI(this@CvLoggerActivity, lifecycleScope, VM, ui)
  }

  override fun postCreate() {
    super.postCreate()
    VM = _vm as CvLoggerViewModel

    lifecycleScope.launch(Dispatchers.IO) {
      // CHECK: if this crashes (latinit not inited),
      // do something similar with the [readPrefsAndContinue] methods
      // or alternatively it could be put in [setupUi]
      VM.prefsCvLog = dsCvLog.read.first()
      // VM.prefsCvNav = dsCvNav.read.first()
    }

    setupCollectors()
  }

  override fun onResume() {
    super.onResume()
    LOG.D(TAG, "$METHOD [CvLogger]")
    // MERGE
  }

  override fun setupUi() {
    super.setupUi()
    LOG.D2()
    setupUiReactions()

    uiLog.setupUploadBtn()
    uiLog.checkForUploadCache()

    // CLR:PM MERGE this was setupComputerVision()
    // uiLog.uiBottom.setup() // why special method?
    // uiLog.setupBottomSheet() // special method?
  }

  private fun setupUiReactions() {
    lifecycleScope.launch(Dispatchers.Main) {
      uiReactObjectDetection()
      uiLog.bottom.logging.collectStatus()
    }
  }

  /**
   * Setup the BottomSheet by always making it visible.
   * This is because the logging UI is part of the BottomSheet.
   */
  override fun lazyInitBottomSheet() {
    uiLog.bottom = BottomSheetCvLoggerUI(this@CvLoggerActivity,
            VM, ui, uiLog,
            id_bottomsheet,
            id_btn_logging)

    // upcasting the CvLog BottomSheet to the regular BottomSheet
    uiBottom = uiLog.bottom
    setupLoggerBottomSheet()
  }

  // TODO:PM put this method in bottom (CvLoggerBottom)
  // and init it on creation.
  // and remove all code from [uiLog]
  private fun setupLoggerBottomSheet() {
    uiLog.bottom.timer.setup()
    uiLog.setupButtonSettings()
  }


  /**
   * Observes [VM.objectDetectionsAll] changes and updates
   * [binding.bottomUi.buttonCameraTimer] accordingly.
   */
  private fun uiReactObjectDetection() {
    VM.statObjWindowAll.observeForever { detections ->
      // CHECK:PM binding.bottomUi.tvWindowObjectsAll
      uiLog.bottom.tvWindowObjectsAll.text = detections.toString()
    }
  }

  /**
   * CHECK:PM
   */
  var collectorsSet=false
  private fun setupCollectors() {
    if (collectorsSet) return
    LOG.D(TAG_METHOD)
    observeFloors()
    collectLoggedInChatUser()
    VM.nwCvFingerprintSend.collect()
    collectorsSet=true
  }

  /*
  * Reacts to updates on [ChatUser]'s login status:
  * Only authenticated users are allowed to use this activity
  */
  private fun collectLoggedInChatUser() {
    // only logged in users are allowed on this activity:
    lifecycleScope.launch(Dispatchers.IO) {
      appSmas.dsChatUser.readUser.collect { user ->
        if (user.sessionkey.isBlank()) {
          finish()
          startActivity(Intent(this@CvLoggerActivity, SmasLoginActivity::class.java))
        } else {
          lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(applicationContext, "Welcome ${user.uid}!", Toast.LENGTH_LONG).show()
          }
        }
      }
    }
  }

  override fun onMapReadyCallback() {
    uiLog.bottom.logging.setupClick()
    uiLog.setupOnMapLongClick()
  }

  /* Runs when the first of any of the floors is loaded */
  // override fun onFirstFloorLoaded() { super.onFirstFloorLoaded() }

  /* Runs when any of the floors is loaded */
  // override fun onFloorLoaded() { super.onFloorLoaded()  }

  override fun onInferenceRan(detections: MutableList<Classifier.Recognition>) {
    LOG.D2(TAG, "$METHOD: CvLoggerActivity")
    uiLog.onInferenceRan()

    if (detections.isNotEmpty()) {
      LOG.D2(TAG, "$METHOD: detections: ${detections.size} (LOGGER OVERRIDE)")
    }
    VM.processDetections(detections)
  }

}