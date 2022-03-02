package cy.ac.ucy.cs.anyplace.lib.android.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.preference.PreferenceDataStore
import cy.ac.ucy.cs.anyplace.lib.android.LOG
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
 * Navigation settings:
 * - used by Smass
 * - TODO use by regular Navigator app
 */
@Singleton
class DataStoreCvNavigation @Inject constructor(@ApplicationContext private val ctx: Context)
  : PreferenceDataStore() {

  private val C by lazy { CONST(ctx) }
  private val Context.dataStoreCvNavigation by preferencesDataStore(name = C.PREF_CVNAV)
  val datastore = ctx.dataStoreCvNavigation

  private val validKeys = setOf(
          C.PREF_CV_WINDOW_LOCALIZATION_SECONDS,
          C.PREF_CV_DEV_MODE,
          C.PREF_CVNAV_MAP_ALPHA,
  )

  private class Keys(c: CONST) {
    val windowLocalizationSeconds = stringPreferencesKey(c.PREF_CV_WINDOW_LOCALIZATION_SECONDS)
    val devMode = booleanPreferencesKey(c.PREF_CV_DEV_MODE)
    val mapAlpha = stringPreferencesKey(c.PREF_CVNAV_MAP_ALPHA)
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
          C.PREF_CV_WINDOW_LOCALIZATION_SECONDS-> it[KEY.windowLocalizationSeconds] =
                  value ?: C.DEFAULT_PREF_CVNAV_WINDOW_LOCALIZATION_SECONDS
          C.PREF_CVNAV_MAP_ALPHA-> it[KEY.mapAlpha] = value ?: C.DEFAULT_PREF_CVNAV_MAP_ALPHA

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
        C.PREF_CV_WINDOW_LOCALIZATION_SECONDS-> prefs.windowLocalizationSeconds
        C.PREF_CVNAV_MAP_ALPHA-> prefs.mapAlpha
        else -> null
      }
    }
  }

  val read: Flow<CvNavigationPrefs> = ctx.dataStoreCvNavigation.data
          .catch { exception ->
            if (exception is IOException) {
              emit(emptyPreferences())
            } else { throw exception }
          }
          .map { preferences ->
            val windowLocalizationSeconds = preferences[KEY.windowLocalizationSeconds] ?:
            C.DEFAULT_PREF_CVNAV_WINDOW_LOCALIZATION_SECONDS
            val mapAlpha = preferences[KEY.mapAlpha] ?: C.DEFAULT_PREF_CVNAV_MAP_ALPHA
            val devMode = preferences[KEY.devMode] ?: C.DEFAULT_PREF_CV_DEV_MODE
            val prefs = CvNavigationPrefs(windowLocalizationSeconds, mapAlpha, devMode)
            LOG.D2(TAG, "read prefs: $prefs")
            prefs
          }
}

data class CvNavigationPrefs(
        val windowLocalizationSeconds: String,
        val mapAlpha: String, /** visibility of the google maps layer */
        val devMode: Boolean,
)