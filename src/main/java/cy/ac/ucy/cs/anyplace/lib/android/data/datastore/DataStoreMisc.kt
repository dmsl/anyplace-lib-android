package cy.ac.ucy.cs.anyplace.lib.android.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_MISC_BACK_FROM_SETTINGS
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_MISC_BACK_ONLINE
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_MISC_NAME
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

}