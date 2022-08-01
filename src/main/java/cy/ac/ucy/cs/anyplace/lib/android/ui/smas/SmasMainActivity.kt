package cy.ac.ucy.cs.anyplace.lib.android.ui.smas

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.ExperimentalMaterialApi
import androidx.constraintlayout.widget.Group
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.BuildConfig
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.appSmas
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.ACT_NAME_SMAS
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.CvMapActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.BottomSheetCvUI
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.Classifier
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilNotify
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.DetectorViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationResult
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.UserCoordinates
import cy.ac.ucy.cs.anyplace.lib.android.ui.settings.MainSettingsDialog
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.chat.SmasChatActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
import cy.ac.ucy.cs.anyplace.lib.android.utils.toLatLng
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasChatViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasMainViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.nw.LocationSendNW
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Coord
import cy.ac.ucy.cs.anyplace.lib.smas.models.CONSTchatMsg.MTYPE_ALERT
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

/**
 * This activity takes care of pulling user locations and messages.
 * When the [SmasChatActivity] runs, this activity is still in the background,
 * providing this functionality:
 *
 *
 *  + How locations are pulled:
 *    - [updateLocationsLOOP] every [locationRefreshMs] ms (settting) it:
 *      - sends own user location (only if it has one)
 *        - it sends the last localization position of the user
 *        - so the user must have localized at least once
 *        - it will keep sending that one until it gets updated
 *      - it also receives other user's location
 *
 *  - How messages are pulled:
 *    - [collectLocations] of the [SmasMainViewModel] reacts when new locations are pulled
 *      - the ones we pull using [updateLocationsLOOP]
 *      - then (in [LocationGetNW]), it processes those locations ([processUserLocations])
 *      - the response also includes a timestamp of the last msg of the user
 *      - so [checkForNewMsgs] checks it, and if there are newer msgs it pulls them
 *
 */
@AndroidEntryPoint
class SmasMainActivity : CvMapActivity(), OnMapReadyCallback {

  //// PROVIDE TO BASE CLASS ([CvMapActivity]), which will provide to its base class
  override val layout_activity: Int get() = R.layout.activity_smas
  override val id_bottomsheet: Int get() = R.id.bottom_sheet_cvmap
  override val id_gesture_layout: Int get() = R.id.gesture_layout
  //// PROVIDE TO IMMEDIATE PARENT CLASS [CvMapActivity]:
  override val id_gmap: Int get() = R.id.mapView
  override val actName = ACT_NAME_SMAS
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

  @Suppress("UNCHECKED_CAST")
  override val view_model_class: Class<DetectorViewModel> =
          SmasMainViewModel::class.java as Class<DetectorViewModel>


  // VIEW MODELS
  /** extends [CvViewModel] */
  private lateinit var VM: SmasMainViewModel

  /** Async handling of SMAS Messages and Alerts */
  private lateinit var VMchat: SmasChatViewModel

  // UI COMPONENTS
  private lateinit var btnChat: Button
  private lateinit var btnFlir: Button
  private lateinit var btnAlert: MaterialButton

  private val btnImu by lazy { findViewById<MaterialButton>(R.id.btn_imu) }
  private val utlNotify by lazy { UtilNotify(applicationContext) }

  /** whether this activity is active or not */
  private var isActive = false

