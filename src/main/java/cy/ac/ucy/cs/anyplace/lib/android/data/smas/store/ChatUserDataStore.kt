package cy.ac.ucy.cs.anyplace.lib.android.data.smas.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.SMAS
import cy.ac.ucy.cs.anyplace.lib.smas.models.SmasUser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore for the Logged-in SMAS user
 */
@Singleton
class ChatUserDataStore @Inject constructor(@ApplicationContext private val ctx: Context) {

  private val C by lazy { SMAS(ctx) }
  private val Context.dsChatUser by preferencesDataStore(name = C.PREF_SMAS_USER)

  private class Keys(c: SMAS) {
    val uid = stringPreferencesKey(c.PREF_USER_ID)
    val sessionkey = stringPreferencesKey(c.PREF_USER_ACCESS_TOKEN)
  }
  private val KEY = Keys(C)

  val readUser: Flow<SmasUser> =
    ctx.dsChatUser.data
        .catch { exception ->
         if (exception is IOException)  { emit(emptyPreferences()) } else { throw exception }
        }
        .map {
          val sessionkey = it[KEY.sessionkey] ?: ""
          val uid = it[KEY.uid] ?: ""
          SmasUser(uid, sessionkey)
        }

  /** Stores a logged in user to the datastore */
  suspend fun storeUser(user: SmasUser) {
    ctx.dsChatUser.edit {
      it[KEY.uid] = user.uid
      it[KEY.sessionkey] = user.sessionkey
    }
  }

  /** Deletes the logged in user */
  suspend fun deleteUser() {  ctx.dsChatUser.edit { it.clear() } }
}
