package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.nw

import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.LevelWrapper
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.GmapWrapper
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.UserLocation
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.smas.ChatUserAuth
import cy.ac.ucy.cs.anyplace.lib.android.SmasApp
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.SMAS
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.smas.models.SmasUser
import cy.ac.ucy.cs.anyplace.lib.smas.models.UserLocations
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.di.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.utils.*
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasChatViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasMainViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationResult
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.toCoord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
  private val TG = "nw-location-get"
  private val notify = app.notify
  private val scope = VM.viewModelScope
  private val utlErr by lazy { UtilErr() }

  /** Another user in alert mode */
  val alertingUser: MutableStateFlow<UserLocation?> = MutableStateFlow(null)

  private val err by lazy { SmasErrors(app, scope) }

  /** Network Responses from API calls */
  private val resp: MutableStateFlow<NetworkResult<UserLocations>> = MutableStateFlow(NetworkResult.Unset())

  private val C by lazy { SMAS(app.applicationContext) }
  private lateinit var smasUser : SmasUser

  /** Get [UserLocations] SafeCall */
  suspend fun safeCall() {
    val MT = ::safeCall.name
    LOG.D3(TG, MT)
    smasUser = app.dsUserSmas.read.first()

    resp.value = NetworkResult.Loading()
    if (app.hasInternet()) {
      try {
        val response = repo.remote.locationGet(ChatUserAuth(smasUser))
        LOG.D4(TG, "$MT: ${response.message()}" )
        resp.value = handleResponse(response)
      } catch(ce: ConnectException) {
        val msg = "Connection failed:\n${RH.retrofit.baseUrl()}"
        handleException(msg, ce)
      } catch(e: Exception) {
        val msg = "$TG: Not Found." + "\nURL: ${RH.retrofit.baseUrl()}"
        handleException(msg, e)
      }
    } else {
      resp.value = NetworkResult.Error(C.ERR_MSG_NO_INTERNET)
    }
  }

  private fun handleResponse(resp: Response<UserLocations>): NetworkResult<UserLocations> {
    val MT = ::handleResponse.name
    LOG.D3(TG, MT)
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
      LOG.E(TG, "$MT: unsuccessful")
    }
    return NetworkResult.Error("$TAG: ${resp.message()}")
  }

  private fun handleException(msg:String, e: Exception) {
    val MT = ::handleException.name
    resp.value = NetworkResult.Error(msg)
    LOG.E(TG, "$MT: msg")
    LOG.E(TG, MT, e)
  }

  suspend fun collect(VMchat: SmasChatViewModel, gmap: GmapWrapper) {
    val MT = ::collect.name
    LOG.D3(TG, MT)

    resp.collect {
      when (it)  {
        is NetworkResult.Success -> {
          val locations = it.data
          LOG.D4(TG, "$MT: Got user locations: ${locations?.rows?.size}")
          processUserLocations(VMchat, locations, gmap)
        }
        is NetworkResult.Error -> {
          LOG.D3(TG, "$MT: Error: msg: ${it.message}")
          if (!err.handle(app, it.message, "loc-get")) {
            val msg = it.message ?: "unspecified error"
            utlErr.handlePollingRequest(app, scope, msg)
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
    val MT = ::processUserLocations.name

    LOG.D3(TG, MT)
    if (locations == null) return

    val LW = LevelWrapper(app.level.value!!, app.wSpace)
    val sameFloorUsers = locations.rows.filter { userLocation ->
      userLocation.buid == LW.wSpace.obj.buid &&  // same space
              userLocation.level == LW.obj.number.toInt() && // same deck
              userLocation.uid != smasUser.uid // not current user
    }

    val alertingUsers = locations.rows.filter { userLocation ->
      userLocation.alert == 1 &&
              userLocation.uid != smasUser.uid // not current user
    }

    val ownLocations = locations.rows.filter { it.uid == smasUser.uid }
    if (ownLocations.isNotEmpty()) {
      val ownLocation = ownLocations[0]

      // when the current user has not (ever) reported its own location,
      // then it might not be included in the locations DB
      checkForNewMsgs(VMchat, ownLocation.lastMsgTime)

      autoSetInitialLocation(ownLocation)
    }

    // pick the first alerting user
    if (alertingUsers.isNotEmpty()) {
      alertingUser.value = alertingUsers[0]
    } else {
     alertingUser.value = null
    }

    LOG.D3(TG, "$MT: : current floor: ${LW.prettyLevelName()}")
    // val dataset = MutableList<>(); // TODO: scalability?
    gmap.renderUserLocations(sameFloorUsers)

    if (DBG.D2) {
      sameFloorUsers.forEach {
        LOG.D4(TG, "$MT: User: ${it.uid} on floor: ${it.level}")
      }
    }
  }

  /**
   * When the user has no last location (i.e., app opened, and have not localized yet),
   * and has set the option to auto-update location,
   * and the last location was within 10 minutes, then auto assign to user that location
   */
  var triedAutosettingLocation = false
  private fun autoSetInitialLocation(ownLocation: UserLocation) {
    val MT = ::autoSetInitialLocation.name
    if (triedAutosettingLocation) return

    scope.launch(Dispatchers.IO) {
      val prefsCvMap = app.dsCvMap.read.first()
      if (prefsCvMap.autoSetInitialLocation && !app.hasLastLocation() ) {
        triedAutosettingLocation=true

        if (utlTime.isWithinMinutes(ownLocation.time, 10)) {
          app.locationSmas.update {
            val coord = ownLocation.toCoord()
            LOG.E(TG, "$MT RECENT LOC: ${coord.lon}, ${coord.lon}, LVL: ${coord.level}")
            LocalizationResult.Success(ownLocation.toCoord(), LocalizationResult.AUTOSET_RECENT)
          }
          notify.short(scope, "Restored last location.")
          delay(500)

          VM.ui.map.moveToLocation(ownLocation.toCoord().toLatLng())
        }
      }
    }
  }

  /**
   * Getting the locations includes [lastMsgsTs].
   * If it's newer than the previos one (ion [repo.local]), then we fetch the messages again
   */
  private fun checkForNewMsgs(VMchat: SmasChatViewModel, lastMsgTs: Long) {
    val MT = ::checkForNewMsgs.name
    val localTs = repo.local.getLastMsgTimestamp()
    LOG.V2(TG, "$MT: ${VM.hasNewMsgs(localTs, lastMsgTs)}")

    if (VM.hasNewMsgs(localTs, lastMsgTs)) {
      LOG.V2(TG, "$MT: ${VM.hasNewMsgs(localTs, lastMsgTs)}: pulling msgs..")
      VMchat.nwPullMessages()
    }
  }
}