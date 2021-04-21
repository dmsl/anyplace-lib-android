package cy.ac.ucy.cs.anyplace.lib.android.utils

import android.content.Context
import android.content.SharedPreferences
import cy.ac.ucy.cs.anyplace.lib.android.consts.DEFAULT

/**
 * TODO: also these: Anyplace_Preferences (UnifiedNav/ Navigator)
 */
class Preferences(private val ctx: Context) {
  companion object {
    const val SERVER_IP = "server_ip_address"
    const val SERVER_PORT = "server_port"
  }

  // CHECK:PM where is this stored? why not merged in lib?
  // The things accessed below are AP generic (not logger specific).
  // logger specific settings are: number of measurements, step size, new cv settings, etc
  private val PREFS_LOGGER = "LoggerPreferences"  // TODO:PM this is logger prefs. MUST change this!
  private var pref: SharedPreferences = ctx.getSharedPreferences(PREFS_LOGGER, Context.MODE_PRIVATE)

  val ip: String? get() { return pref.getString(SERVER_IP, DEFAULT.IP) }
  val port: String? get() { return pref.getString(SERVER_PORT, DEFAULT.PORT) }
  val cacheDir: String get() { return ctx.filesDir.absolutePath + "/app" } // TODO:PM rename
  val cacheJsonDir: String get() { return "$cacheDir/json"
  }
}