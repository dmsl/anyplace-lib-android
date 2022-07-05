package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger

import android.content.Intent
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.appSmas
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.CvMapActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.Classifier
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.SmasLoginActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.DetectorViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvLoggerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

  override fun postResume() {
    super.postResume()
    VM = _vm as CvLoggerViewModel

    lifecycleScope.launch(Dispatchers.IO) {
      // CHECK: if this crashes (latinit not inited),
      // do something similar with the [readPrefsAndContinue] methods
      // or alternatively it could be put in [setupUi]
      // VM.prefsCvNav = dsCvNav.read.first()
    }

    setupCollectors()
  }

  override fun onResume() {
    super.onResume()
    LOG.D(TAG, "$METHOD [CvLogger]")
    if (DBG.BN11c) updateModelName() // TODO:PMX: BN11c
  }


  /**
   * TODO: for this (and any similar code that loops+delay):
   * - create a variable and observe it (a Flow or something observable/collactable)
   */
  fun updateModelName() {
    val tvTitle = findViewById<TextView>(R.id.tvTitle)
    lifecycleScope.launch(Dispatchers.IO)
    {
      while (!VM.modelEnumLoaded) delay(100)
      utlUi.text(tvTitle, "logger (model: ${VM.model.modelName})")
    }
  }

  /**
   * nothing here as most setup is done at [setupUiAfterGmap]
   */
  override fun setupUi() {
    super.setupUi()
    LOG.D2()
  }

  private fun setupUiReactions() {
    lifecycleScope.launch(Dispatchers.Main) {
      uiReactObjectDetection()
      VM.uiLog.bottom.logging.collectStatus()
    }
  }

  /**
   * Setup UI that requires Gmap initialization
   *
   * Sets up:
   * - uiLog for the logger
   * - BottomSheet by always making it visible.
   *   This is because the logging UI is part of the BottomSheet.
   */
  override fun setupUiAfterGmap() {
    LOG.D(TAG, "$METHOD: init logging click")

    VM.uiLog = CvLoggerUI(this@CvLoggerActivity, lifecycleScope, VM, VM.ui)
    VM.uiLog.bottom = BottomSheetCvLoggerUI(this@CvLoggerActivity,
            VM, id_bottomsheet, id_btn_logging)

    // upcasting the CvLog BottomSheet to the regular BottomSheet
    uiBottom = VM.uiLog.bottom
    setupLoggerBottomSheet()
    VM.uiLog.uiBottomLazilyInited=true

    VM.uiLog.bottom.logging.setupClick()
    VM.uiLog.setupUploadBtn()
    VM.uiLog.checkForUploadCache()

    // setup reactions:
    setupUiReactions()
  }

  // TODO:PM put this method in bottom (CvLoggerBottom)
  // and init it on creation.
  // and remove all code from [uiLog]
  private fun setupLoggerBottomSheet() {
    VM.uiLog.bottom.timer.setup()
    VM.uiLog.setupButtonSettings()
  }


  /**
   * Observes [VM.objectDetectionsAll] changes and updates
   * [binding.bottomUi.buttonCameraTimer] accordingly.
   */
  private fun uiReactObjectDetection() {
    VM.statObjWindowAll.observeForever { detections ->
      // CHECK:PM binding.bottomUi.tvWindowObjectsAll
      VM.uiLog.bottom.tvWindowObjectsAll.text = detections.toString()
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

  override fun onMapReady(googleMap: GoogleMap) {
    super.onMapReady(googleMap)
    LOG.E(TAG, "onMapReadyCallback: [CvLogger]")
    VM.uiLog.setupOnMapLongClick()
  }


  /* Runs when the first of any of the floors is loaded */
  // override fun onFirstFloorLoaded() { super.onFirstFloorLoaded() }

  /* Runs when any of the floors is loaded */
  // override fun onFloorLoaded() { super.onFloorLoaded()  }

  override fun onInferenceRan(detections: MutableList<Classifier.Recognition>) {
    LOG.V2(TAG, "$METHOD: CvLoggerActivity")
    VM.uiLog.onInferenceRan()

    if (detections.isNotEmpty()) {
      LOG.V2(TAG, "$METHOD: detections: ${detections.size} (LOGGER OVERRIDE)")
    }
    VM.processDetections(detections, this@CvLoggerActivity)
  }

}