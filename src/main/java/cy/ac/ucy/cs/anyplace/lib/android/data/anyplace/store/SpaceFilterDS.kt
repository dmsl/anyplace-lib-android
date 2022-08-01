package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceType
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceOwnership
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Filtering preferences for the [SelectSpaceActivity]
 */
@Singleton
class SpaceFilterDS @Inject constructor(@ApplicationContext private val ctx: Context) {

  private val C by lazy { CONST(ctx) }
  private val Context.dataStoreMisc by preferencesDataStore(name = C.PREF_MISC_NAME)

  private class Keys(c: CONST) {
    val backOnline = booleanPreferencesKey(c.PREF_MISC_BACK_ONLINE)
    val backFromSettings = booleanPreferencesKey(c.PREF_MISC_BACK_FROM_SETTINGS)

    // SPACES QUERY
    val querySpace_ownership= stringPreferencesKey(c.PREF_MISC_QUERY_SPACE_OWNERSHIP)
    val querySpace_ownershipId= intPreferencesKey(c.PREF_MISC_QUERY_SPACE_OWNERSHIP_ID)

    val querySpace_type = stringPreferencesKey(c.PREF_MISC_QUERY_SPACE_TYPE)
    val querySpace_typeId = intPreferencesKey(c.PREF_MISC_QUERY_SPACE_TYPE_ID)

    // val queryTypeSpace= stringPreferencesKey(c.PREF_MISC_QUERY_TYPE_SPACE)
  }
  private val KEY = Keys(C)

  suspend fun saveBackOnline(value: Boolean) =
    saveBoolean(KEY.backOnline, value)

  suspend fun saveBackFromSettings(value: Boolean) =
    saveBoolean(KEY.backOnline, value)

  private suspend fun saveBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
    ctx.dataStoreMisc.edit { prefs -> prefs[key] = value }
  }

  val readBackOnline : Flow<Boolean> = ctx.dataStoreMisc.data
      .catch {  exception ->
        if (exception is IOException) {
          emit(emptyPreferences())
        } else { throw exception }
      }.map { prefs -> prefs[KEY.backOnline] ?: false }

  val readBackFromSettings : Flow<Boolean> = ctx.dataStoreMisc.data
      .catch {  exception ->
        if (exception is IOException) {
          emit(emptyPreferences())
        } else { throw exception }
      }.map { prefs -> prefs[KEY.backFromSettings] ?: false }


  suspend fun saveQuerySpace(queryFilter: SpaceFilter) {
    ctx.dataStoreMisc.edit { preferences ->
      preferences[KEY.querySpace_ownership] = queryFilter.ownership.toString().uppercase()
      preferences[KEY.querySpace_ownershipId] = queryFilter.ownershipId
      preferences[KEY.querySpace_type] = queryFilter.spaceType.toString().uppercase()
      preferences[KEY.querySpace_typeId] = queryFilter.spaceTypeId
    }
  }

  val readSpaceFilterFilter: Flow<SpaceFilter> = ctx.dataStoreMisc.data
      .catch { exception ->
        if (exception is IOException)  {
          emit(emptyPreferences())
        } else {
          throw exception
        }
      }
      .map { preferences ->
        val ownershipStr = preferences[KEY.querySpace_ownership] ?: C.DEFAULT_QUERY_SPACE_OWNERSHIP
        val ownershipId = preferences[KEY.querySpace_ownershipId] ?: 0

        val spaceTypeStr = preferences[KEY.querySpace_type] ?: C.DEFAULT_QUERY_SPACE_TYPE
        val spaceTypeId = preferences[KEY.querySpace_typeId] ?: 0

        SpaceFilter(SpaceOwnership.valueOf(ownershipStr.uppercase()), ownershipId,
          SpaceType.valueOf(spaceTypeStr.uppercase()), spaceTypeId)
      }
}

data class SpaceFilter(
        val ownership: SpaceOwnership = SpaceOwnership.ALL,
        val ownershipId: Int=0,
        val spaceType: SpaceType = SpaceType.ALL,
        val spaceTypeId: Int=0,
        var spaceName: String=""
  )