  private var tag = "SmasACT"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    LOG.D2(TAG, "$tag: $METHOD")
    app.dsCvMap.setMainActivity(CONST.START_ACT_SMAS)
  }

  /**
   * Called by [CvMapActivity]
   */
  override fun setupUi() {
    super.setupUi()
    LOG.D2()

    setupButtonSettings()
    // setupButtonSwitch()
    setupButtonChat()
    setupButtonFlir()
    setupButtonAlert()

    VM.ui.localization.setupButtonImu(btnImu)
  }


  override fun onMapReady(googleMap: GoogleMap) {
    super.onMapReady(googleMap)

    // LMN1
    // val googleMap : GoogleMap = null!!; googleMap.projection
    setupMapLongClick()
  }

  var handlingGmapLongClick= false
  private fun setupMapLongClick() {
    if (handlingGmapLongClick) return
    handlingGmapLongClick=true

    // BUG:F34LC: for some reason some normal clicks are registered as long-clicks
    VM.ui.map.obj.setOnMapLongClickListener {
      LOG.W(TAG, "$tag: long click")
      forceUserLocation(it)
    }
  }

  private fun forceUserLocation(forcedLocation: LatLng) {
    LOG.W(TAG, "forcing location: $forcedLocation")
    app.snackbarShort(lifecycleScope, "Location set manually (long-clicked)")

    val floorNum = app.wLevel!!.levelNumber()
    val loc = forcedLocation.toCoord(floorNum)
    app.locationSmas.update { LocalizationResult.Success(loc, LocalizationResult.MANUAL) }
  }

  /**
   * Called by [CvMapActivity] ? (some parent method)
   */
  override fun postResume() {
    super.postResume()
    LOG.D2(TAG, "$tag: $METHOD")
    VM = _vm as SmasMainViewModel
    app.setMainView(findViewById(R.id.layout_root), false)

    VMchat = ViewModelProvider(this)[SmasChatViewModel::class.java]
    appSmas.setMainActivityVMs(VM, VMchat)

    VM.readBackendVersion()
    setupCollectors()
  }

  /**
   * Runs only once, when any of the floors is loaded for the first time.
   */
  override fun onFirstLevelLoaded() {
    LOG.I(TAG, "$tag: $METHOD: Floor: ${app.level.value?.number}")

    super.onFirstLevelLoaded()

    updateLocationsLOOP()  // send own location & receive other users locations
    lifecycleScope.launch(Dispatchers.IO) {
      if (DBG.BG5) VM.waitForUi()
      VM.collectLocations(VMchat, VM.ui.map)
    }
    collectAlertingUser()
    VM.collectUserOutOfBounds()
  }


  /* Runs when any of the floors is loaded */
  override fun onLevelLoaded() {
    super.onLevelLoaded()

    lifecycleScope.launch(Dispatchers.IO) {
      // workaround: wait for UI to be ready (not the best one)
      if (DBG.BG5) VM.waitForUi()

      VM.ui.map.markers.clearChatLocationMarker()
    }
  }



  /**
   * In a loop:
   * - conditionally send own location
   * - get other users locations
   */
  var updatingLocationsLoop = false
  private fun updateLocationsLOOP()  {
    if (updatingLocationsLoop) return
    updatingLocationsLoop=true

    lifecycleScope.launch(Dispatchers.IO) {

      while (true) {
        var msg = "pull"
        if (isActive && app.hasLastLocation()) {
          val coords = app.locationSmas.value.coord!!
          val userCoords = UserCoordinates(app.wSpace.obj.buid,
                  coords.level, coords.lat, coords.lon)
          VM.nwLocationSend.safeCall(userCoords)
          msg+="&send"
        }

        msg="($msg) "
        if (!isActive) msg+=" [inactive]"
        if (!app.hasLastLocation()) msg+=" [no-location-yet]"

        if (!app.hasInternet()) {
          msg+="[SKIP: NO-INTERNET]"
        } else {
          VM.nwLocationGet.safeCall()
        }

        LOG.V2(TAG, "loop-location: main: $msg")

        delay(VM.prefsCvMap.locationRefreshMs.toLong())
      }
    }
  }

  ////////////////////////////////////////////////

  override fun onResume() {
    super.onResume()
    LOG.W(TAG, "$tag: $METHOD")
    isActive = true
    if (DBG.uim) VMsensor.registerListeners()
  }

  override fun setupUiAfterGmap() {
    // bsheet will be hidden in SMAS
    uiBottom = BottomSheetCvUI(this@SmasMainActivity, false)
  }

  override fun onPause() {
    super.onPause()
    LOG.W(TAG, "$tag: $METHOD")
    isActive = false

    if (DBG.uim) VMsensor.unregisterListener()
  }

  /**
   * Async Collection of remotely fetched data
   */
  private fun setupCollectors() {
    LOG.D()

    collectLoggedInUser()
    observeLevels()
  }

  /**
   * Update the UI button when new msgs come in
   */
  private fun reactToNewMessages() {
    lifecycleScope.launch(Dispatchers.Main) {
      VM.readHasNewMessages.observeForever { hasNewMsgs ->
        val btn = btnChat as MaterialButton
        LOG.W(TAG,"NEW-MSGS: $hasNewMsgs")

        if (hasNewMsgs) {
          utlUi.changeBackgroundMaterial(btn, R.color.redDark)
          utlUi.changeMaterialIcon(btn, R.drawable.ic_chat_unread)
          utlNotify.msgReceived()
        } else {
          utlUi.changeBackgroundMaterial(btn, R.color.colorPrimary)
          utlUi.changeMaterialIcon(btn, R.drawable.ic_chat)
        }
      }
    }
  }

  /**
   * Reacts to updates on [ChatUser]'s login status:
   * Only authenticated users are allowed to use this activity
   */
  var collectingUser = false
  private fun collectLoggedInUser() {
    if (collectingUser) return
    collectingUser = true

    // only logged in users are allowed on this activity:
    lifecycleScope.launch(Dispatchers.IO) {

      val user= appSmas.dsUserSmas.read.first()
      if (user.sessionkey.isBlank()) {
        finish()
        startActivity(Intent(this@SmasMainActivity, SmasLoginActivity::class.java))
      } else {
        // lifecycleScope.launch(Dispatchers.Main) {
        //   Toast.makeText(applicationContext, "Welcome ${user.uid}!", Toast.LENGTH_LONG).show()
        // }
      }
    }
  }

  /**
   * React when a user is in alert mode
   */
  var notifiedForAlert = false
  var collectingAlertingUser = false
  @SuppressLint("SetTextI18n")
  private fun collectAlertingUser() {
    if (collectingAlertingUser) return
    collectingAlertingUser=true

    val group: Group = findViewById(R.id.group_userAlert)
    val btnAlertOngoing: MaterialButton = findViewById(R.id.btnAlertOngoing)

    lifecycleScope.launch(Dispatchers.IO) {
      VM.alertingUser.collect {
        if (it == null) { // no user alerting
          utlUi.fadeOut(group)
          delay(100)
          utlUi.fadeIn(btnAlert)
          utlUi.clearAnimation(btnAlertOngoing)
          notifiedForAlert=false
        } else { // user alerting
          utlUi.text(btnAlertOngoing, "${it.name} ${it.surname}")
          utlUi.fadeOut(btnAlert)
          delay(100)
          utlUi.fadeIn(group)
          delay(100)
          utlUi.flashingLoop(btnAlertOngoing)
          if (!notifiedForAlert) {  // only when the alert is initially raised
            notifiedForAlert=true
            utlNotify.alertReceived()
          }
        }
      }
    }

    btnAlertOngoing.setOnClickListener {
      val alertingUser = VM.alertingUser.value ?: return@setOnClickListener

      val alertingUserCoords = Coord(alertingUser.x,
              alertingUser.y,
              alertingUser.deck)

      val curFloor = app.wLevel?.levelNumber()
      if (alertingUser.deck != curFloor) {
        app.wLevels.moveToFloor(VM, alertingUser.deck)
      }

      VM.ui.map.moveToLocation(alertingUserCoords.toLatLng())
    }
  }

  private fun setupButtonAlert() {
    btnAlert = findViewById(R.id.btnAlert)
    btnAlert.setOnClickListener {
      app.snackbarShort(lifecycleScope, "Long-press to toggle alert")
    }

    btnAlert.setOnLongClickListener {
      if (app.locationSmas.value is LocalizationResult.Unset) {
        app.snackbarLong(lifecycleScope, "Please find location first,\nor set it manually (map long-press)")
        return@setOnLongClickListener true
      }

      when (VM.toggleAlert()) {
        LocationSendNW.Mode.alert -> { issueAlert() }
        LocationSendNW.Mode.normal -> { cancelAlert() }
      }
      true
    }
  }

  /**
   * Runs once, when the alert is issued
   *
   * Sends also a location-based alert message on chat
   */
  fun issueAlert() {
    app.alerting=true

    val lr = app.locationSmas.value
    val usedMethod = LocalizationResult.getUsedMethod(lr)
    VM.ui.map.markers.setOwnLocationMarker(lr.coord!!, usedMethod, app.alerting)
    utlUi.flashingLoop(btnAlert)
    utlUi.text(btnAlert, "ALERTING")
    utlUi.setTextSizeSp(btnAlert, 52f)
    utlUi.changeBackgroundMaterial(btnAlert, R.color.redDark)

    VMchat.sendMessage("", MTYPE_ALERT)  // submit also an alert msg
  }

  fun cancelAlert() {
    app.alerting=false

    val lr = app.locationSmas.value
    val usedMethod = LocalizationResult.getUsedMethod(lr)
    VM.ui.map.markers.setOwnLocationMarker(lr.coord!!, usedMethod, app.alerting)
    utlUi.clearAnimation(btnAlert)
    utlUi.text(btnAlert, "SEND ALERT")
    utlUi.setTextSizeSp(btnAlert, 42f)
    utlUi.changeBackgroundMaterial(btnAlert, R.color.gray)
  }

  private fun setupButtonSettings() {
    btnSettings.setOnClickListener {
      val versionStr = BuildConfig.VERSION_CODE
      MainSettingsDialog.SHOW(supportFragmentManager,
              MainSettingsDialog.FROM_MAIN, this@SmasMainActivity, versionStr)
    }
  }



  private fun setupButtonFlir() {
    btnFlir = findViewById(R.id.btnFlir)
    val FLIR_PKG = "com.flir.myflir.s62"

    btnFlir.setOnClickListener {
      LOG.E(TAG_METHOD, "on click")
      lifecycleScope.launch {
        var intent = packageManager.getLaunchIntentForPackage(FLIR_PKG)
        if (intent == null) {
          intent = try {
            LOG.E(TAG_METHOD, "intent is null")
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$FLIR_PKG"))
          } catch (e: Exception) {
            LOG.E(TAG_METHOD, "" + e.message)
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$FLIR_PKG"))
          }
        }
        LOG.E(TAG_METHOD, "launching activity")
        startActivity(intent)
      }
    }
  }

  /**
   * starts an activity and listens for a result
   */
  private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
  { result: ActivityResult ->
    if (!DBG.GREs) return@registerForActivityResult
    if (result.resultCode == Activity.RESULT_OK) {
      val intent: Intent? = result.data
      if (intent != null) {
        val lat = intent.getDoubleExtra("lat", 0.0)
        val lon = intent.getDoubleExtra("lon", 0.0)
        val level = intent.getIntExtra("level", Int.MIN_VALUE)

        if (level != Int.MIN_VALUE) {
          LOG.E(TAG, "VALID LOC from chat: $lat $lon $level")

          // clear any previous markers
          VM.ui.map.markers.clearAllInfoWindow()

          lifecycleScope.launch(Dispatchers.IO) {
            val curFloor = app.wLevel?.levelNumber()
            if (level != curFloor) {
              app.wLevels.moveToFloor(VM, level)
              LOG.E(TAG," will clear all info (from actForResult)")

              // add some delay, so any markers can be rendered
              // before rendering the chat shared-location marker
              delay(250)
            }
            VM.ui.map.markers.addSharedLocationMarker(Coord(lat, lon, level))
          }
        }
      }
    }
  }

  @OptIn(ExperimentalMaterialApi::class, ExperimentalPermissionsApi::class) // compose
  private fun setupButtonChat() {
    LOG.D()
    btnChat = findViewById(R.id.btnChat)

    collectMessages()
    reactToNewMessages()

    btnChat.setOnClickListener {
      lifecycleScope.launch {
        if (!app.hasLastLocation()) {
          val msg = "To open chat, you must localize\n" +
                    "or set location manually with a long-press."
          app.snackbarInf(lifecycleScope, msg)
          utlUi.attentionZoom(VM.ui.localization.btn)
          return@launch
        }

        LOG.W(TAG, "Clearing markers before opening chat..")
        VM.ui.map.markers.clearChatLocationMarker()
        startForResult.launch(Intent(applicationContext, SmasChatActivity::class.java))
      }
    }
  }


  var collectorMsgsEnabled = false  // BUGFIX: setting up multiple collectors
  /**
   * React to flow that is populated by [nwMsgGet] safeCall
   */
  private fun collectMessages() {
    if (!collectorMsgsEnabled) {
      collectorMsgsEnabled = true
      lifecycleScope.launch(Dispatchers.IO) {
        VMchat.nwMsgGet.collect(app)
      }
    }
  }


  // TODO:PMX FR ?
  override fun onInferenceRan(detections: MutableList<Classifier.Recognition>) {
    LOG.D3(TAG, "$METHOD: SmasMainActivity")
    VM.ui.onInferenceRan()
    VM.processDetections(detections, this@SmasMainActivity)
  }

}