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
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.store.SmasDataStore
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlImg
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.di.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs.smas.ImgDialog
import cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs.smas.MsgDeliveryDialog
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.theme.AnyplaceBlue
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.nw.MsgGetNW
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.nw.MsgSendNW
import cy.ac.ucy.cs.anyplace.lib.smas.models.CONSTchatMsg.MTYPE_LOCATION
import cy.ac.ucy.cs.anyplace.lib.smas.models.CONSTchatMsg.MTYPE_TXT
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
        private val dsChat: SmasDataStore,
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
      uid = app.dsUserSmas.read.first().uid
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

  fun nwPullMessages(showToast: Boolean = false) {
    LOG.D2(TAG, "PULL-MSGS")
    viewModelScope.launch(Dispatchers.IO) {
      nwMsgGet.safeCall(showToast)
    }
  }

  private fun getUserCoordinates(): UserCoordinates? {
    val userCoord: UserCoordinates?
    if (app.locationSmas.value.coord != null) {
      val smasCoord =  app.locationSmas.value.coord!!
      userCoord = UserCoordinates(app.wSpace.obj.buid,
              smasCoord.level,
              smasCoord.lat,
              smasCoord.lon)
      return userCoord
    }

    return null
  }

  private fun getCenterOfFloor(): UserCoordinates {
    val latLng = app.wSpace.latLng()
    return UserCoordinates(app.wSpace.obj.buid,
            app.wLevel?.obj!!.number.toInt(),
            latLng.latitude,
            latLng.longitude)
  }

  data class ClipboardLocation(
          val uid: String,
          val deck: Int,
          val lat: Double,
          val lon: Double,
          ) {
    override fun toString() = "$uid,$deck,$lat,$lon"
    companion object {
      fun fromString(str: String?) : ClipboardLocation? {
        if (str==null) return null

        val arr = str.split(",").toTypedArray()
        if (arr.size != 4) return null

        return try {
          val uid = arr[0].toString()
          val deck = arr[1].toInt()
          val lat = arr[2].toDouble()
          val lon = arr[3].toDouble()

          ClipboardLocation(uid, deck, lat, lon)
        } catch (e: Exception) {
          null
        }
      }
    }
  }

  fun sendMessage(newMsg: String?, mtype: Int) {
    viewModelScope.launch(Dispatchers.IO) {
      var ownUserCoords = getUserCoordinates()
      if (ownUserCoords==null) {
        val msg = "Cannot attach location to msg.\nUsing selected floor's (${app.level.value?.number}) center."
        LOG.E(TAG, "$tag: $METHOD: $msg")
        ownUserCoords = getCenterOfFloor()
      }

      var msgProcessed = false
      // text type might have a pasted [ClipboardLocation]
      if (mtype == MTYPE_TXT) {
        val pastedLocation = ClipboardLocation.fromString(newMsg)
        if (pastedLocation != null) {

          // if sharing through clipboard the location of another user (not ourself)
          // send 2 messages:
          // 1. a text, acknowledging the location sharing
          // 2. a location message of the attached location
          // otherwise: if it's our own location, just share it as normal..

          val ownUid = app.dsUserSmas.read.first().uid

          // share location
          val otherUserCoords = UserCoordinates(app.space!!.buid,
                  pastedLocation.deck,
                  pastedLocation.lat,
                  pastedLocation.lon)

          val locationInfo = if (pastedLocation.uid != ownUid)
            "location of: ${pastedLocation.uid}" else ""
          sendMessageInternal(otherUserCoords, locationInfo, MTYPE_LOCATION)

          msgProcessed=true
        }
      }

      if (!msgProcessed) {
        sendMessageInternal(ownUserCoords, newMsg, mtype)
      }

      delay(100)
      collectMsgsSend()
    }
  }


  private fun sendMessageInternal(userCoords: UserCoordinates, newMsg: String?, mtype: Int) {
    viewModelScope.launch(Dispatchers.IO) {
      val chatPrefs = dsChat.read.first()
      val mdelivery = chatPrefs.mdelivery
      var mexten: String? = null
      if (imageUri != null) {
        mexten = utlImg.getMimeType(imageUri!!, app)
      }

      nwMsgSend.safeCall(userCoords, mdelivery, mtype, newMsg, mexten)
    }
  }


  /**
   * React to flow that is populated by [nwMsgSend] safeCall
   */
  fun collectMsgsSend() {
    viewModelScope.launch(Dispatchers.IO) {
      nwMsgSend.collect()
    }
  }

  fun savedNewMsgs(value: Boolean) {
    viewModelScope.launch(Dispatchers.IO) {
      dsChat.saveNewMsgs(value)
    }
  }

}