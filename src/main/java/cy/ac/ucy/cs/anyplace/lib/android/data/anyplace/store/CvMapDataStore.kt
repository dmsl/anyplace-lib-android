package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.preference.PreferenceDataStore
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Settings for [CvMap]-based activities
 * - common settings
 * - used both in CvLogger and SMAS apps
 *
 */
@Singleton
class CvMapDataStore @Inject constructor(@ApplicationContext private val ctx: Context)
  : PreferenceDataStore() {
  val TG = "ds-cv"

  private val C by lazy { CONST(ctx) }
  private val Context.dataStoreCvMap by preferencesDataStore(name = C.PREF_CVMAP)
  val datastore = ctx.dataStoreCvMap

  private val validKeys = setOf(
          C.PREF_CV_START_ACT,
          C.PREF_CV_WINDOW_LOCALIZATION_MS,
          C.PREF_CV_WINDOW_LOGGING_MS,
          C.PREF_CV_SCAN_DELAY,
          C.PREF_CV_DEV_MODE,
          C.PREV_CVMAP_ALPHA,
          C.PREF_SMAS_LOCATION_REFRESH_MS,
          C.PREF_CV_AUTOSET_INITIAL_LOCATION,
          C.PREF_CV_FOLLOW_SELECTED_USER,
          C.PREF_SELECTED_SPACE,
          C.PREF_CV_AUTO_UPDATE_FINGERPRINTS,
          C.PREF_CV_LOC_ALGO_CHOICE,
          C.PREF_CV_LOC_ALGO_EXECUTION,
          C.PREF_CV_TRACKING_DELAY,
          C.PREF_CV_TRACKING_AUTO_DISABLE,
  )

  private class Keys(c: CONST) {
    val startActivity = stringPreferencesKey(c.PREF_CV_START_ACT)
    val windowLocalizationMs = stringPreferencesKey(c.PREF_CV_WINDOW_LOCALIZATION_MS)
    val windowLoggingMs = stringPreferencesKey(c.PREF_CV_WINDOW_LOGGING_MS)
    val scanDelay = stringPreferencesKey(c.PREF_CV_SCAN_DELAY)
    val devMode = booleanPreferencesKey(c.PREF_CV_DEV_MODE)
    val mapAlpha = stringPreferencesKey(c.PREV_CVMAP_ALPHA)
    val locationRefreshMs = stringPreferencesKey(c.PREF_SMAS_LOCATION_REFRESH_MS)
    val autoSetInitialLocation = booleanPreferencesKey(c.PREF_CV_AUTOSET_INITIAL_LOCATION)
    val followSelectedUser = booleanPreferencesKey(c.PREF_CV_FOLLOW_SELECTED_USER)
    val selectedSpace = stringPreferencesKey(c.PREF_SELECTED_SPACE)
    val autoUpdateCvFingerprints= booleanPreferencesKey(c.PREF_CV_AUTO_UPDATE_FINGERPRINTS)

    val cvAlgoChoice=stringPreferencesKey(c.PREF_CV_LOC_ALGO_CHOICE)
    val cvAlgoExec=stringPreferencesKey(c.PREF_CV_LOC_ALGO_EXECUTION)

    val cvTrackingDelay=stringPreferencesKey(c.PREF_CV_TRACKING_DELAY)
    val cvTrackingAutoDisable=stringPreferencesKey(c.PREF_CV_TRACKING_AUTO_DISABLE)
  }
  private val KEY = Keys(C)

  private fun validKey(key: String?): Boolean {
    val MT = ::validKey.name
    val found = validKeys.contains(key)
    if(!found) LOG.W(TG, "$MT unknown: $key")
    return found
  }

  override fun putBoolean(key: String?, value: Boolean) {
    val MT = ::putBoolean.name
    if (!validKey(key)) return
    runBlocking {
      datastore.edit {
        LOG.W(TG, "$MT: $key:$value" )
        when (key) {
          C.PREF_CV_DEV_MODE -> it[KEY.devMode] = value
          C.PREF_CV_AUTOSET_INITIAL_LOCATION-> it[KEY.autoSetInitialLocation] = value
          C.PREF_CV_FOLLOW_SELECTED_USER-> it[KEY.followSelectedUser] = value
          C.PREF_CV_AUTO_UPDATE_FINGERPRINTS-> it[KEY.autoUpdateCvFingerprints] = value
        }
      }
    }
  }

  /**
   * WORKAROUND: SharedPreferences/XML use by default string.
   * Here it is convert ot an [Int] and stored in the [DataStore].
   */
  override fun putString(key: String?, value: String?) {
    val MT = ::putString.name
    LOG.W(TG, "$MT: $key = $value")

    if (!validKey(key)) return
    runBlocking {

      val app = C.ctx as AnyplaceApp

      datastore.edit {
        when (key) {
          C.PREF_CV_START_ACT-> it[KEY.startActivity] = value ?: app.defaultNavigationAppCode()

          C.PREF_CV_WINDOW_LOCALIZATION_MS-> it[KEY.windowLocalizationMs] = value ?: C.DEFAULT_PREF_CV_WINDOW_LOCALIZATION_MS

          C.PREF_CV_WINDOW_LOGGING_MS-> it[KEY.windowLoggingMs] = value ?: C.DEFAULT_PREF_CVLOG_WINDOW_LOGGING_MS

          C.PREF_CV_SCAN_DELAY-> it[KEY.scanDelay] = value ?: C.DEFAULT_PREF_CV_SCAN_DELAY

          C.PREV_CVMAP_ALPHA-> it[KEY.mapAlpha] = value ?: C.DEFAULT_PREF_CVMAP_ALPHA

          C.PREF_SMAS_LOCATION_REFRESH_MS-> it[KEY.locationRefreshMs] = value ?: C.DEFAULT_PREF_SMAS_LOCATION_REFRESH_MS

          C.PREF_SELECTED_SPACE-> it[KEY.selectedSpace] = value ?: ""

          C.PREF_CV_LOC_ALGO_CHOICE-> it[KEY.cvAlgoChoice] = value ?: C.DEFAULT_PREF_CV_LOC_ALGO_CHOICE

          C.PREF_CV_LOC_ALGO_EXECUTION-> it[KEY.cvAlgoExec] = value ?: C.DEFAULT_PREF_CV_LOC_ALGO_EXECUTION

          C.PREF_CV_TRACKING_DELAY-> it[KEY.cvTrackingDelay] = value ?: C.DEFAULT_PREF_CV_TRACKING_DELAY
          C.PREF_CV_TRACKING_AUTO_DISABLE-> it[KEY.cvTrackingDelay] = value ?: C.DEFAULT_PREF_CV_TRACKING_AUTO_DISABLE
        }
      }
    }
  }

  override fun getBoolean(key: String?, defValue: Boolean): Boolean {
    val MT = ::getBoolean.name
    if (!validKey(key)) return false
    return runBlocking(Dispatchers.IO) {
      LOG.W(TG, "$MT: read")
      val prefs = read.first()
      return@runBlocking when (key) {
        C.PREF_CV_DEV_MODE -> prefs.devMode
        C.PREF_CV_AUTOSET_INITIAL_LOCATION-> prefs.autoSetInitialLocation
        C.PREF_CV_FOLLOW_SELECTED_USER-> prefs.followSelectedUser
        C.PREF_CV_AUTO_UPDATE_FINGERPRINTS-> prefs.autoUpdateCvFingerprints
        else -> false
      }
    }
  }

  override fun getString(key: String?, defValue: String?): String? {
    val MT = ::getString.name
    if (!validKey(key)) return null
    return runBlocking(Dispatchers.IO) {
      LOG.D(TG, "$MT: calls read")
      val prefs = read.first()
      return@runBlocking when (key) {
        C.PREF_CV_START_ACT-> prefs.startActivity
        C.PREF_CV_WINDOW_LOCALIZATION_MS-> prefs.windowLocalizationMs
        C.PREF_CV_WINDOW_LOGGING_MS-> prefs.windowLoggingMs
        C.PREF_CV_SCAN_DELAY-> { prefs.scanDelay }
        C.PREV_CVMAP_ALPHA-> prefs.mapAlpha
        C.PREF_SMAS_LOCATION_REFRESH_MS-> prefs.locationRefreshMs
        C.PREF_SELECTED_SPACE -> prefs.selectedSpace

        C.PREF_CV_LOC_ALGO_CHOICE-> prefs.cvAlgoChoice
        C.PREF_CV_LOC_ALGO_EXECUTION-> prefs.cvAlgoExec

        C.PREF_CV_TRACKING_DELAY-> prefs.cvTrackingDelay
        C.PREF_CV_TRACKING_AUTO_DISABLE-> prefs.cvTrackingAutoDisable
        else -> null
      }
    }
  }

  val read: Flow<CvMapPrefs> = ctx.dataStoreCvMap.data
          .catch { exception ->
            if (exception is IOException) {
              emit(emptyPreferences())
            } else { throw exception }
          }
          .map { preferences ->

            val app = C.ctx as AnyplaceApp

            val startAct = preferences[KEY.startActivity] ?: app.defaultNavigationAppCode()
            val windowLocalizationMs = preferences[KEY.windowLocalizationMs] ?: C.DEFAULT_PREF_CV_WINDOW_LOCALIZATION_MS
            val windowLoggingMs= preferences[KEY.windowLoggingMs] ?: C.DEFAULT_PREF_CVLOG_WINDOW_LOGGING_MS
            val mapAlpha = preferences[KEY.mapAlpha] ?: C.DEFAULT_PREF_CVMAP_ALPHA
            val scanDelay= preferences[KEY.scanDelay] ?: C.DEFAULT_PREF_CV_SCAN_DELAY
            val devMode = preferences[KEY.devMode] ?: C.DEFAULT_PREF_CV_DEV_MODE
            val locationRefresh= preferences[KEY.locationRefreshMs] ?: C.DEFAULT_PREF_SMAS_LOCATION_REFRESH_MS
            val autoSetInitialLocation = preferences[KEY.autoSetInitialLocation] ?: C.DEFAULT_PREF_CV_AUTOSET_INITIAL_LOCATION
            val followSelectedUser = preferences[KEY.followSelectedUser] ?: C.DEFAULT_PREF_CV_FOLLOW_SELECTED_USER
            val selecteSpace = preferences[KEY.selectedSpace] ?: ""
            val autoUpdateCvFingerprints = preferences[KEY.autoUpdateCvFingerprints] ?: C.DEFAULT_PREF_CV_AUTO_UPDATE_FINGERPRINTS

            val cvAlgoChoice = preferences[KEY.cvAlgoChoice] ?: C.DEFAULT_PREF_CV_LOC_ALGO_CHOICE
            val cvAlgoExec= preferences[KEY.cvAlgoExec] ?: C.DEFAULT_PREF_CV_LOC_ALGO_EXECUTION

            val cvTrackingDelay= preferences[KEY.cvTrackingDelay] ?: C.DEFAULT_PREF_CV_TRACKING_DELAY
            val cvTrackingAutoDisable= preferences[KEY.cvTrackingAutoDisable] ?: C.DEFAULT_PREF_CV_TRACKING_AUTO_DISABLE

            val prefs = CvMapPrefs(startAct,
                    windowLocalizationMs,
                    windowLoggingMs,
                    scanDelay,
                    mapAlpha,
                    devMode,
                    locationRefresh,
                    autoSetInitialLocation,
                    followSelectedUser,
                    selecteSpace,
                    autoUpdateCvFingerprints,
                    cvAlgoChoice,
                    cvAlgoExec,
                    cvTrackingDelay,
                    cvTrackingAutoDisable)
            prefs
          }

  fun setMainActivity(value: String) { putString(C.PREF_CV_START_ACT, value) }
  fun setSelectedSpace(buid: String) { putString(C.PREF_SELECTED_SPACE, buid) }
  fun clearSelectedSpace() { setSelectedSpace("") }
}

data class CvMapPrefs(
        val startActivity: String,
        val windowLocalizationMs: String,
        val windowLoggingMs: String,
        /** Save power by introducing artificial delay between CV scans */
        val scanDelay: String,
        /** visibility of the google maps layer */
        val mapAlpha: String,
        val devMode: Boolean,
        /** how often to fetch nearby users location (in seconds) */
        val locationRefreshMs: String,
        val autoSetInitialLocation: Boolean,
        val followSelectedUser: Boolean,
        val selectedSpace: String,
        val autoUpdateCvFingerprints: Boolean,

        val cvAlgoChoice: String,
        val cvAlgoExec: String,

        val cvTrackingDelay: String,
        val cvTrackingAutoDisable: String,
)