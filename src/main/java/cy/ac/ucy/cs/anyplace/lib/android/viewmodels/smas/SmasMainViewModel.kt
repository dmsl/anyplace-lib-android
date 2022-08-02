package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas

import android.app.Application
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.Preference
import cy.ac.ucy.cs.anyplace.lib.android.SmasApp
import cy.ac.ucy.cs.anyplace.lib.android.appSmas
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.SMAS
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.CvDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.CvMapDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.MiscDS
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.di.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.store.SmasDataStore
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.GmapWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.di.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.nw.LocationGetNW
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.nw.LocationSendNW
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.nw.VersionSmasNW
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.UserLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Extends [CvViewModel]:
 * - TODO here merge chat / messages
 */
@HiltViewModel
class SmasMainViewModel @Inject constructor(
        application: Application,
        repoAP: RepoAP,
        repoSmas: RepoSmas,
        val dsChat: SmasDataStore,
        dsCv: CvDataStore,
        dsCvMap: CvMapDataStore,
        dsMisc: MiscDS,
        RHsmas: RetrofitHolderSmas,
        RHap: RetrofitHolderAP):
        CvViewModel(application, dsCv, dsMisc, dsCvMap, repoAP, RHap, repoSmas, RHsmas) {
  private val TG = "vm-cv-smas"
  val ctx by lazy { app.applicationContext }
  override val C by lazy { SMAS(ctx) }

  // PREFERENCES
  val prefsChat = dsChat.read

  override fun prefWindowLocalizationMs(): Int {
    // modify properly for Smas?
    return C.DEFAULT_PREF_CV_WINDOW_LOCALIZATION_MS.toInt()
  }

  //// RETROFIT UTILS:
  val nwVersion by lazy { VersionSmasNW(app as SmasApp, RHsmas, repoSmas) }
  val nwLocationGet by lazy { LocationGetNW(app as SmasApp, this, RHsmas, repoSmas) }
  val nwLocationSend by lazy { LocationSendNW(app as SmasApp, this, RHsmas, repoSmas) }

  val alertingUser : MutableStateFlow<UserLocation?>
    get() = nwLocationGet.alertingUser

  /**
   * [p]: the Chat [Preference] row that will be replaced with the result of the version call
   */
  fun displayVersion(p: Preference?) = viewModelScope.launch { nwVersion.safeCallAndUpdateUi(p) }

  /**
   * React to user location updates:
   * - for current user [nwLocationSend]
   * - for other users [nwLocationGet]
   */
  var collectingLocations = false
  fun collectLocations(VMchat: SmasChatViewModel,mapH: GmapWrapper) {
    if (collectingLocations) return
    collectingLocations=true
    if (app.level.value == null) {  // floor not ready yet
      LOG.W(TAG_METHOD, "Floor not loaded yet")
      return
    }

    viewModelScope.launch(Dispatchers.IO) { nwLocationSend.collect() }
    viewModelScope.launch(Dispatchers.IO) { nwLocationGet.collect(VMchat, mapH) }
  }

  fun toggleAlert() : LocationSendNW.Mode {
    val newMode = if (nwLocationSend.alerting()) {
      LocationSendNW.Mode.normal
    } else {
      LocationSendNW.Mode.alert
    }

    nwLocationSend.mode.value = newMode
    return newMode
  }

  fun hasNewMsgs(localTs: Long?, remoteTs: Long?) : Boolean {
    LOG.V2(TG, "$METHOD: local: $localTs remote: $remoteTs")

    return when {
      // there is no remote timestamp
      remoteTs == null || remoteTs == 0L -> false

      // remote timestamp exists, but a local does not
      remoteTs != 0L && localTs == null -> true

      // both timestamps exist, and the remote one is more up-to-date
      // (this might end up loading local+remote new data)
      (remoteTs > localTs!!) -> true

      // there is a local timestamp (localTs != null; checked above)
      // meaning the DB has data, but the msgList is empty:
      // we have not loaded the local msgs yet
      appSmas.msgList.isEmpty() -> true

      // both timestamps exist
      else -> false
    }
  }

  fun readBackendVersion() {
    viewModelScope.launch(Dispatchers.IO) {
      val prefsChat = appSmas.dsSmas.read.first()
      if (prefsChat.version == null) {
        nwVersion.getVersion()
      }
    }
  }

  /** Set when a user has new messages */
  var readHasNewMessages = dsChat.readHasNewMessages.asLiveData()
}