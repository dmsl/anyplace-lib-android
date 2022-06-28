package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.preference.PreferenceDataStore
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Computer Vision DataStore
 * TODO SETTINGS:
 * - image
 */
@Singleton
class CvLoggerDataStore @Inject constructor(@ApplicationContext private val ctx: Context)
  : PreferenceDataStore() {

  private val C by lazy { CONST(ctx) }
  private val Context.dataStoreCvLogger by preferencesDataStore(name = C.PREF_CVLOG)
  val datastore = ctx.dataStoreCvLogger

  private val validKeys = setOf(
          C.PREF_CVLOG_WINDOW_LOGGING_SECONDS,
          // C.PREF_CV_WINDOW_LOCALIZATION_MS,
          C.PREF_CV_DEV_MODE,
  )

  private class Keys(c: CONST) {
    val windowLoggingSeconds = stringPreferencesKey(c.PREF_CVLOG_WINDOW_LOGGING_SECONDS)
    // val windowLocalizationSeconds = stringPreferencesKey(c.PREF_CV_WINDOW_LOCALIZATION_MS)
    val devMode = booleanPreferencesKey(c.PREF_CV_DEV_MODE)
  }
  private val KEY = Keys(C)

  private fun validKey(key: String?): Boolean {
    val found = validKeys.contains(key)
    if(!found) LOG.W(TAG, "Unknown key: $key")
    return found
  }

  override fun putBoolean(key: String?, value: Boolean) {
    if (!validKey(key)) return
    runBlocking {
      datastore.edit {
        LOG.E(TAG, "put boolean: $key:$value" )
        when (key) {
          C.PREF_CV_DEV_MODE -> it[KEY.devMode] = value
        }
      }
    }
  }

  /**
   * WORKAROUND: SharedPreferences/XML use by default string.
   * Here it is convert ot an [Int] and stored in the [DataStore].
   */
  override fun putString(key: String?, value: String?) {
    LOG.D3(TAG, "putString: $key = $value")
    if (!validKey(key)) return
    runBlocking {
      datastore.edit {
        when (key) {
          C.PREF_CVLOG_WINDOW_LOGGING_SECONDS -> it[KEY.windowLoggingSeconds] =
                  value ?: C.DEFAULT_PREF_CVLOG_WINDOW_LOGGING_SECONDS
          // C.PREF_CV_WINDOW_LOCALIZATION_MS-> it[KEY.windowLocalizationSeconds] =
          //         value ?: C.DEFAULT_PREF_CVLOG_WINDOW_LOCALIZATION_MS
        }
      }
    }
  }

  override fun getBoolean(key: String?, defValue: Boolean): Boolean {
    if (!validKey(key)) return false
    return runBlocking(Dispatchers.IO) {
      val prefs = read.first()
      return@runBlocking when (key) {
        C.PREF_CV_DEV_MODE -> prefs.devMode
        else -> false
      }
    }
  }

  override fun getString(key: String?, defValue: String?): String? {
    if (!validKey(key)) return null
    return runBlocking(Dispatchers.IO) {
      val prefs = read.first()
      return@runBlocking when (key) {
        C.PREF_CVLOG_WINDOW_LOGGING_SECONDS-> prefs.windowLoggingSec
        // C.PREF_CV_WINDOW_LOCALIZATION_MS-> prefs.windowLocalizationMs
        else -> null
      }
    }
  }

  val read: Flow<CvLoggerPrefs> = ctx.dataStoreCvLogger.data
          .catch { exception ->
            if (exception is IOException) {
              emit(emptyPreferences())
            } else { throw exception }
          }
          .map { preferences ->
            val windowLoggingSeconds = preferences[KEY.windowLoggingSeconds] ?:
            C.DEFAULT_PREF_CVLOG_WINDOW_LOGGING_SECONDS
            // val windowLocalizationMs = preferences[KEY.windowLocalizationSeconds] ?:
            // C.DEFAULT_PREF_CVLOG_WINDOW_LOCALIZATION_MS
            val devMode = preferences[KEY.devMode] ?: C.DEFAULT_PREF_CV_DEV_MODE

            val prefs = CvLoggerPrefs(windowLoggingSeconds, devMode)
            LOG.D2(TAG, "read prefs: $prefs")

            prefs
          }
}

data class CvLoggerPrefs(
        val windowLoggingSec: String,
        /** We keep a duplicate option here (as well in [CvNavigationPrefs])
         * because the XML [SettingsCvLoggerActivity] has to be bound to this DS
         *
         * See in [SettingsCvLoggerActivity]: preferenceManager.preferenceDataStore = dsCvLog
         * */
        // val windowLocalizationMs: String,
        val devMode: Boolean,
)
