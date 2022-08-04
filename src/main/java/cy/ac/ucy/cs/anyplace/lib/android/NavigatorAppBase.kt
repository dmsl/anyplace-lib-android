package cy.ac.ucy.cs.anyplace.lib.android

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.asLiveData
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.smas.models.ChatMsg
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.store.SmasDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.di.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasChatViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasMainViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The Navigator
 * BASE [AnyplaceApp] for [SmasApp] and [NavigatorApp]
 */
abstract class NavigatorAppBase : AnyplaceApp() {
  private val TG ="app-nav-base"

  @Inject lateinit var rfhSmas: RetrofitHolderSmas
  @Inject lateinit var dsChat: SmasDataStore

  /** list of messages shown on screen by [LazyColumn] */
  var msgList = mutableStateListOf<ChatMsg>()

  /** The VM set by [SmasMainActivity] */
  private var VM: SmasMainViewModel?= null
  /** The VMchat set by [SmasMainActivity] */
  var VMchat : SmasChatViewModel? = null

  override fun onCreate() {
    super.onCreate()

    observeSmasPrefs()
  }

  /**
   * Set from the [SmasMainActivity]/[CvNavigatorActivity],
   * to allow triggering the [SmasMainViewModel] from the [SmasChatActivity]
   */
  fun setMainActivityVMs(VM: SmasMainViewModel, VMchat: SmasChatViewModel) {
    this.VM=VM
    this.VMchat=VMchat
  }

  /**
   * NOTE: this runs from a different activity ([SettingsChatActivity])
   *       and affects a ViewModel of [SmasMainActivity]
   *
   */
  fun pullMessagesONCE() {
    val MT = ::pullMessagesONCE.name
    LOG.V2(TG, "$MT: using VMchat of Main Activity?")
    VMchat?.nwPullMessages(false)
  }

  fun setUnreadMsgsState(scope: CoroutineScope, value: Boolean) {
    scope.launch(Dispatchers.IO) {
      dsChat.saveNewMsgs(value)
    }
  }

  /** Manually create a new instance of the RetrofitHolder on pref changes */
  private fun observeSmasPrefs() {
    val prefsChat = dsSmas.read
    prefsChat.asLiveData().observeForever { prefs ->
      rfhSmas.set(prefs)

      LOG.D2(TG, "SMAS API URL: ${rfhSmas.baseURL} (${rfhSmas.retrofit.baseUrl()})")
    }
  }

  /**
   * Stops the receival of messages and waits if there is an ongoing receival
   *
   * NOTE: this runs from a different activity ([SettingsChatActivity])
   *       and affects a ViewModel of [SmasMainActivity]
   */
  fun stopMsgGetBLOCKING() {
    val MT = ::stopMsgGetBLOCKING.name
    VMchat?.nwMsgGet?.skipCall=true
    while (VMchat?.nwMsgGet?.resp?.value is NetworkResult.Loading) {
      LOG.D2(TG, "$MT: waiting for last MsgGet to end..")
    }
  }

  /**
   * NOTE: this runs from a different activity ([SettingsChatActivity])
   *       and affects a ViewModel of [SmasMainActivity]
   */
  fun resumeMsgGet() {
    VMchat?.nwMsgGet?.skipCall = false
  }
}
