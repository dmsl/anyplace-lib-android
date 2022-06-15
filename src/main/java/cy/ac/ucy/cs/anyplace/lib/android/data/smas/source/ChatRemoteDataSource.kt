package cy.ac.ucy.cs.anyplace.lib.android.data.smas.source

import cy.ac.ucy.cs.anyplace.lib.anyplace.models.CvLocalizationReq
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.CvLocalizationResp
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.FingerprintSendReq
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.FingerprintSendResp
import cy.ac.ucy.cs.anyplace.lib.smas.ChatUserAuth
import cy.ac.ucy.cs.anyplace.lib.smas.models.*
import cy.ac.ucy.cs.anyplace.lib.smas.models.CONSTchatMsg.TP_GET_FROM
import retrofit2.Response
import javax.inject.Inject

/**
 * Chat DataSource
 */
class ChatRemoteDataSource @Inject constructor(private val RH: RetrofitHolderSmas) {

  // MISC
  suspend fun getVersion(): Response<ChatVersion>  = RH.api.version()

  // USER
  suspend fun userLogin(r: ChatLoginReq) : Response<ChatLoginResp> = RH.api.login(r)

  // LOCATION
  /** Get locations of all other users */
  suspend fun locationGet(r: ChatUserAuth) : Response<UserLocations> = RH.api.locationGet(r)
  /** Send location of current user */
  suspend fun locationSend(r: LocationSendReq) : Response<LocationSendResp> = RH.api.locationSend(r)

  // CHAT
  /** Get all Chat Messages */
  suspend fun messagesGet(r: MsgGetReq) : Response<ChatMsgsResp> = RH.api.messagesGet(r)

  /** Get all Chat Messages that have arrived after the timestamp */
  suspend fun messagesGetFrom(r: MsgGetReq, timestamp: Long) : Response<ChatMsgsResp> {
    val from=(timestamp+1).toString()  // +1 to make timestamp exclusive
    val req = MsgGetReq(r.uid, r.sessionkey, TP_GET_FROM, from)
    return RH.api.messagesGet(req)
  }
  suspend fun messagesSend(r: MsgSendReq) : Response<MsgSendResp> = RH.api.messageSend(r)

  // TODO fingerprint-send.php

  suspend fun cvModelsGet(r: ChatUserAuth) : Response<CvModelsResp> = RH.api.cvModelsGet(r)
  suspend fun cvFingerprintSend(r: FingerprintSendReq) : Response<FingerprintSendResp>
          = RH.api.cvFingerprintSend(r)

  suspend fun cvLocalization(r: CvLocalizationReq) : Response<CvLocalizationResp>
          = RH.api.cvLocalization(r)

}