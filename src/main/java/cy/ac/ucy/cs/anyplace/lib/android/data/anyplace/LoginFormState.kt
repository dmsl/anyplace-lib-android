package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace

/**
 * Data validation state of the login form.
 * Used for Anyplace and SMAS login
 */
data class LoginFormState(
  val usernameError: Int? = null,
  val passwordError: Int? = null,
  val isDataValid: Boolean = false
)