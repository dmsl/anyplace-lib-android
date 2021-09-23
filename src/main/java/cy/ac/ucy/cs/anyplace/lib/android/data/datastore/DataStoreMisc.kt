package cy.ac.ucy.cs.anyplace.lib.android.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.DEFAULT_QUERY_SPACE_OWNERSHIP
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.DEFAULT_QUERY_SPACE_TYPE
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_MISC_BACK_FROM_SETTINGS
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_MISC_BACK_ONLINE
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_MISC_NAME
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_MISC_QUERY_SPACE_OWNERSHIP
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_MISC_QUERY_SPACE_OWNERSHIP_ID
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_MISC_QUERY_SPACE_TYPE
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_MISC_QUERY_SPACE_TYPE_ID
import cy.ac.ucy.cs.anyplace.lib.android.data.db.entities.SpaceType
import cy.ac.ucy.cs.anyplace.lib.android.data.db.entities.UserOwnership
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

private val Context.dataStoreMisc by preferencesDataStore(name = PREF_MISC_NAME)

/**
 * Miscellaneous preferences
 */
@ViewModelScoped
class DataStoreMisc @Inject constructor(@ApplicationContext private val ctx: Context) {
  // TODO: DEBUG MODES:
  // BETA, ALPHA, DEV ?

  private object KEY {
    val backOnline = booleanPreferencesKey(PREF_MISC_BACK_ONLINE)
    val backFromSettings = booleanPreferencesKey(PREF_MISC_BACK_FROM_SETTINGS)

    // SPACES QUERY
    val querySpace_ownership= stringPreferencesKey(PREF_MISC_QUERY_SPACE_OWNERSHIP)
    val querySpace_ownershipId= intPreferencesKey(PREF_MISC_QUERY_SPACE_OWNERSHIP_ID)

    val querySpace_type = stringPreferencesKey(PREF_MISC_QUERY_SPACE_TYPE)
    val querySpace_typeId = intPreferencesKey(PREF_MISC_QUERY_SPACE_TYPE_ID)

    // val queryTypeSpace= stringPreferencesKey(PREF_MISC_QUERY_TYPE_SPACE)
  }

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


  suspend fun saveQuerySpace(query: QuerySelectSpace) {
    ctx.dataStoreMisc.edit { preferences ->
      preferences[KEY.querySpace_ownership] = query.ownership.toString().uppercase()
      preferences[KEY.querySpace_ownershipId] = query.ownershipId
      preferences[KEY.querySpace_type] = query.spaceType.toString().uppercase()
      preferences[KEY.querySpace_typeId] = query.spaceTypeId
    }
  }

  val readQuerySpace: Flow<QuerySelectSpace> = ctx.dataStoreMisc.data
      .catch { exception ->
        if (exception is IOException)  {
          emit(emptyPreferences())
        } else {
          throw exception
        }
      }
      .map { preferences ->
        val ownershipStr = preferences[KEY.querySpace_ownership] ?: DEFAULT_QUERY_SPACE_OWNERSHIP
        val ownershipId = preferences[KEY.querySpace_ownershipId] ?: 0

        val spaceTypeStr = preferences[KEY.querySpace_type] ?: DEFAULT_QUERY_SPACE_TYPE
        val spaceTypeId = preferences[KEY.querySpace_typeId] ?: 0

        QuerySelectSpace(UserOwnership.valueOf(ownershipStr.uppercase()), ownershipId,
          SpaceType.valueOf(spaceTypeStr.uppercase()), spaceTypeId)
      }
}

data class QuerySelectSpace(
  val ownership: UserOwnership,
  val ownershipId: Int,
  val spaceType: SpaceType,
  val spaceTypeId: Int,
  )