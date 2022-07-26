package cy.ac.ucy.cs.anyplace.lib.android.ui.smas

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
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
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.CvMapActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.Classifier
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilNotify
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.OutlineTextView
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
 */
@AndroidEntryPoint
class SmasMainActivity : CvMapActivity(), OnMapReadyCallback {

  // PROVIDE TO BASE CLASS [CvMapActivity]:
  override val layout_activity: Int get() = R.layout.activity_smas
  override val id_bottomsheet: Int get() = R.id.bottom_sheet_cvmap
  override val id_gesture_layout: Int get() = R.id.gesture_layout
  override val id_gmap: Int get() = R.id.mapView

  @Suppress("UNCHECKED_CAST")
  override val view_model_class: Class<DetectorViewModel> =
          SmasMainViewModel::class.java as Class<DetectorViewModel>

  override val actName = "SMAS"

  // VIEW MODELS
  /** extends [CvViewModel] */
  private lateinit var VM: SmasMainViewModel

  /** Async handling of SMAS Messages and Alerts */
  private lateinit var VMchat: SmasChatViewModel

  // UI COMPONENTS
  private lateinit var btnChat: Button
  private lateinit var btnFlir: Button
  private lateinit var btnSettings: Button
  private lateinit var btnAlert: MaterialButton

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

