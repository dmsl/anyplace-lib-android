package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.preference.PreferenceDataStore
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.net.NetUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Anyplace Backend settings:
 * - host url, port, etc.
 */
@Singleton // INFO cannot be ViewModelScoped, as it is used by NetworkModule
class AnyplaceDataStore @Inject constructor(@ApplicationContext private val ctx: Context)
  : PreferenceDataStore() {

  private val C by lazy { CONST(ctx) }
  private val Context.dataStoreServer by preferencesDataStore(name = C.PREF_SERVER)

  private val validKeys = setOf(
          C.PREF_SERVER_PROTOCOL,
          C.PREF_SERVER_HOST,
          C.PREF_SERVER_PORT)

  // these are not actual preferences. just placeholders to display some information
  // like backend version when displaying the connection status.
  private val ignoreKeys = setOf(C.PREF_SERVER_VERSION)

  private class Keys(c: CONST) {
    val protocol= stringPreferencesKey(c.PREF_SERVER_PROTOCOL)
    val host = stringPreferencesKey(c.PREF_SERVER_HOST)
    val port = stringPreferencesKey(c.PREF_SERVER_PORT)
  }
  private val KEY = Keys(C)
  val datastore = ctx.dataStoreServer
  private fun ignoreKey(key: String?) = ignoreKeys.contains(key)

  private fun validKey(key: String?): Boolean {
    if (ignoreKey(key)) return false
    val found = validKeys.contains(key)
    if(!found) LOG.W(TAG, "Unknown key: $key")

    return found
  }

  override fun putString(key: String?, value: String?) {
    if (!validKey(key)) return
    runBlocking {
      datastore.edit {
        when (key) {
          C.PREF_SERVER_HOST -> it[KEY.host] = value?: C.DEFAULT_PREF_SERVER_HOST
          C.PREF_SERVER_PORT ->  {
            val storeValue : String =
              if (NetUtils.isValidPort(value)) value!! else C.DEFAULT_PREF_SERVER_PORT
            it[KEY.port] =  storeValue
          }
          C.PREF_SERVER_PROTOCOL -> {
            if(NetUtils.isValidProtocol(value))
              it[KEY.protocol] = value?: C.DEFAULT_PREF_SERVER_PROTOCOL
          }
        }
      }
    }
  }

  override fun getString(key: String?, defValue: String?): String? {
    if (!validKey(key)) return null
    return runBlocking(Dispatchers.IO) {
      val prefs = read.first()
      return@runBlocking when (key) {
        C.PREF_SERVER_HOST -> prefs.host
        C.PREF_SERVER_PORT -> prefs.port
        C.PREF_SERVER_PROTOCOL -> prefs.protocol
        else -> null
      }
    }
  }

  override fun putBoolean(key: String?, value: Boolean) { }
  override fun getBoolean(key: String?, defValue: Boolean): Boolean { return false }

  val read: Flow<ServerPrefs> = ctx.dataStoreServer.data
      .catch { exception ->
        if (exception is IOException) {
          emit(emptyPreferences())
        } else { throw exception }
      }
      .map { preferences ->
        val protocol = preferences[KEY.protocol] ?: C.DEFAULT_PREF_SERVER_PROTOCOL
        val host = preferences[KEY.host] ?: C.DEFAULT_PREF_SERVER_HOST
        val port = preferences[KEY.port] ?: C.DEFAULT_PREF_SERVER_PORT
        ServerPrefs(protocol, host, port)
      }
}

data class ServerPrefs(
  val protocol: String,
  val host: String,
  val port: String,
  // TODO timeout and other settings?
)
