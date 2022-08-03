package cy.ac.ucy.cs.anyplace.lib.android.consts.smas

import android.content.Context
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST

open class SMAS(ctx: Context) : CONST(ctx) {

  companion object {
    const val DB_MSGS= "messages"
    const val DB_OBJECT= "OBJECT" // WAS FINGERPRINT_OBJECT.. CLR:PM
    const val DB_FINGERPRINT= "FINGERPRINT"
    const val DB_LOCALIZE_TEMP= "FINGERPRINT_LOCALIZE_TEMP"
    const val DB_OBJECT_FREQUENCY= "OBJECT_FREQUENCY"
  }

  val DB_SMAS_NAME = "smas_db"

  // PREFERENCES
  //// CHAT SERVER
  val PREF_SMAS_SERVER = ctx.getString(R.string.pref_smas_server)
  val PREF_SMAS_SERVER_PROTOCOL = ctx.getString(R.string.pref_smas_server_protocol)
  val PREF_SMAS_SERVER_HOST = ctx.getString(R.string.pref_smas_server_host)
  val PREF_SMAS_SERVER_PATH= ctx.getString(R.string.pref_smas_server_path)
  val PREF_SMAS_SERVER_PORT = ctx.getString(R.string.pref_smas_server_port)
  val PREF_SMAS_SERVER_VERSION = ctx.getString(R.string.pref_smas_server_version)
  // SMAS SETTINGS
  /** from [ChatMsgs.mdelivery] */
  val PREF_SMAS_MDELIVERY= ctx.getString(R.string.pref_smas_chat_mdelivery)
  val PREF_SMAS_USER= ctx.getString(R.string.pref_smas_user)

  val FLAG_SMAS_NEWMSGS= ctx.getString(R.string.flag_smas_new_msgs)

  ////// CHAT SERVER: DEFAULTS
  val DEFREF_SMAS_SERVER_PROTOCOL = ctx.getString(R.string.default_pref_smas_protocol)
  val DEFPREF_SMAS_SERVER_HOST = ctx.getString(R.string.default_pref_smas_host)
  val DEFPREF_SMAS_SERVER_PATH = ctx.getString(R.string.default_pref_smas_path)
  val DEFPREF_SMAS_SERVER_PORT = ctx.getString(R.string.default_pref_smas_port)
  val DEFPREF_SMAS_CHAT_MDELIVERY = ctx.getString(R.string.default_pref_smas_chat_mdelivery)
}