    setupClickm()
  }



  override fun onMapReady(googleMap: GoogleMap) {
    super.onMapReady(googleMap)

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

    app.showToast(lifecycleScope, "Location set manually (long-click)")

    val floorNum = app.wFloor!!.floorNumber()
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
    VMchat = ViewModelProvider(this)[SmasChatViewModel::class.java]
    appSmas.setMainActivityVMs(VM, VMchat)

    readBackendVersion()
    setupCollectors()
  }

  /**
   * Runs only once, when any of the floors is loaded for the first time.
   */
  override fun onFirstFloorLoaded() {
    LOG.E(TAG, "$tag: $METHOD: Floor: ${app.floor.value}")

    super.onFirstFloorLoaded()

    updateLocationsLOOP()  // send own location & receive other users locations
    lifecycleScope.launch(Dispatchers.IO) {
      while (!VM.uiLoaded()) delay(100) // workaround until ui component is loaded (not the best one..)
      VM.collectLocations(VMchat, VM.ui.map)
    }
    collectAlertingUser()
  }


  /* Runs when any of the floors is loaded */
  override fun onFloorLoaded() {
    super.onFloorLoaded()

    VM.ui.map.markers.clearChatLocationMarker()
  }

  /**
   * In a loop:
   * - conditionally send own location
   * - get other users locations
   *
   *  - TODO:PM get from anyplace location
   *  - TODO:PM get a list of those locations: how? parse json?
   */
  var updatingLocationsLoop = false
  private fun updateLocationsLOOP()  {
    if (updatingLocationsLoop) return
    updatingLocationsLoop=true

    lifecycleScope.launch(Dispatchers.IO) {

      while (true) {
        var msg = "pull"
        val hasRegisteredLocation = app.locationSmas.value.coord != null
        if (isActive && hasRegisteredLocation) {
          val lastCoordinates = UserCoordinates(app.wSpace.obj.id,
                  app.wFloor?.obj!!.floorNumber.toInt(),
                  app.locationSmas.value.coord!!.lat,
                  app.locationSmas.value.coord!!.lon)

          VM.nwLocationSend.safeCall(lastCoordinates)
          msg+="&send"
        }

        msg="($msg) "
        if (!isActive) msg+=" [inactive]"
        if (!hasRegisteredLocation) msg+=" [no-user-location-registered]"

        LOG.V2(TAG, "loop-location: main: $msg")

        VM.nwLocationGet.safeCall()
        delay(VM.prefsCvMap.locationRefreshMs.toLong())
      }
    }
  }

  ////////////////////////////////////////////////

  override fun onResume() {
    super.onResume()
    LOG.W(TAG, "$tag: $METHOD")
    isActive = true
    if (DBG.uim) VMs.registerListeners()
  }

  override fun onPause() {
    super.onPause()
    LOG.W(TAG, "$tag: $METHOD")
    isActive = false

    if (DBG.uim) VMs.unregisterListener()
  }

  /**
   * Async Collection of remotely fetched data
   */
  private fun setupCollectors() {
    LOG.D()

    collectLoggedInUser()
    observeFloors()
  }

  /**
   * Update the UI button when new msgs come in
   */
  private fun reactToNewMessages() {
    lifecycleScope.launch(Dispatchers.Main) {
      VM.readHasNewMessages.observeForever { hasNewMsgs ->
        val btn = btnChat as MaterialButton
        val ctx = this@SmasMainActivity
        LOG.W(TAG,"NEW-MSGS: $hasNewMsgs")

        if (hasNewMsgs) {
          utlUi.changeBackgroundMaterial(btn, R.color.redDark)
          utlUi.changeMaterialIcon(btn, R.drawable.ic_chat_unread)
          utlNotify.msgReceived()
        } else {
          utlUi.changeBackgroundMaterial(btn, R.color.colorPrimaryDark)
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

      val user= appSmas.dsSmasUser.read.first()
      if (user.sessionkey.isBlank()) {
        finish()
        startActivity(Intent(this@SmasMainActivity, SmasLoginActivity::class.java))
      } else {
        lifecycleScope.launch(Dispatchers.Main) {
          Toast.makeText(applicationContext, "Welcome ${user.uid}!", Toast.LENGTH_LONG).show()
        }
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
          btnAlertOngoing.clearAnimation()
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

      val curFloor = app.wFloor?.floorNumber()
      if (alertingUser.deck != curFloor) {
        app.wFloors.moveToFloor(VM, alertingUser.deck)
      }

      VM.ui.map.markers.animateToLocation(alertingUserCoords.toLatLng())
    }
  }

  private fun setupButtonAlert() {
    btnAlert = findViewById(R.id.btnAlert)
    btnAlert.setOnClickListener {
      Toast.makeText(applicationContext, "long-press (to toggle alert)", Toast.LENGTH_SHORT).show()
    }

    btnAlert.setOnLongClickListener {
      if (app.locationSmas.value is LocalizationResult.Unset) {
        app.showToast(lifecycleScope, "Localize first, or set location manually (long-click)")
        return@setOnLongClickListener true
      }

      when (VM.toggleAlert()) {
        LocationSendNW.Mode.alert -> { issueAlert() }
        LocationSendNW.Mode.normal -> { cancelAlert() }
      }
      true
    }
  }

  fun issueAlert() {
    utlUi.flashingLoop(btnAlert)
    utlUi.text(btnAlert, "ALERTING")
    utlUi.setTextSizeSp(btnAlert, 52f)
    utlUi.changeBackgroundMaterial(btnAlert, R.color.redDark)

    VMchat.sendMessage("", MTYPE_ALERT)  // submit also an alert msg
  }

  fun cancelAlert() {
    utlUi.clearAnimation(btnAlert)
    utlUi.text(btnAlert, "SEND ALERT")
    utlUi.setTextSizeSp(btnAlert, 42f)
    utlUi.changeBackgroundMaterial(btnAlert, R.color.gray)
  }

  private fun setupButtonSettings() {
    btnSettings = findViewById(R.id.button_settings)
    btnSettings.setOnClickListener {
      val versionStr = BuildConfig.VERSION_CODE
      MainSettingsDialog.SHOW(supportFragmentManager,
              MainSettingsDialog.FROM_MAIN, this@SmasMainActivity, versionStr)
    }
  }

  var setupClickm = false
  private fun setupClickm() {
    if (setupClickm) return
    if (!DBG.uim) return
    setupClickm=true
    // TODO: PM: put in localization button component?
    // - hide along with localization
    // - put also in XML: activity_cv_logger
    LOG.E(TAG, "$METHOD")

    val btn = findViewById<MaterialButton>(R.id.btn_imu)
    btn.visibility= View.VISIBLE

    btn.setOnClickListener {
      VM.miEnabled=!VM.miEnabled

      if (VM.miEnabled) mToggleOn(btn) else mToggleOff(btn)

      when {
        !initedGmap -> {
          app.showToast(lifecycleScope, "Cannot start (map not ready)")
          mToggleOff(btn)
        }

        app.locationSmas.value.coord == null -> {
          app.showToast(lifecycleScope, "Cannot start (need an initial location)")
          mToggleOff(btn)
        }

        VM.miEnabled -> { VM.mu.start() }
      }
    }
  }

  fun mToggleOn(btn: MaterialButton) {
    utlUi.changeBackgroundMaterial(btn, R.color.colorPrimary)
    VM.miEnabled=true
  }

  fun mToggleOff(btn: MaterialButton) {
    utlUi.changeBackgroundMaterial(btn, R.color.darkGray)
    VM.miEnabled=false
  }

  private fun setupButtonFlir() {
    btnFlir = findViewById(R.id.button_flir)
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

          val curFloor = app.wFloor?.floorNumber()
          if (level != curFloor) {
            // app.showToast(lifecycleScope, "Changing floor: ${coord.level} (from: ${curFloor})")
            app.wFloors.moveToFloor(VM, level)
          }

          VM.ui.map.markers.addChatLocationMarker(Coord(lat, lon, level))
        }
      }
    }
  }

  @OptIn(ExperimentalMaterialApi::class, ExperimentalPermissionsApi::class) // compose
  private fun setupButtonChat() {
    LOG.D()
    btnChat = findViewById(R.id.button_chat)

    collectMessages()
    reactToNewMessages()

    btnChat.setOnClickListener {
      lifecycleScope.launch {
        if (!app.hasLastLocation()) {
          var msg = "Please localize at least once, or set location manually (long-click)\n"
          msg+= "before opening chat"
          app.showToast(lifecycleScope, msg, Toast.LENGTH_LONG)
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

  private fun readBackendVersion() {
    CoroutineScope(Dispatchers.IO).launch {
      val prefsChat = appSmas.dsSmas.read.first()
      if (prefsChat.version == null) {
        VM.nwVersion.getVersion()
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