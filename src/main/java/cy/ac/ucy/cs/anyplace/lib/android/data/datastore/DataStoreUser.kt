package cy.ac.ucy.cs.anyplace.lib.android.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_PHOTO_URI
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_USER
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_USER_ACCESS_TOKEN
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_USER_ACCOUNT
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_USER_EMAIL
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_USER_ID
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_USER_NAME
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_USER_TYPE
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.PREF_USER_USERNAME
import cy.ac.ucy.cs.anyplace.lib.models.User
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

private val Context.dataStoreUser by preferencesDataStore(name = PREF_USER)

@ViewModelScoped
class DataStoreUser @Inject constructor(@ApplicationContext private val ctx: Context) {
  private object KEY {
    val accessToken = stringPreferencesKey(PREF_USER_ACCESS_TOKEN)
    val name = stringPreferencesKey(PREF_USER_NAME)
    val username = stringPreferencesKey(PREF_USER_USERNAME)
    val id = stringPreferencesKey(PREF_USER_ID)
    val email = stringPreferencesKey(PREF_USER_EMAIL)
    val account = stringPreferencesKey(PREF_USER_ACCOUNT)
    val type = stringPreferencesKey(PREF_USER_TYPE)
    val photoUri= stringPreferencesKey(PREF_PHOTO_URI)
  }

  val readUser: Flow<User> =
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

          User(accessToken, id, name, type, account, username, email, photoUri)
        }

  /**
   * Stores a logged in user to the datastore
   */
  suspend fun storeUser(user: User) {
    ctx.dataStoreUser.edit {
      it[KEY.accessToken] = user.accessToken
      it[KEY.id] = user.id
      it[KEY.name] = user.name
      it[KEY.type] = user.type
      it[KEY.account] = user.account
      it[KEY.username] = user.username
      it[KEY.email] = user.email
      it[KEY.photoUri] = user.photoUri
    }
  }

  // TODO delete only if connected to the internet.
  /** Deletes the logged in user */
  suspend fun deleteUser() {
    ctx.dataStoreUser.edit { it.clear() }
  }
}
