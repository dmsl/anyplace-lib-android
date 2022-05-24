package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.nw

import android.widget.Toast
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.SmasApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Handling SMAS errors.
 * - Logging-out the user if required
 */
class SmasErrors(private val app: SmasApp,
                 private val scope: CoroutineScope) {
  private val SESSION_KEY = "invalid sessionkey"
  private val DB_ERROR = "db error"
  private val SESSION_KEY_PRETTY = "Logged session expired"
  private val DB_ERROR_PRETTY = "Server Database error"

  /**
   * Returns [true] when an error is handled
   */
  fun handle(app: SmasApp, cause: String?, extra: String?) : Boolean {
    return when (cause) {
      SESSION_KEY -> {
        scope.launch {
          LOG.W(TAG, "Logging out user (from: $extra)")
          app.showToast(scope, "$SESSION_KEY_PRETTY ($extra)", Toast.LENGTH_SHORT)
          logoutUser()
        }
        true
      }
      DB_ERROR -> {
        val msg = "$DB_ERROR_PRETTY ($extra)"
        LOG.W(TAG, msg)
        app.showToast(scope, msg, Toast.LENGTH_SHORT)
        true
      }
      else -> false
    }
  }

  private suspend fun logoutUser() {
    app.dsChatUser.deleteUser()
  }

}
