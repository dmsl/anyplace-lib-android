package cy.ac.ucy.cs.anyplace.lib.android.utils

import android.content.Context
import android.content.SharedPreferences
import cy.ac.ucy.cs.anyplace.lib.android.consts.DEFAULT

/**
 * also deprecate these: Anyplace_Preferences (UnifiedNav/ Navigator)
 */
@Deprecated("")
class Preferences(private val ctx: Context) {
  companion object {
    const val SERVER_IP = "server_ip_address"
    const val SERVER_PORT = "server_port"
    const val ACCESS_TOKEN= "server_access_token"
  }

  // CHECK:PM where is this stored? why not merged in lib?
  // The things accessed below are AP generic (not logger specific).
  // logger specific settings are: number of measurements, step size, new cv settings, etc
  private val PREFS_LOGGER = "LoggerPreferences"  // This is logger prefs. Must change this!
  private var pref: SharedPreferences = ctx.getSharedPreferences(PREFS_LOGGER, Context.MODE_PRIVATE)

  val ip: String? get() { return pref.getString(SERVER_IP, DEFAULT.IP) }
  val port: String? get() { return pref.getString(SERVER_PORT, DEFAULT.PORT) }
  val access_token: String? get() { return pref.getString(ACCESS_TOKEN, DEFAULT.ACCESS_TOKEN) }

  val cacheDir: String get() { return ctx.filesDir.absolutePath + "/app" } // rename?
  val cacheJsonDir: String get() { return "$cacheDir/json" }
  val radiomapsDir: String get() { return "$cacheDir/radiomaps" }
  val floorplansDir: String get() { return "$cacheDir/floorplans" }
}