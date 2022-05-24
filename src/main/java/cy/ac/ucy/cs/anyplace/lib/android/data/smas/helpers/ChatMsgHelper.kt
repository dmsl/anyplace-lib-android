package cy.ac.ucy.cs.anyplace.lib.android.data.smas.helpers

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import cy.ac.ucy.cs.anyplace.lib.android.cache.anyplace.Cache
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.smas.models.CONSTchatMsg.STP_IMG
import cy.ac.ucy.cs.anyplace.lib.smas.models.CONSTchatMsg.STP_LOCATION
import cy.ac.ucy.cs.anyplace.lib.smas.models.CONSTchatMsg.STP_TP4
import cy.ac.ucy.cs.anyplace.lib.smas.models.CONSTchatMsg.STP_TXT
import cy.ac.ucy.cs.anyplace.lib.smas.models.CONSTchatMsg.TP_SEND_4
import cy.ac.ucy.cs.anyplace.lib.smas.models.CONSTchatMsg.TP_SEND_IMG
import cy.ac.ucy.cs.anyplace.lib.smas.models.CONSTchatMsg.TP_SEND_LOCATION
import cy.ac.ucy.cs.anyplace.lib.smas.models.CONSTchatMsg.TP_SEND_TXT
import cy.ac.ucy.cs.anyplace.lib.smas.models.ChatMsg

/**
 * Extra functionality on top of the [ChatMsg] data class.
 * TODO: rename to data the encapsualting class of all helpers
 */
class ChatMsgHelper(val ctx: Context,
                    val repo: RepoSmas,
                    val obj: ChatMsg) {

  override fun toString(): String = Gson().toJson(obj, ChatMsg::class.java)

  companion object {

    fun parse(str: String): ChatMsg = Gson().fromJson(str, ChatMsg::class.java)

    fun isImage(tp: Int) = tp == TP_SEND_IMG
    fun isText(tp: Int) = tp == TP_SEND_TXT
    @Deprecated("alerts are sent differently")
    fun isAlert(tp: Int) = tp == TP_SEND_LOCATION

    fun content(obj: ChatMsg) : String {
      return when {
        isImage(obj.mtype) -> "<base64>"
        else -> obj.msg.toString()
      }
    }
  }

  private val cache by lazy { Cache(ctx) }

  val prettyType: String
    get() {
      return when (obj.mtype) {
        TP_SEND_TXT -> STP_TXT
        TP_SEND_IMG -> STP_IMG
        TP_SEND_LOCATION ->  STP_LOCATION
        TP_SEND_4 -> STP_TP4
        else -> "UnknownType"
      }
    }

  val prettyTypeCapitalize: String
    get() { return prettyType.replaceFirstChar(Char::uppercase) }

  fun isText() : Boolean = isText(obj.mtype)
  fun isAlert() : Boolean = isAlert(obj.mtype)
  fun isImage() : Boolean = isImage(obj.mtype)

  fun content() = content(obj)

  fun latLng() : LatLng {
    val lat = obj.x
    val lon = obj.y
    return LatLng(lat, lon)
  }

}
