package cy.ac.ucy.cs.anyplace.lib.android.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.preference.PreferenceDataStore
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.DEFAULT_PREF_SERVER_HOST
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.DEFAULT_PREF_SERVER_PORT
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.DEFAULT_PREF_SERVER_PROTOCOL
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_SERVER
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_SERVER_HOST
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_SERVER_PORT
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_SERVER_PROTOCOL
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_SERVER_VERSION
import cy.ac.ucy.cs.anyplace.lib.android.utils.network.NetUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStoreServer by preferencesDataStore(name = PREF_SERVER)

/**
 * Backend Server settings, such as host url, port, etc.
 * TODO:PM update through UI !!
 * Is there an automatic way?
 */
// @ViewModelScoped
@Singleton // INFO cannot be ViewModelScoped, as it is used by NetworkModule
class DataStoreServer @Inject constructor(@ApplicationContext private val ctx: Context)
  : PreferenceDataStore() {
  private val TAG = DataStoreServer::class.java.simpleName

  private val validKeys = setOf(
    PREF_SERVER_PROTOCOL,
    PREF_SERVER_HOST,
    PREF_SERVER_PORT)

  // these are not actual preferences. just placeholders to display some information
  // like backend version when displaying the connection status.
  private val ignoreKeys = setOf(PREF_SERVER_VERSION)

  private object KEY {
    val protocol= stringPreferencesKey(PREF_SERVER_PROTOCOL)
    val host = stringPreferencesKey(PREF_SERVER_HOST)
    val port = stringPreferencesKey(PREF_SERVER_PORT)
  }

  val datastore = ctx.dataStoreServer

  private fun ignoreKey(key: String?): Boolean {
    return  ignoreKeys.contains(key)
  }

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
          PREF_SERVER_HOST -> it[KEY.host] = value?: DEFAULT_PREF_SERVER_HOST
          PREF_SERVER_PORT ->  {
            val storeValue : String =
            if (NetUtils.isValidPort(value)) value!! else DEFAULT_PREF_SERVER_PORT
            it[KEY.port] =  storeValue
          }
          PREF_SERVER_PROTOCOL-> {
            if(NetUtils.isValidProtocol(value))
              it[KEY.protocol] = value?: DEFAULT_PREF_SERVER_PROTOCOL
          }
        }
      }
    }
  }

  override fun getString(key: String?, defValue: String?): String? {
    if (!validKey(key)) return null

    return runBlocking(Dispatchers.IO) {
      val prefs = readServerPrefs.first()
      return@runBlocking when (key) {
        PREF_SERVER_HOST -> prefs.host
        PREF_SERVER_PORT -> prefs.port
        PREF_SERVER_PROTOCOL -> prefs.protocol
        else -> null
      }
    }
  }

  override fun putBoolean(key: String?, value: Boolean) {
    LOG.W(TAG, "TODO: putBoolean")
  }

  override fun getBoolean(key: String?, defValue: Boolean): Boolean {
    LOG.W(TAG, "TODO: getBoolean")
    return false // TODO
  }

  val readServerPrefs: Flow<ServerPrefs> = ctx.dataStoreServer.data
      .catch { exception ->
        if (exception is IOException) {
          emit(emptyPreferences())
        } else { throw exception }
      }
      .map { preferences ->
        val protocol = preferences[KEY.protocol] ?: DEFAULT_PREF_SERVER_PROTOCOL
        val host = preferences[KEY.host] ?: DEFAULT_PREF_SERVER_HOST
        val port = preferences[KEY.port] ?: DEFAULT_PREF_SERVER_PORT

        ServerPrefs(protocol, host, port)
      }
}

data class ServerPrefs(
  val protocol: String,
  val host: String,
  val port: String,
  // TODO timeout? other?
)
