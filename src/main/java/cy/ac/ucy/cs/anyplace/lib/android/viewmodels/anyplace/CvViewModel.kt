package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace

import android.app.Application
import android.graphics.Bitmap
import android.widget.Toast
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.MapBounds
import cy.ac.ucy.cs.anyplace.lib.android.SmasApp
import cy.ac.ucy.cs.anyplace.lib.android.cache.anyplace.Cache
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.ACT_NAME_SMAS
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.LevelWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.*
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.di.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.utils.imu.IMU
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.LevelSelector
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.CvUI
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.Classifier
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.DetectorActivityBase
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.di.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.UtilUI
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlImg
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw.*
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.*
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.smas.models.CvObjectReq
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject


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
}

/** CvMapViewModel is used as a base by:
 *  - Logger
 *  - SMAS
 */
@HiltViewModel
open class CvViewModel @Inject constructor(
        /** [application] is not an [AnyplaceApp], hence it is not a field.
        [AnyplaceApp] can be used within the class as app through an Extension function */
        application: Application,
        dsCv: CvDataStore,
        private val dsMisc: SpaceFilterDS,
        dsCvMap: CvMapDataStore,
        val repo: RepoAP,
        val RH: RetrofitHolderAP,
        val repoSmas: RepoSmas,
        val RHsmas: RetrofitHolderSmas, ): DetectorViewModel(application, dsCv, dsCvMap) {

  /** Make sure to initialize this one */
  private lateinit var attachedActivityId: String
  val app : AnyplaceApp = application as AnyplaceApp
  private val TG = "vm-cv"

  open val C by lazy { CONST(app) }
  private val utlUi by lazy { UtilUI(app.applicationContext, viewModelScope) }
  /** Updated on changes */
  lateinit var prefsCvMap: CvMapPrefs
  /** Updated on changes */
  lateinit var prefsCv: CvEnginePrefs

  val cache by lazy { Cache(application) }

  lateinit var mu: IMU
  var imuEnabled = false

  // UI
  //// COMPONENTS
  lateinit var levelSelector: LevelSelector
  /** Initialized when [GoogleMap] is initialized (see [setupUiGmap]) */
  var uiComponentInited = false
  var uiBottomInited = false
  lateinit var ui: CvUI

  // CV WINDOW: on Localization/Logging the detections are grouped per scanning window,
  // e.g., each window might be 5seconds.
  /** related to cv scan window */
  var currentTime : Long = 0
  var windowStart : Long = 0

  val nwConnections by lazy { ConnectionsGetNW(app, this, RH, repo) }
  val nwPOIs by lazy { POIsGetNW(app, this, RH, repo) }

  val nwCvModelsGet by lazy { CvModelsGetNW(app as SmasApp, this, RHsmas, repoSmas) }
  val nwCvMapGet by lazy { CvMapGetNW(app as SmasApp, this, RHsmas, repoSmas) }
  val nwCvFingerprintSend by lazy { CvFingerprintSendNW(app as SmasApp, this, RHsmas, repoSmas) }
  val nwCvLocalize by lazy { CvLocalizeNW(app as SmasApp, this, RHsmas, repoSmas) }
  val nwLevelPlan by lazy { LevelPlanNW(app as SmasApp, this, RHsmas, repoSmas) }

  /** Controlling localization mode */
  val statusLocalization = MutableStateFlow(LocalizationStatus.stopped)

  /** Detections for the localization scan-window */
  val detectionsLOC: MutableStateFlow<List<Classifier.Recognition>> = MutableStateFlow(emptyList())

  /** LastVals: user last selections regarding a space.
   * Currently not much use (for a field var), but if we have multiple
   * lastVals for space then it would make sense. */
  var lastValSpaces: LastValSpaces = LastValSpaces()

  /** workaround. no the best one */
  @Deprecated("this may be an ugly hack")
  suspend fun waitForUi() {
    while(!uiLoaded()) delay(100)
  }

  fun uiLoaded(): Boolean {
    return uiComponentInited &&
            ui.map.gmapWrLoaded &&
            uiBottomInited &&
            (!DBG.WAI || ui.localization.btnWhereAmISetup)
  }

  /**
   * React to [CvEnginePrefs] and [CvMapPrefs] changes
   */
  fun reactToPrefChanges() {
    viewModelScope.launch (Dispatchers.IO){
      dsCvMap.read.collectLatest { prefsCvMap=it }
      dsCv.read.collectLatest { prefsCv=it }
    }
  }

  protected open fun prefWindowLocalizationMs(): Int {
    return prefsCvMap.windowLocalizationMs.toInt()
  }

  open fun processDetections(recognitions: List<Classifier.Recognition>,
                             activity: DetectorActivityBase) {
    val MT = ::processDetections.name
    LOG.V2(TG, "$MT: ${recognitions.size}")
    when(statusLocalization.value) {
      LocalizationStatus.running -> {
        updateDetectionsLocalization(recognitions)
      }
      else -> {}
    }
  }

  /**
   * Update [detections] that are related only to the localization window.
   */
  protected fun updateDetectionsLocalization(detections: List<Classifier.Recognition>) {
    val MT = :: updateDetectionsLocalization.name
    LOG.W(TG, "$MT: updating for localization..")
    currentTime = System.currentTimeMillis()
    val appendedDetections = detectionsLOC.value + detections

    when {
      currentTime-windowStart > prefWindowLocalizationMs() -> { // window finished
        statusLocalization.tryEmit(LocalizationStatus.stopped)
        if (appendedDetections.isNotEmpty()) {
          LOG.W(TG, "$MT: stop: objects: ${appendedDetections.size}")
          // deduplicate detections (as we are scanning things in a window of a few seconds)
          val detectionsDedup = detectionsLOC.value
          // val detectionsDedup = YoloV4Classifier.NMS(detectionsLOC.value, detector.labels)
          LOG.W(TG, "$MT: stop: objects: ${detectionsDedup.size} (dedup)")

          // POINT OF LOCALIZING:
          if (!app.cvUtils.isModelInited()) {
            LOG.E(TG, "CvUtils: classes not inited")
            return
          }

          localizeCvMap(detectionsLOC.value)

          detectionsLOC.value = emptyList()
        } else {
          LOG.W(TG, "$MT: stopped. no detections..")
        }
      } else -> {  // Within a window
      detectionsLOC.value = appendedDetections as MutableList<Classifier.Recognition>
      LOG.D5(TG, "$MT: append: ${appendedDetections.size}")
    }
    }
  }

  var collectingCvLocalization = false
  fun localizeCvMap(recognitions: List<Classifier.Recognition>) {
    val MT = ::localizeCvMap.name
    LOG.D2(TG, "$MT: performing remote localization")

    // TODO convert detections
    val detectionsReq = app.cvUtils.toCvDetections(viewModelScope, recognitions, model)
    viewModelScope.launch(Dispatchers.IO) {

      if (!collectingCvLocalization) {
        collectingCvLocalization=true
        nwCvLocalize.collect()
      }

      // TODO: ALR: pick an option for localization
      // automode: has internet or not..
      if (DBG.CVM && !app.hasInternet()) {
        localizeOffline(detectionsReq)
        // return@launch
      } else {
        // REMOTE: ALGO
        nwCvLocalize.safeCall(app.wSpace.obj.buid, detectionsReq, model)
      }
    }
  }

  suspend fun localizeOffline(detectionsReq: List<CvObjectReq>) {
    if (!repoSmas.local.hasCvMap()) {
      val msg = "Cannot localize offline: No CvMap.\nUse settings to download the latest one."
      app.snackbarInf(viewModelScope, msg)
      return
    }

    val chatUser = app.dsUserAP.read.first()
    repoSmas.local.localize(this, model.idSmas, app.space!!.buid, detectionsReq, chatUser)
  }

  /** TODO in new class
   * If there was a previous level selection, it picks that.
   * Otherwise:
   * - it picks `0` if exists
   * - else the smallest level
   * Selects the first available level, or the last level that was picked
   * for a particular space.
   */
  fun selectInitialLevel() {
    val MT = ::selectInitialLevel.name
    LOG.V2()
    LOG.E(TG,"$MT: ${app.wSpace.prettyFloors}: ${app.wLevels.size}")

    if (!app.wLevels.hasLevels()) {
      val msg = "Selected ${app.wSpace.prettyTypeCapitalize} has no ${app.wSpace.prettyFloors}."
      LOG.W(TG, "$MT: msg")
      app.snackbarWarning(viewModelScope, msg)
      app.level.update { null }
    }

    if (app.wSpace.hasLastValuesCached()) {
      LOG.E(TG, "$MT: HAS last values cached")
      val lastVal = app.wSpace.loadLastValues()
      LOG.E(TG, "$MT: Space: ${app.wSpace.obj.name} has last level: ${lastVal.lastFloor}")
      if (lastVal.lastFloor != null) {
        LOG.E(TG, "$MT: lastVal cache: ${app.wSpace.prettyLevel}${lastVal.lastFloor}.")
        val lastLevel = app.wLevels.getLevel(lastVal.lastFloor!!)!!
        LOG.E(TG, "$MT: lastLevel: ${lastLevel.name} ${lastLevel.buid}")
        app.level.update { lastLevel }
      }
      lastValSpaces = lastVal
    } else {
      // try to get level 0. if not exists, get 1st available level
      // e.g. 1. a building might have just floors 3, and 4. it will pick 3
      // e.g. 1. a building might have floors -3 to 3. it will pick 0
      LOG.E(TG, "$MT: WILL load first level")
      val pickedLevel = app.wLevels.getLevel(0) ?: app.wLevels.getFirstLevel()
      app.level.update { pickedLevel }
      LOG.E(TG, "$MT: firstLevel: ${pickedLevel.name} ${pickedLevel.buid}")
    }

    LOG.E(TG, "$MT: Selected ${app.wSpace.obj.buid}/${app.level.value?.buid} ${app.wSpace.prettyLevel}: ${app.level.value!!.number}")
  }

  // TODO:PM network manager? ineternet connectiovity?
  var networkStatus = false
  /** normal var, filled by the observer (SelectSpaceActivity) */
  var backOnline = false

  // TODO:PM: bind this when connectivity status changes
  var readBackOnline = dsMisc.readBackOnline.asLiveData()
  var backFromSettings= false // INFO filled by the observer (collected from the fragment)
  var readBackFromSettings= dsMisc.readBackFromSettings.asLiveData()

  // TODO:PMX IST
  fun showNetworkStatus() {
    if (!networkStatus) {
      app.showToast(viewModelScope, C.ERR_MSG_NO_INTERNET, Toast.LENGTH_LONG)
      saveBackOnline(true)
    } else if(networkStatus && backOnline)  {
      app.showToast(viewModelScope, "Back online!", Toast.LENGTH_LONG)
      saveBackOnline(false)
    }
  }
  private fun saveBackOnline(value: Boolean) =
          viewModelScope.launch(Dispatchers.IO) {
            dsMisc.saveBackOnline(value)
          }
  fun setBackFromSettings() = saveBackFromSettings(true)
  fun unsetBackFromSettings() = saveBackFromSettings(false)
  private fun saveBackFromSettings(value: Boolean) =
          viewModelScope.launch(Dispatchers.IO) {  dsMisc.saveBackFromSettings(value) }

  open fun onLocalizationStarted() {
  }

  open fun onLocalizationEnded() {
  }

  var collectingOOB=false

  /**
   * Observes whether the user is in bounds and
   * updates the whereAmI functionality
   */
  fun collectUserOutOfBounds() {
    if (collectingOOB) return
    collectingOOB=true

    viewModelScope.launch(Dispatchers.IO) {

      // wait for UI components to become ready
      if (DBG.BG5) waitForUi()

      app.userOutOfBounds.collectLatest { state ->
        if (!DBG.WAI) return@collectLatest

        when (state) {
          MapBounds.inBounds -> {
            utlUi.fadeIn(ui.localization.btnWhereAmI)
            utlUi.changeBackgroundMaterial(ui.localization.btnWhereAmI, R.color.colorPrimary)
          }
          MapBounds.outOfBounds -> {
            utlUi.fadeIn(ui.localization.btnWhereAmI)
            utlUi.changeBackgroundMaterial(ui.localization.btnWhereAmI, R.color.darkGray)
            utlUi.attentionZoom(ui.localization.btnWhereAmI)
          }
          MapBounds.notLocalizedYet -> {
            // give it a sec, as auto-restore might kick-in on boot
            // delaying a bit more so the user is not overwhelmed
            delay(1500)

            // on success, then return (no need to show red icon first)
            if (app.hasLastLocation()) return@collectLatest

            utlUi.fadeIn(ui.localization.btnWhereAmI)
            utlUi.changeBackgroundMaterial(ui.localization.btnWhereAmI, R.color.redDark)
            utlUi.attentionZoom(ui.localization.btnWhereAmI)

            if (app.wLevel!= null) {
              ui.map.moveIfOutOufBounds(app.wLevel!!.bounds().center)
            }

            // show notification on smas
            if (attachedActivityId==ACT_NAME_SMAS) {
              // var msg = "Please localize or set location manually."
              // if (dsCvMap.read.first().autoSetInitialLocation) {
              //   // msg="Previous location expired.\nPlease localize or set it manually"
              // }
              // app.snackbarLong(viewModelScope, msg)
            }
          }
        }
      }
    }
  }

  fun setAttachedActivityId(actName: String) {
    attachedActivityId = actName
  }
}