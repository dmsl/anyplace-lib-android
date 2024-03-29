package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.UserAP
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Anyplace User DataStore (for logging into Anyplace)
 */
@Singleton
class ApUserDataStore @Inject constructor(@ApplicationContext private val ctx: Context) {

  private val C by lazy { CONST(ctx) }
  private val Context.dataStoreUser by preferencesDataStore(name = C.PREF_AP_USER)

  private class Keys(c: CONST) {
    val accessToken = stringPreferencesKey(c.PREF_AP_USER_ACCESS_TOKEN)
    val name = stringPreferencesKey(c.PREF_AP_USER_NAME)
    val username = stringPreferencesKey(c.PREF_AP_USER_USERNAME)
    val id = stringPreferencesKey(c.PREF_AP_USER_ID)
    val email = stringPreferencesKey(c.PREF_AP_USER_EMAIL)
    val account = stringPreferencesKey(c.PREF_AP_USER_ACCOUNT)
    val type = stringPreferencesKey(c.PREF_AP_USER_TYPE)
    val photoUri= stringPreferencesKey(c.PREF_AP_PHOTO_URI)
  }
  private val KEY = Keys(C)

  val read: Flow<UserAP> =
    ctx.dataStoreUser.data
        .catch { exception ->
         if (exception is IOException)  { emit(emptyPreferences()) } else { throw exception }
        }
        .map {
          val accessToken = it[KEY.accessToken] ?: ""
          val id = it[KEY.id] ?: ""
          val name = it[KEY.name] ?: ""
          val type = it[KEY.type] ?: ""
          val account = it[KEY.account] ?: ""
          val username = it[KEY.username] ?: ""
          val email = it[KEY.email] ?: ""
          val photoUri = it[KEY.photoUri] ?: ""

          UserAP(accessToken, id, name, type, account, username, email, photoUri)
        }

  /**
   * Stores a logged in user to the datastore
   */
  suspend fun storeUser(userAP: UserAP) {
    ctx.dataStoreUser.edit {
      it[KEY.accessToken] = userAP.accessToken
      it[KEY.id] = userAP.id
      it[KEY.name] = userAP.name
      it[KEY.type] = userAP.type
      it[KEY.account] = userAP.account
      it[KEY.username] = userAP.username
      it[KEY.email] = userAP.email
      it[KEY.photoUri] = userAP.photoUri
    }
  }

  // TODO delete only if connected to the internet.
  /** Deletes the logged in user */
  suspend fun deleteUser() {
    ctx.dataStoreUser.edit { it.clear() }
  }
}
