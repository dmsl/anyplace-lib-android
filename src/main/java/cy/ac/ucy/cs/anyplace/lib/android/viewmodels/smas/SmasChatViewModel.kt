package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.CvMapDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.MiscDataStore
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.UserCoordinates
import cy.ac.ucy.cs.anyplace.lib.android.SmasApp
import cy.ac.ucy.cs.anyplace.lib.android.cache.smas.SmasCache
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.SMAS
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.smas.models.ReplyToMessage
import cy.ac.ucy.cs.anyplace.lib.smas.models.UserLocations
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.store.ChatPrefsDataStore
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlImg
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.source.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs.smas.ImgDialog
import cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs.smas.MsgDeliveryDialog
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.theme.AnyplaceBlue
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.nw.MsgGetNW
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.nw.MsgSendNW
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles everything related to the Chat.
 *
 * [UserLocations] are handled by [SmasMainViewModel]
 *
 */
@HiltViewModel
class SmasChatViewModel @Inject constructor(
  private val _application: Application,
  private val repoSmas: RepoSmas,
  private val RFH: RetrofitHolderSmas,
  private val dsChat: ChatPrefsDataStore,
  dsCvMap: CvMapDataStore,
  private val dsMisc: MiscDataStore,
) : AndroidViewModel(_application) {

  val tag = "vm-smas-chat"

  private val app = _application as SmasApp
  private val C by lazy { SMAS(app.applicationContext) }

  val nwMsgGet by lazy { MsgGetNW(app, this, RFH, repoSmas) }
  private val nwMsgSend by lazy { MsgSendNW(app, this, RFH, repoSmas) }

  val chatCache by lazy { SmasCache(app.applicationContext) }

  // Preferences
  val prefsCvMap = dsCvMap.read

  //Variables observed by composable functions
  var reply: String by mutableStateOf("")
  var imageUri: Uri? by mutableStateOf(null)
  var showDialog: Boolean by mutableStateOf(false)
  var replyToMessage: ReplyToMessage? by mutableStateOf(null)
  var mdelivery: String by mutableStateOf("")
  var errColor: Color by mutableStateOf(AnyplaceBlue)
  var isLoading: Boolean by mutableStateOf(false)

  var newMessages = MutableStateFlow(false)

  fun getLoggedInUser(): String {
    var uid = ""
    viewModelScope.launch {
      uid = app.dsSmasUser.read.first().uid
    }
    return uid
  }

  fun setDeliveryMethod(){
    viewModelScope.launch {
      val chatPrefs = dsChat.read.first()
      mdelivery = chatPrefs.mdelivery
    }
  }

  fun openMsgDeliveryDialog(fragmentManager: FragmentManager){
    MsgDeliveryDialog.SHOW(fragmentManager, dsChat, app,this)
  }

  fun openImgDialog(fragmentManager: FragmentManager, img: Bitmap){
    ImgDialog.SHOW(fragmentManager, img)
  }

  fun clearReply() {
    reply = ""
  }

  fun clearImgUri() {
    imageUri = null
  }

  fun clearTheReplyToMessage() {
    replyToMessage = null
  }

  fun netPullMessagesONCE(showToast: Boolean = false) {
    LOG.D2(TAG, "PULL-MSGS")
    viewModelScope.launch(Dispatchers.IO) {
      nwMsgGet.safeCall(showToast)
    }
  }

  private fun getUserCoordinates(VM: SmasMainViewModel): UserCoordinates? {
    val userCoord: UserCoordinates?
    if (VM.locationSmas.value.coord != null) {
      userCoord = UserCoordinates(VM.wSpace.obj.id,
              VM.wFloor?.obj!!.floorNumber.toInt(),
              VM.locationSmas.value.coord!!.lat,
              VM.locationSmas.value.coord!!.lon)
      return userCoord
    }

    return null
  }

  private fun getCenterOfFloor(VM: SmasMainViewModel): UserCoordinates {
    val latLng = VM.wSpace.latLng()
    return UserCoordinates(VM.wSpace.obj.id,
            VM.wFloor?.obj!!.floorNumber.toInt(),
            latLng.latitude,
            latLng.longitude)
  }

  fun sendMessage(VM: SmasMainViewModel, newMsg: String?, mtype: Int) {
    viewModelScope.launch {
      var userCoordinates = getUserCoordinates(VM)
      if (userCoordinates==null) {
        val msg = "Cannot attach location to msg.\nUsing selected floor's (${VM.floor.value?.floorNumber}) center."
        LOG.E(TAG, "$tag: $METHOD: msg")
        app.showToast(this, msg)
        userCoordinates = getCenterOfFloor(VM)
      }

      val chatPrefs = dsChat.read.first()
      val mdelivery = chatPrefs.mdelivery
      var mexten: String? = null
      if (imageUri != null) {
        mexten = utlImg.getMimeType(imageUri!!, app)
      }

      nwMsgSend.safeCall(userCoordinates, mdelivery, mtype, newMsg, mexten)

      // can be more strict: send msg only if location was available
      // if (userCoord != null)
      //   nwMsgsSend.safeCall(userCoord, mdelivery, mtype, newMsg, mexten)
      // else{
      //   Toast.makeText(app,"Localization problem. Message cannot be delivered.",Toast.LENGTH_SHORT)
      //   LOG.E("Localization problem. Message cannot be delivered.")
      // }
    }

    collectMsgsSend()
  }

  /**
   * React to flow that is populated by [nwMsgSend] safeCall
   */
  fun collectMsgsSend() {
    viewModelScope.launch {
      nwMsgSend.collect()
    }
  }

  fun savedNewMsgs(value: Boolean) {
    viewModelScope.launch(Dispatchers.IO) {
      dsChat.saveNewMsgs(value)
    }
  }

}