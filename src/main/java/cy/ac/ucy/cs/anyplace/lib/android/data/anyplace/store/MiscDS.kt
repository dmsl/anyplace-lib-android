package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceType
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceOwnership
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Miscellaneous settings for the app that need persistency
 * - Filtering preferences for the [SelectSpaceActivity]
 * - storing tutorial flags
 */
@Singleton
class MiscDS @Inject constructor(@ApplicationContext private val ctx: Context) {

  private val C by lazy { CONST(ctx) }
  private val Context.dsMisc by preferencesDataStore(name = C.PREF_MISC_NAME)

  private class Keys(c: CONST) {
    val backOnline = booleanPreferencesKey(c.PREF_MISC_BACK_ONLINE)
    val backFromSettings = booleanPreferencesKey(c.PREF_MISC_BACK_FROM_SETTINGS)

    // SPACES QUERY
    val spaceFilter_ownership= stringPreferencesKey(c.PREF_MISC_QUERY_SPACE_OWNERSHIP)
    val spaceFilter_ownershipId= intPreferencesKey(c.PREF_MISC_QUERY_SPACE_OWNERSHIP_ID)
    val spaceFilter_type = stringPreferencesKey(c.PREF_MISC_QUERY_SPACE_TYPE)
    val spaceFilter_typeId = intPreferencesKey(c.PREF_MISC_QUERY_SPACE_TYPE_ID)

    // TUTORIALS (tutorial example)
    //// LOGGER: (just an example on how to do a tutorial. follow all variables related to this)
    val tutLogMapLongPress=booleanPreferencesKey(c.PREF_MISC_TUT_LOG_LONG_PRESS)
    val tutNavMapLongPress=booleanPreferencesKey(c.PREF_MISC_TUT_NAV_LONG_PRESS)
    val tutNavLocalize=booleanPreferencesKey(c.PREF_MISC_TUT_NAV_LOCALIZE)
    val tutNavTracking=booleanPreferencesKey(c.PREF_MISC_TUT_NAV_TRACKING)
    val tutNavWhereAmI=booleanPreferencesKey(c.PREF_MISC_TUT_NAV_WHEREAMI)
    val tutNavImu=booleanPreferencesKey(c.PREF_MISC_TUT_NAV_IMU)
  }
  private val KEY = Keys(C)

  suspend fun saveBackOnline(value: Boolean) = saveBoolean(KEY.backOnline, value)
  suspend fun saveBackFromSettings(value: Boolean) = saveBoolean(KEY.backOnline, value)

  /** assign a tutorial as watched (false: don't trigger it again) */
  private suspend fun tutorialWatched(key: Preferences.Key<Boolean>) = saveBoolean(key, false)

  private suspend fun saveBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
    ctx.dsMisc.edit { prefs -> prefs[key] = value }
  }

  /** TODO:PM make this app specific?!
   * see its usages
   */
  val backOnline = readBoolean(KEY.backOnline, false)
  /** */
  val backFromSettings = readBoolean(KEY.backFromSettings, false)

  /** whether the long-press tutorial of the logger was shown yet or not */
  suspend fun showTutorialLoggerMapLongPress() = tutInternal(KEY.tutLogMapLongPress)
  /** whether the long-press tutorial of the navigator was shown yet or not */
  suspend fun showTutorialNavMapLongPress() = tutInternal(KEY.tutNavMapLongPress)
  suspend fun showTutorialNavLocalize() = tutInternal(KEY.tutNavLocalize)
  suspend fun showTutorialNavTracking() = tutInternal(KEY.tutNavTracking)
  suspend fun showTutorialNavWhereAmI() = tutInternal(KEY.tutNavWhereAmI)
  suspend fun showTutorialNavImu(forcedTutorial: Boolean = false) = tutInternal(KEY.tutNavImu, forcedTutorial)

  /**
   * Figures out if a tutorial must be shown, and returns that value.
   * In those cases, it disables it so the next time it won't run
   */
  private suspend fun tutInternal(key: Preferences.Key<Boolean>, forcedTutorial: Boolean=false) : Boolean {
    if (!DBG.TUTORIALS && !forcedTutorial) return false  // centrally controlling tutorials

    val result = readBoolean(key, true).first()
    if (result) { tutorialWatched(key) } // unset tutorial
    return result
  }

  private fun readBoolean(key: Preferences.Key<Boolean>, default: Boolean) : Flow<Boolean> {
    return ctx.dsMisc.data
            .catch { exception ->
              if (exception is IOException) {
                emit(emptyPreferences())
              } else {
                throw exception
              }
            }.map { prefs -> prefs[key] ?: default }
  }



  suspend fun saveQuerySpace(queryFilter: SpaceFilter) {
    ctx.dsMisc.edit { preferences ->
      preferences[KEY.spaceFilter_ownership] = queryFilter.ownership.toString().uppercase()
      preferences[KEY.spaceFilter_ownershipId] = queryFilter.ownershipId
      preferences[KEY.spaceFilter_type] = queryFilter.spaceType.toString().uppercase()
      preferences[KEY.spaceFilter_typeId] = queryFilter.spaceTypeId
    }
  }

  val spaceFilter: Flow<SpaceFilter> = ctx.dsMisc.data
      .catch { exception ->
        if (exception is IOException)  {
          emit(emptyPreferences())
        } else {
          throw exception
        }
      }
      .map { preferences ->
        val ownershipStr = preferences[KEY.spaceFilter_ownership] ?: C.DEFAULT_QUERY_SPACE_OWNERSHIP
        val ownershipId = preferences[KEY.spaceFilter_ownershipId] ?: 0

        val spaceTypeStr = preferences[KEY.spaceFilter_type] ?: C.DEFAULT_QUERY_SPACE_TYPE
        val spaceTypeId = preferences[KEY.spaceFilter_typeId] ?: 0

        SpaceFilter(SpaceOwnership.valueOf(ownershipStr.uppercase()), ownershipId,
          SpaceType.valueOf(spaceTypeStr.uppercase()), spaceTypeId)
      }
}

/**
 * Filtering parameters for the [SpaceSelector]
 * - assigned by the [Chip] of [SpaceFilterBottomSheet],
 *   and are persisted through [dsMisc]
 * - text predicate is not persisted.
 */
data class SpaceFilter(
        val ownership: SpaceOwnership = SpaceOwnership.ALL,
        val ownershipId: Int=0,
        val spaceType: SpaceType = SpaceType.ALL,
        val spaceTypeId: Int=0,
        var spaceName: String=""
  )