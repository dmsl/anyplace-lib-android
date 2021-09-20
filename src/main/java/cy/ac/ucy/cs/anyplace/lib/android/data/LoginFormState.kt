package cy.ac.ucy.cs.anyplace.lib.android.data

/**
 * Data validation state of the login form.
 */
data class LoginFormState(
  val usernameError: Int? = null,
  val passwordError: Int? = null,
  val isDataValid: Boolean = false
)