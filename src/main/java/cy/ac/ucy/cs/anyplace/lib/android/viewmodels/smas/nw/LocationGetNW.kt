package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.nw

import android.widget.Toast
import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.FloorWrapper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.GmapWrapper
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.UserLocation
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.smas.ChatUserAuth
import cy.ac.ucy.cs.anyplace.lib.android.SmasApp
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.CHAT
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.smas.models.ChatUser
import cy.ac.ucy.cs.anyplace.lib.smas.models.UserLocations
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.source.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasChatViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasMainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import retrofit2.Response
import java.lang.Exception
import java.net.ConnectException

/**
 * Manages Location fetching of users.
 *
 * It fetches current user's location too.
 * The location per se is not used, but the last timestamp
 * of the current user msgs it is used
 * (to figure out whether the are new msgs to fetch)
 */
class LocationGetNW(
        private val app: SmasApp,
        private val VM: SmasMainViewModel,
        private val RH: RetrofitHolderSmas,
        private val repo: RepoSmas) {

  /** Another user in alert mode */
  val alertingUser: MutableStateFlow<UserLocation?> = MutableStateFlow(null)

  private val err by lazy { SmasErrors(app, VM.viewModelScope) }

  /** Network Responses from API calls */
  private val resp: MutableStateFlow<NetworkResult<UserLocations>> = MutableStateFlow(NetworkResult.Unset())

  private val C by lazy { CHAT(app.applicationContext) }
  private lateinit var chatUser : ChatUser

  /** Get [UserLocations] SafeCall */
  suspend fun safeCall() {
    LOG.D3(TAG, "LocationGet")
    chatUser = app.dsChatUser.readUser.first()

    resp.value = NetworkResult.Loading()
    if (app.hasInternet()) {
      try {
        val response = repo.remote.locationGet(ChatUserAuth(chatUser))
        LOG.D4(TAG, "LocationGet: ${response.message()}" )
        resp.value = handleResponse(response)

        // TODO: Persist: put in cache & list (main mem) ?
        // val userLocations = resp.value.data
        // if (userLocations != null) { cache(useLocations, UserOwnership.PUBLIC) }
      } catch(ce: ConnectException) {
        val msg = "Connection failed:\n${RH.retrofit.baseUrl()}"
        handleException(msg, ce)
      } catch(e: Exception) {
        val msg = "$TAG: Not Found." + "\nURL: ${RH.retrofit.baseUrl()}"
        handleException(msg, e)
      }
    } else {
      resp.value = NetworkResult.Error(C.ERR_MSG_NO_INTERNET)
    }
  }

  private fun handleResponse(resp: Response<UserLocations>): NetworkResult<UserLocations> {
    LOG.D3(TAG)
    if(resp.isSuccessful) {
      return when {
        resp.message().toString().contains("timeout") -> NetworkResult.Error("Timeout.")

        resp.isSuccessful -> {
          // SMAS special handling (errors should not be 200/OK)
          val r = resp.body()!!
          if (r.status == "err") return NetworkResult.Error(r.descr)

          return NetworkResult.Success(r)
        } // can be nullable
        else -> NetworkResult.Error(resp.message())
      }
    } else {
      LOG.E(TAG, "handleResponse: unsuccessful")
    }

    return NetworkResult.Error("$TAG: ${resp.message()}")
  }

  private fun handleException(msg:String, e: Exception) {
    resp.value = NetworkResult.Error(msg)
    LOG.E(TAG, msg)
    LOG.E(TAG, e)
  }

  suspend fun collect(VMchat: SmasChatViewModel, gmap: GmapWrapper) {
    LOG.D3()

    resp.collect {
      when (it)  {
        is NetworkResult.Success -> {
          val locations = it.data
          LOG.D4(TAG, "Got user locatiens: ${locations?.rows?.size}")
          processUserLocations(VMchat, locations, gmap)
        }
        is NetworkResult.Error -> {
          LOG.D3(TAG, "Error: msg: ${it.message}")
          if (!err.handle(app, it.message, "loc-get")) {
            val msg = it.message ?: "unspecified error"
            app.showToast(VM.viewModelScope, msg, Toast.LENGTH_SHORT)
          }
        }
        else -> {}
      }
    }
  }

  /**
   * Processes the received locations:
   * - filters own users (1) from other users (2), some of which might be alerting
   *
   * 1. Own User
   * - pulls new msgs if have to
   *
   * 2. Other users
   * - propagates this information so the user locations can be rendered on the map
   */
  private fun processUserLocations(
          VMchat: SmasChatViewModel, locations: UserLocations?, gmap: GmapWrapper) {
    LOG.D3(TAG_METHOD)
    if (locations == null) return

    val FH = FloorWrapper(VM.floor.value!!, VM.wSpace)
    val sameFloorUsers = locations.rows.filter { userLocation ->
      userLocation.buid == FH.spaceH.obj.id &&  // same space
              userLocation.deck == FH.obj.floorNumber.toInt() && // same deck
              userLocation.uid != chatUser.uid // not current user
    }

    val alertingUsers = locations.rows.filter { userLocation ->
      userLocation.alert == 1 &&
              userLocation.uid != chatUser.uid // not current user
    }

    val ownLocation = locations.rows.filter { it.uid == chatUser.uid }
    if (ownLocation.isNotEmpty()) {
      // when the current user has not (ever) reported its own location,
      // then it might not be included in the locations DB
      checkForNewMsgs(VMchat, ownLocation[0].lastMsgTime)
    }

    // pick the first alerting user
    // CHECK edge case: more than one?
    if (alertingUsers.isNotEmpty()) {
      alertingUser.value = alertingUsers[0]
    } else {
     alertingUser.value = null
    }

    LOG.D3(TAG, "UserLocations: current floor: ${FH.prettyFloorName()}")
    // val dataset = MutableList<>(); // TODO: scalability?
    gmap.renderUserLocations(sameFloorUsers)

    if (DBG.D2) {
      sameFloorUsers.forEach {
        LOG.D4(TAG, "User: ${it.uid} on floor: ${it.deck}")
      }
    }
  }

  private fun checkForNewMsgs(VMchat: SmasChatViewModel, lastMsgTs: Long) {
    val localTs = repo.local.getLastMsgTimestamp()
    LOG.V2(TAG, "$METHOD: ${VM.hasNewMsgs(localTs, lastMsgTs)}")

    if (VM.hasNewMsgs(localTs, lastMsgTs)) {
      LOG.W(TAG, "$METHOD: ${VM.hasNewMsgs(localTs, lastMsgTs)}: pulling msgs..")
      VMchat.netPullMessagesONCE()
    }
  }
}