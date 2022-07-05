package cy.ac.ucy.cs.anyplace.lib.android.consts.smas

import android.content.Context
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST

open class SMAS(ctx: Context) : CONST(ctx) {

  //// ROOM
  companion object {
    const val DB_MSGS= "messages"
    const val DB_CVMODELS= "FINGERPRINT_OBJECT"
    const val DB_FINGERPRINT= "FINGERPRINT"
    const val DB_LOCALIZE_TEMP= "FINGERPRINT_LOCALIZE_TEMP"
    const val DB_OBJECT_FREQUENCY= "OBJECT_FREQUENCY"
  }

  val DB_SMAS_NAME = "smas_db"

  // PREFERENCES
  //// CHAT SERVER
  val PREF_CHAT_SERVER = ctx.getString(R.string.pref_chat_server)
  val PREF_CHAT_SERVER_PROTOCOL = ctx.getString(R.string.pref_chat_server_protocol)
  val PREF_CHAT_SERVER_HOST = ctx.getString(R.string.pref_chat_server_host)
  val PREF_CHAT_SERVER_PATH= ctx.getString(R.string.pref_chat_server_path)
  val PREF_CHAT_SERVER_PORT = ctx.getString(R.string.pref_chat_server_port)
  val PREF_CHAT_SERVER_VERSION = ctx.getString(R.string.pref_chat_server_version)
  // CHAT SETTINGS
  /** from [ChatMsgs.mdelivery] */
  val PREF_CHAT_MDELIVERY= ctx.getString(R.string.pref_chat_mdelivery)
  val PREF_CHAT_USER= ctx.getString(R.string.pref_chat_user)

  val FLAG_CHAT_NEWMSGS= ctx.getString(R.string.flag_chat_new_msgs)

  ////// CHAT SERVER: DEFAULTS
  val DEFAULT_PREF_CHAT_SERVER_PROTOCOL = ctx.getString(R.string.default_pref_smas_protocol)
  val DEFAULT_PREF_CHAT_SERVER_HOST = ctx.getString(R.string.default_pref_smas_host)
  val DEFAULT_PREF_CHAT_SERVER_PATH = ctx.getString(R.string.default_pref_smas_path)
  val DEFAULT_PREF_CHAT_SERVER_PORT = ctx.getString(R.string.default_pref_smas_port)

  val DEFAULT_PREF_CHAT_MDELIVERY = ctx.getString(R.string.default_pref_smas_chat_mdelivery)
}