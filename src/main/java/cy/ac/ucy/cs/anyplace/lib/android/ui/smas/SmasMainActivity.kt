package cy.ac.ucy.cs.anyplace.lib.android.ui.smas

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.ExperimentalMaterialApi
import androidx.constraintlayout.widget.Group
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.BuildConfig
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.appSmas
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.LocalizationStatus
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.CvMapActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.GmapWrapper
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.Classifier
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilNotify
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.OutlineTextView
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.utlButton
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.DetectorViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationResult
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.UserCoordinates
import cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs.smas.MainSmasSettingsDialog
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.chat.SmasChatActivity
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasChatViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasMainViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.nw.LocationSendNW
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/*
  CONST: from string/xml
  - prefs:
    - xml
    - ChatDataStore
    - SettingsActivity (uses CDS)

  - RetrofitSetup: (with all the above)
    - RetrofitHolder: VERIFY dynamically adapt to domain change

    - API Setup:
      - remoteDataSources (talks with API interface)
      - connection verification with server (due to some issues)1
    - ENDPOINTS:
      - version: check I can talk
      - login: working on it (below)
    - models

    - loginProgrammatically
      - TODO build UI for this
      - TODO persist in a ChatUser DataStore

  - ViewModelChat: this will be used by Athina

  - working on:
    - ChatUserDataSource: to preserve the logged in user

    - TODO: persist user login: store ChatUser DS

    TODO:
    - async get version of the SMAS backend (in start activity maybe..)

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

  // VIEW MODELS
  /** extends [CvViewModel] */
  private lateinit var VM: SmasMainViewModel

  /** Async handling of SMAS Messages and Alerts */
  private lateinit var VMchat: SmasChatViewModel

  // UI COMPONENTS
  private lateinit var btnChat: Button
  private lateinit var btnFlir: Button
  private lateinit var btnSettings: Button
  private lateinit var btnAlert: Button

  private val utlNotify by lazy { UtilNotify(applicationContext) }

  /** whether this activity is active or not */
  private var isActive = false

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
  }

  override fun onMapReadyCallback() {
      // nothing for now..
  }

  /**
   * Called by [CvMapActivity] ? (some parent method)
   */
  override fun postCreate() {
    super.postCreate()
    LOG.D2()

    LOG.W(TAG, "main: onPostCreate")

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
    LOG.D2(TAG_METHOD, "Floor: ${VM.floor.value}")

    super.onFirstFloorLoaded()

    // Send own location, and receive other users locations
    // VM.nwUpdateLocationsLOOP(true, "main")
    updateLocationsLOOP()

    VM.collectLocations(VMchat, ui.map)

    // collect alert TODO:PMX
  }


  /* Runs when any of the floors is loaded */
  override fun onFloorLoaded() {
    super.onFloorLoaded()
  }


  /**
   * In a loop:
   * - conditionally send own location
   * - get other users locations
   *
   *  - TODO:PM get from anyplace location
   *  - TODO:PM get a list of those locations: how? parse json?
   */
  private fun updateLocationsLOOP()  {
    lifecycleScope.launch(Dispatchers.IO) {

      // VM.collectRefreshMs()
      while (true) {
        var msg = "pull"
        val hasRegisteredLocation = VM.locationREMOTE.value.coord != null
        if (isActive && hasRegisteredLocation) {
          val lastCoordinates = UserCoordinates(VM.wSpace.obj.id,
                  VM.wFloor?.obj!!.floorNumber.toInt(),
                  VM.locationREMOTE.value.coord!!.lat,
                  VM.locationREMOTE.value.coord!!.lon)

          VM.nwLocationSend.safeCall(lastCoordinates)
          msg+="&send"
        }

        msg="($msg) "
        if (!isActive) msg+=" [inactive]"
        if (!hasRegisteredLocation) msg+=" [no-user-location-registered]"

        LOG.W(TAG, "loop-location: main: $msg")

        VM.nwLocationGet.safeCall()
        delay(VM.prefsCvNav.locationRefreshMs.toLong())
      }
    }
  }

  ////////////////////////////////////////////////

  override fun onResume() {
    super.onResume()
    LOG.W(TAG, "main: resumed")
    isActive = true
  }

  override fun onPause() {
    super.onPause()
    LOG.W(TAG, "main: paused")
    isActive = false
  }

  /**
   * Async Collection of remotely fetched data
   * TODO local cache (SQLITE)
   */
  private fun setupCollectors() {
    LOG.D()

    collectLoggedInUser()
    observeFloors()

    // NOTE: [collectOtherUsersLocations] is done on floorLoaded
    // collectUserLocalizationStatus(): localizing or not localizing
  }

  @Deprecated("TODO replace with Anyplace Location")
  private fun setupFakeUserLocation(mapH: GmapWrapper) {
    val floorNum = VM.wFloor!!.floorNumber()
    val loc = VM.wSpace.latLng().toCoord(floorNum)
    VM.locationREMOTE.value = LocalizationResult.Success(loc)

    lifecycleScope.launch(Dispatchers.Main) {
      mapH.obj.setOnMapLongClickListener {
        LOG.W(TAG, "Setting fake location: $it")
        VM.locationREMOTE.value = LocalizationResult.Success(it.toCoord(floorNum))
      }
    }
  }


  // CLR:PM: this was moved to [CvMapActivity]
  // /**
  //  * Observes when the initial floor will be loaded, and runs a method
  //  */
  // var firstFloorLoaded = false
  // private fun collectLoadedFloors() {
  //   super.collectLoadedFloors()
  //
  //   lifecycleScope.launch {
  //     VM.floor.collect { floor ->
  //       if (floor == null) return@collect
  //
  //       LOG.D4(TAG, "collectLoadedFloors: is spaceH filled? ${VM.spaceH.obj.name}")
  //       // Update FH
  //       VM.floorH = FloorHelper(floor, VM.spaceH)
  //
  //       if (firstFloorLoaded) {
  //         cancel()
  //       } else {
  //         onFloorLoaded()
  //         firstFloorLoaded = true
  //       }
  //     }
  //   }
  // }


  /**
   * Update the UI button when new msgs come in
   */
  private fun reactToNewMessages() {
    lifecycleScope.launch(Dispatchers.Main) {
      VM.readHasNewMessages.observeForever { hasNewMsgs ->
      val btn = btnChat as MaterialButton
      val ctx = this@SmasMainActivity
      LOG.E(TAG,"NEW-MSGS: $hasNewMsgs")

        if (hasNewMsgs) {
          utlButton.changeBackgroundButtonDONT_USE(btn, ctx, R.color.redDark)
          utlButton.changeMaterialButtonIcon(btn, ctx, R.drawable.ic_chat_unread)
          utlNotify.msgReceived()
        } else {
          utlButton.changeBackgroundButtonDONT_USE(btn, ctx, R.color.colorPrimaryDark)
          utlButton.changeMaterialButtonIcon(btn, ctx, R.drawable.ic_chat)
        }
      }
    }
  }

  /**
   * Reacts to updates on [ChatUser]'s login status:
   * Only authenticated users are allowed to use this activity
   */
  private fun collectLoggedInUser() {
    // only logged in users are allowed on this activity:
    lifecycleScope.launch(Dispatchers.IO) {
      appSmas.dsChatUser.readUser.collect { user ->
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
  }

  /**
   * React when a user is in alert mode
   */
  @SuppressLint("SetTextI18n")
  private fun collectAlertingUser() { // TODO:PMX
    lifecycleScope.launch {
      val group: Group = findViewById(R.id.group_userAlert)
      val tvUserAlert: OutlineTextView = findViewById(R.id.tv_alertUser)
      val tvAlertTitle: OutlineTextView = findViewById(R.id.tv_alertTitle)
      VM.alertingUser.collect {
        if (it == null) { // no user alerting
          // btnAlert.visibility = View.VISIBLE
          group.fadeOut()
          delay(100)
          btnAlert.fadeIn()
          tvAlertTitle.clearAnimation()
          // group.visibility = View.INVISIBLE
        } else { // user alerting
          tvUserAlert.text = "${it.name} ${it.surname}"
          btnAlert.fadeOut()
          delay(100)
          group.fadeIn()
          delay(100)
          tvAlertTitle.flashingLoop()
          // btnAlert.visibility = View.INVISIBLE
          // group.visibility = View.VISIBLE
        }
      }
    }
  }

  private fun setupButtonAlert() {
    btnAlert = findViewById(R.id.btnAlert)
    btnAlert.setOnClickListener {
      Toast.makeText(applicationContext, "long-press (to alert)", Toast.LENGTH_SHORT).show()
    }

    btnAlert.setOnLongClickListener {
      when (VM.toggleAlert()) {
        LocationSendNW.Mode.alert -> {
          btnAlert.flashingLoop()
          btnAlert.text = "ALERTING"
          utlButton.changeBackgroundButtonCompat(btnAlert, this, R.color.redDark)
          btnAlert.setTextColor(Color.WHITE)
        }
        LocationSendNW.Mode.normal -> {
          btnAlert.clearAnimation()
          btnAlert.text = "SEND ALERT"
          btnAlert.setTextColor(Color.BLACK)
          utlButton.changeBackgroundButtonCompat(btnAlert, this, R.color.yellowDark)
        }
      }
      true
    }
  }

  private fun setupButtonSettings() {
    btnSettings = findViewById(R.id.button_settings)
    btnSettings.setOnClickListener {

      val versionStr = BuildConfig.LIB_VERSION
      MainSmasSettingsDialog.SHOW(supportFragmentManager,
              MainSmasSettingsDialog.FROM_MAIN, this@SmasMainActivity, versionStr)
    }
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

  private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
    if (result.resultCode == Activity.RESULT_OK) {
      val intent: Intent? = result.data
      if (intent != null) {
        val lat = intent.getDoubleExtra("latitude", 0.0)
        val lon = intent.getDoubleExtra("longitude", 0.0)

        LOG.E(TAG, "GOT LOC FROM CHAT: $lat $lon")
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
      val prefsChat = appSmas.dsChat.read.first()
      if (prefsChat.version == null) {
        VM.nwVersion.getVersion()
      }
    }
  }

  // TODO:PMX FR
  override fun onInferenceRan(detections: MutableList<Classifier.Recognition>) {
    ui.onInferenceRan()

    if (detections.isNotEmpty()) {
      LOG.D3(TAG, "$METHOD: detections: ${detections.size} (LOGGER OVERRIDE)")
    }
    VM.processDetections(detections)
  }

}