package cy.ac.ucy.cs.anyplace.lib.android.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.preference.PreferenceDataStore
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.DEFAULT_PREF_CVLOG_DEV_MODE
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.DEFAULT_PREF_CVLOG_EPX_IMG_PADDING
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.DEFAULT_PREF_CVLOG_WINDOW_LOCALIZATION_SECONDS
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.DEFAULT_PREF_CVLOG_WINDOW_LOGGING_SECONDS
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_CVLOG
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_CVLOG_EPX_IMG_PADDING
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_CVLOG_DEV_MODE
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_CVLOG_WINDOW_LOCALIZATION_SECONDS
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_CVLOG_WINDOW_LOGGING_SECONDS
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStoreCvLogger by preferencesDataStore(name = PREF_CVLOG)

/**
 * Backend Server settings, such as host url, port, etc.
 */
@Singleton
class DataStoreCvLogger @Inject constructor(@ApplicationContext private val ctx: Context)
  : PreferenceDataStore() {

  private val validKeys = setOf(
          PREF_CVLOG_WINDOW_LOGGING_SECONDS,
          PREF_CVLOG_WINDOW_LOCALIZATION_SECONDS,
          PREF_CVLOG_DEV_MODE,
          PREF_CVLOG_EPX_IMG_PADDING,
  )

  private object KEY {
    val windowLoggingSeconds = stringPreferencesKey(PREF_CVLOG_WINDOW_LOGGING_SECONDS)
    val windowLocalizationSeconds = stringPreferencesKey(PREF_CVLOG_WINDOW_LOGGING_SECONDS)
    val devMode = booleanPreferencesKey(PREF_CVLOG_DEV_MODE)
    val expImagePadding = booleanPreferencesKey(PREF_CVLOG_EPX_IMG_PADDING)
  }

  val datastore = ctx.dataStoreCvLogger

  private fun validKey(key: String?): Boolean {
    val found = validKeys.contains(key)
    if(!found) LOG.W(TAG, "Unknown key: $key")
    return found
  }

  override fun putBoolean(key: String?, value: Boolean) {
    if (!validKey(key)) return
    runBlocking {
      datastore.edit {
        when (key) {
          PREF_CVLOG_DEV_MODE -> it[KEY.devMode] = value
          PREF_CVLOG_EPX_IMG_PADDING-> it[KEY.expImagePadding] = value
        }
      }
    }
  }

  override fun putString(key: String?, value: String?) {
    LOG.D3(TAG, "putString: $key = $value")
    if (!validKey(key)) return
    runBlocking {
      datastore.edit {
        when (key) {
          PREF_CVLOG_WINDOW_LOGGING_SECONDS -> it[KEY.windowLoggingSeconds] = value ?:
                  DEFAULT_PREF_CVLOG_WINDOW_LOGGING_SECONDS
          PREF_CVLOG_WINDOW_LOCALIZATION_SECONDS-> it[KEY.windowLocalizationSeconds] = value ?:
                  DEFAULT_PREF_CVLOG_WINDOW_LOCALIZATION_SECONDS
        }
      }
    }
  }

  override fun getBoolean(key: String?, defValue: Boolean): Boolean {
    if (!validKey(key)) return false
    return runBlocking(Dispatchers.IO) {
      val prefs = read.first()
      return@runBlocking when (key) {
        PREF_CVLOG_DEV_MODE -> prefs.devMode
        PREF_CVLOG_EPX_IMG_PADDING -> prefs.expImagePadding
        else -> false
      }
    }
  }

  override fun getString(key: String?, defValue: String?): String? {
    if (!validKey(key)) return null
    return runBlocking(Dispatchers.IO) {
      val prefs = read.first()
      return@runBlocking when (key) {
        PREF_CVLOG_WINDOW_LOGGING_SECONDS-> prefs.windowLoggingSeconds
        PREF_CVLOG_WINDOW_LOCALIZATION_SECONDS-> prefs.windowLocalizationSeconds
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
            var windowLoggingSeconds = preferences[KEY.windowLoggingSeconds] ?: DEFAULT_PREF_CVLOG_WINDOW_LOGGING_SECONDS
            var windowLocalizationSeconds = preferences[KEY.windowLocalizationSeconds] ?: DEFAULT_PREF_CVLOG_WINDOW_LOCALIZATION_SECONDS
            val devMode = preferences[KEY.devMode] ?: DEFAULT_PREF_CVLOG_DEV_MODE // TODO:PM make default to true
            val expImagePadding= preferences[KEY.expImagePadding] ?: DEFAULT_PREF_CVLOG_EPX_IMG_PADDING

            // CHECK: are the below needed? (given the handling above..)
            windowLoggingSeconds.toIntOrNull() ?: run {
              windowLoggingSeconds = DEFAULT_PREF_CVLOG_WINDOW_LOGGING_SECONDS
            }
            windowLocalizationSeconds.toIntOrNull() ?: run {
              windowLocalizationSeconds = DEFAULT_PREF_CVLOG_WINDOW_LOCALIZATION_SECONDS
            }

            LOG.D2(TAG, "Prefs: logging: $windowLoggingSeconds, localization: $windowLocalizationSeconds")

            CvLoggerPrefs(windowLoggingSeconds, windowLocalizationSeconds, devMode, expImagePadding)
          }
}

data class CvLoggerPrefs(
        val windowLoggingSeconds: String,
        val windowLocalizationSeconds: String,
        val devMode: Boolean,
        val expImagePadding: Boolean,
)
