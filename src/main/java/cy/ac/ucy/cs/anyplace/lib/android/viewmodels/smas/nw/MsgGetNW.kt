package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.nw

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.NavigatorAppBase
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.SMAS
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.ConverterDB.Companion.entityToChatMessages
import cy.ac.ucy.cs.anyplace.lib.smas.models.*
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.wrappers.ChatMsgWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.di.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.Response
import java.lang.Exception
import java.net.ConnectException

/**
 * Fetches the chat messages
 */
class MsgGetNW(
  private val app: NavigatorAppBase,
  private val VM: SmasChatViewModel,
  private val RH: RetrofitHolderSmas,
  private val repo: RepoSmas) {
  private val TG = "nw-msg-get"

  /**
   * Network Responses from API calls
   */
  val resp: MutableStateFlow<NetworkResult<ChatMsgsResp>> = MutableStateFlow(NetworkResult.Unset())

  private val C by lazy { SMAS(app.applicationContext) }
  private val err by lazy { SmasErrors(app, VM.viewModelScope) }
  private lateinit var smasUser: SmasUser

  /** Show warning only once */
  var warnNoInternet = false
  var skipCall = false

  /**
   * Get [ChatMsg] SafeCall
   */
  suspend fun safeCall(showToast: Boolean = false) {
    val MT = ::safeCall.name
    LOG.D2(TG, MT)
    LOG.D2(TG, "msg-get: size: ${app.msgList.size}")

    if (resp.value is NetworkResult.Loading) {
      LOG.W(TG, "MsgsGet: already in progress (skipped)")
      return
    } else if (skipCall) {
      LOG.W(TG, "MsgsGet: forced skip (unsetting)")
      resp.value = NetworkResult.Unset()
      return
    }

    resp.value = NetworkResult.Loading()
    smasUser = app.dsUserSmas.read.first()

    if (app.hasInternet()) {
      try {
        val lastTimestamp = repo.local.getLastMsgTimestamp()
        var incrementalFetch = false
        val response : Response<ChatMsgsResp>
        if (lastTimestamp==null) {
          LOG.W(TG, "Will fetch ALL messages")
          response = repo.remote.messagesGet(MsgGetReq(smasUser))
        } else {
          incrementalFetch=true
          response = repo.remote.messagesGetFrom(MsgGetReq(smasUser), lastTimestamp)
        }

        LOG.D2(TAG, "Messages: ${response.message()}")
        resp.value = handleResponse(response, incrementalFetch, showToast)

        // Persist msgs in local store
        val msgs = resp.value.data
        // no new msgs fetched. instead the prev msgs were were loaded from localDB
        val dbLoaded = resp.value.message.toString() == NetworkResult.DB_LOADED
        if (msgs != null && msgs.msgs.isNotEmpty() && !dbLoaded) {
          LOG.D(TAG, "Persisting new msgs to db")
          persistToDB(msgs)
        }

      } catch (ce: ConnectException) {
        val msg = "Connection failed:\n${RH.retrofit.baseUrl()}"
        handleException(msg, ce)
      } catch (e: Exception) {
        val msg = TG
        handleException(msg, e)
      }
    } else { // offline-mode
      val msg="${C.ERR_MSG_NO_INTERNET} (message fetch)"
      LOG.D(TAG_METHOD, msg)
      if (!warnNoInternet) {
        warnNoInternet=true
        app.showToast(VM.viewModelScope, msg, Toast.LENGTH_LONG)
      }

      resp.value = getMsgsFromDB()
    }
  }

  /**
   * Reads messages from Room (SQLite)
   */
  private suspend fun getMsgsFromDB(): NetworkResult<ChatMsgsResp> {
    LOG.D2(TG, "reading msgs from cache")
    val localMsgs = repo.local.readMsgs().first()

    return if (localMsgs.isNotEmpty()) {
      NetworkResult.Success(entityToChatMessages(localMsgs), NetworkResult.DB_LOADED)
    } else {
      NetworkResult.Error(C.ERR_MSG_NO_INTERNET)
    }
  }

  /**
   * - [incrementalFetch]: load local msgs from DB also
   */
  private suspend fun handleResponse(
          response: Response<ChatMsgsResp>,
          incrementalFetch: Boolean,
          showToast: Boolean): NetworkResult<ChatMsgsResp> {
    val MT = ::handleResponse.name
    LOG.D3(TG, MT)
    if (response.isSuccessful) {
      when {
        response.message().toString().contains("timeout") -> return NetworkResult.Error("Timeout.")
        // response.body()!!.chatMsgs.isNullOrEmpty() -> return NetworkResult.Error("Can't get messages.")
        response.isSuccessful -> {

          LOG.V2(TG, "MSG-GET: Success")

          // SMAS special handling (errors should not be 200/OK)
          val r = response.body()!!
          if (r.status == "err") {
            return NetworkResult.Error(r.descr)
          }

          val noNewMsgsFetched=response.body()!!.msgs.isEmpty()
          if (noNewMsgsFetched) {
            if (incrementalFetch) {
              return if (app.msgList.isEmpty()) {
                // the app was opened, w/o new msgs. the [app.msgList] was empty
                // all we have to do is to load the previous msgs from DB
                LOG.D2(TG,"Reading from DB. (empty msgList + no new msgs)")

                getMsgsFromDB()
              } else {
                // no new msgs fetched. [app.msgList] is populated: no work needed!
                LOG.D2(TG, "No new msgs. Prev msgList: ${app.msgList.size}")
                NetworkResult.Success(response.body()!!, NetworkResult.UP_TO_DATE)
              }
            }

            val msg = "No new messages."
            if (showToast) {
              LOG.D2(TG, msg)
            }

            // empty messages
            LOG.W(TG, msg)
            return NetworkResult.Success(response.body()!!)
          }

          // else: some msgs fetched
          var resp  = response.body()!!
          var loadType: String? = null
          if (incrementalFetch)  {
            val localMsgs = if (app.msgList.isEmpty()) {
              val oldMsgs = getMsgsFromDB().data!!.msgs
              LOG.E(TG, "Old msgs: ${oldMsgs.size}")
              oldMsgs
            } else emptyList()

            // persist to DB the new msgs
            val newMsgs = resp.msgs // remote messages
            persistToDB(resp) // persist after we use [getMsgsFromDB] (avoid dups)

            // localMsgs are descending: newest msgs is first
            val mergedMsgs = localMsgs + newMsgs.reversed()

            val merged=ChatMsgsResp(resp.status, resp.descr, resp.uid, mergedMsgs)

            resp=merged
            loadType=NetworkResult.DB_LOADED
          } else {
            // workaround: reverse msgs for LazyColumn
            resp=ChatMsgsResp(resp.status, resp.descr, resp.uid, resp.msgs.reversed())
          }

          // This is actually hybrid:
          // - we load new msgs from remote. we persist those async in DB.
          // - we merge newMsgs w/ local msgs
          // - and return the result
          return NetworkResult.Success(resp, loadType)

        } // can be nullable
        else -> return NetworkResult.Error(response.message())
      }
    }
    return NetworkResult.Error("$TAG: ${response.message()}")
  }

  private fun handleException(msg: String, e: Exception) {
    val MT = ::handleException.name
    val details = "$MT: $msg:${e.message}"
    LOG.E(TG, details)
    resp.value = NetworkResult.Error(details)
  }

  suspend fun collect(ctx: Context) {
    resp.collect {
      when (it) {
        is NetworkResult.Success -> {
          when (it.message) {
            NetworkResult.UP_TO_DATE -> {
              LOG.W(TG, "Messages: up-to-date. Size: ${app.msgList.size} (skip processing)")
            }
            NetworkResult.DB_LOADED,
            null -> {
              val msgs = it.data!!.msgs
              LOG.V2(TG, "MSGS: collect: new: ${msgs.size}. old: ${app.msgList.size}. (processing)")
              appendMessages(app, VM, msgs)
            }
          }

        }
        is NetworkResult.Error ->  {
          //db error
          if (!err.handle(app, it.message, "msg-get")) {
            val msg = it.message ?: "unspecified error"
            app.showToast(VM.viewModelScope, msg, Toast.LENGTH_SHORT)
            LOG.E(TG, msg)
            // LOG.E(TAG, "$msg: from MsgGetNW Collect. class: ${it::class.simpleName}")
          }
        }
        else -> {}
      }
    }
  }

  // TODO:PM
  // 1. make refresh for whole chat: messages, locations, etc.
  // 2. messages should be fetched: in mid delay:
  //// e.g. after 2 secs: locations: after 1 sec msgs
  // Separate alerts from others?
  private fun appendMessages(app: NavigatorAppBase, VM: SmasChatViewModel, msgs: List<ChatMsg>) {
    val MT = ::appendMessages.name
    LOG.D2(TG,"$MT: New: ${msgs.size} Old: ${app.msgList.size}")
    msgs.forEach { obj ->
      val msgH = ChatMsgWrapper(app, repo, obj)
      val contents = msgH.content()

      // cache images
      if (obj.mtype == 2) VM.chatCache.saveImg(obj)

      app.msgList.add(0, obj)   // add to the beginning
      val prettyTimestamp = utlTime.getPrettyEpoch(obj.time, utlTime.TIMEZONE_CY)
      LOG.V3(TG, "MSG |$prettyTimestamp| ${msgH.prettyTypeCapitalize.format(6)} | $contents  || [${obj.time}][${obj.timestr}]")
      LOG.V4(TG, "MsgList: updated size: ${app.msgList.size}")
    }

    // clear the response
    resp.value = NetworkResult.Unset()
  }

  // ROOM
  /**
   * Persists [ChatMsg]s to SQLite (through ROOM).
   * It does not store images (base64) to DB.
   * Those will be stored in [SmasCache] (file cache)
   */
  private fun persistToDB(msgs: ChatMsgsResp) {
    val MT = ::persistToDB.name
    LOG.D2(TG, "$MT: storing: ${msgs.msgs.size} msgs")
    VM.viewModelScope.launch(Dispatchers.IO) {

      VM.savedNewMsgs(true) // new messages were saved

      // repo.local.dropMsgs() // TODO: don't drop msgs first..
      msgs.msgs.forEach { msg ->
        repo.local.insertMsg(msg)
      }
    }
  }
}

