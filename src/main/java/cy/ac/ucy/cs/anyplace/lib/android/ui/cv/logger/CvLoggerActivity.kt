package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.appSmas
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.ACT_NAME_LOGGER
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.CvMapActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.Classifier
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.SmasLoginActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.DetectorViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvLoggerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 *
 */
@AndroidEntryPoint
class CvLoggerActivity: CvMapActivity(), OnMapReadyCallback {
  private var TG = "act-cv-smas"

  //// PROVIDE TO BASE CLASS ([CvMapActivity]), which will provide to its base class
  override val layout_activity: Int get() = R.layout.activity_cv_logger
  override val id_bottomsheet: Int get() = R.id.bottom_sheet_layout
  override val id_gesture_layout: Int get() = R.id.gesture_layout
  //// PROVIDE TO IMMEDIATE PARENT CLASS [CvMapActivity]:
  override val id_gmap: Int get() = R.id.mapView
  override val actName = ACT_NAME_LOGGER
  override val id_btn_settings: Int get() = R.id.button_settings
  ////// FLOOR SELECTOR
  override val id_group_levelSelector : Int get() = R.id.group_levelSelector
  override val id_tvTitleFloor: Int get() = R.id.textView_titleLevel
  override val id_btnSelectedFloor: Int get() = R.id.button_selectedLevel
  override val id_btnFloorUp: Int get() = R.id.button_levelUp
  override val id_btnFloorDown: Int get() = R.id.button_levelDown
  ////// UI-LOCALIZATION
  override val id_btn_localization: Int get() = R.id.btn_localization
  override val id_btn_whereami: Int get() = R.id.btn_whereami


  val id_btn_logging: Int get() = R.id.button_logging

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    app.dsCvMap.setMainActivity(CONST.START_ACT_LOGGER)
  }

  @Suppress("UNCHECKED_CAST")
  override val view_model_class: Class<DetectorViewModel> =
          CvLoggerViewModel::class.java as Class<DetectorViewModel>

  // VIEW MODELS
  /** extends [CvMapViewModel] */
  private lateinit var VM: CvLoggerViewModel

  override fun postResume() {
    super.postResume()
    VM = _vm as CvLoggerViewModel
    app.setMainView(findViewById(R.id.layout_root), false)

    lifecycleScope.launch(Dispatchers.IO) {
      // CHECK: if this crashes (latinit not inited),
      // do something similar with the [readPrefsAndContinue] methods
      // or alternatively it could be put in [setupUi]
      // VM.prefsCvMap = dsCvMap.read.first()
    }

    setupCollectors()
  }

  override fun onResume() { super.onResume() }


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
   * Setup UI that requires GMap initialization
   *
   * Sets up:
   * - uiLog for the logger
   * - BottomSheet by always making it visible.
   *   This is because the logging UI is part of the BottomSheet.
   */
  override fun setupUiAfterGmap() {
    val MT = ::setupUiAfterGmap.name

    LOG.D(TG, "$MT: init logging click")

    VM.uiLog = CvLoggerUI(this@CvLoggerActivity, lifecycleScope, VM, VM.ui)
    // bsheet is always visible as we show the tutorial
    VM.uiLog.bottom = BottomSheetCvLoggerUI(this@CvLoggerActivity,
            VM, id_bottomsheet, id_btn_logging, true)

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
   */
  var collectorsSet=false
  private fun setupCollectors() {
    val MT = ::setupCollectors.name
    if (collectorsSet) return

    LOG.D(TG, MT)
    observeLevels()
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
      appSmas.dsUserSmas.read.collect { user ->
        if (user.sessionkey.isBlank()) {
          finish()
          startActivity(Intent(this@CvLoggerActivity, SmasLoginActivity::class.java))
        } else {
          // lifecycleScope.launch(Dispatchers.Main) {
          //   Toast.makeText(applicationContext, "Welcome ${user.uid}!", Toast.LENGTH_LONG).show()
          // }
        }
      }
    }
  }

  override fun onMapReady(googleMap: GoogleMap) {
    super.onMapReady(googleMap)

    val MT = ::onMapReady.name
    LOG.E(TG, "$MT: [callback]")
    VM.uiLog.setupOnMapLongClick()
  }


  /* Runs when the first of any of the floors is loaded */
  // override fun onFirstFloorLoaded() { super.onFirstFloorLoaded() }

  /* Runs when any of the floors is loaded */
  // override fun onFloorLoaded() { super.onFloorLoaded()  }

  override fun onInferenceRan(detections: MutableList<Classifier.Recognition>) {
    val MT = ::onInferenceRan.name
    LOG.V2(TG, "$MT: CvLoggerActivity")
    VM.uiLog.onInferenceRan()

    if (detections.isNotEmpty()) {
      LOG.V2(TG, "$MT: detections: ${detections.size} (LOGGER OVERRIDE)")
    }
    VM.processDetections(detections, this@CvLoggerActivity)
  }

}