package cy.ac.ucy.cs.anyplace.lib.android.ui.login

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.ui.settings.MainSettingsDialog
import cy.ac.ucy.cs.anyplace.lib.android.ui.BaseActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.selector.space.SelectSpaceActivity
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.LoginViewModel
import cy.ac.ucy.cs.anyplace.lib.databinding.ActivityLoginBinding
import cy.ac.ucy.cs.anyplace.lib.models.User
import cy.ac.ucy.cs.anyplace.lib.models.UserLoginGoogleData
import cy.ac.ucy.cs.anyplace.lib.models.UserLoginLocalForm
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : BaseActivity() {
  private val TAG = "ap_"+LoginActivity::class.java.simpleName

  private lateinit var loginViewModel: LoginViewModel
  private lateinit var mGoogleSignInClient: GoogleSignInClient
  private var _binding: ActivityLoginBinding ?= null
  private val binding get() = _binding!!

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    _binding = ActivityLoginBinding.inflate(layoutInflater)
    setContentView(binding.root)

    val username = binding.username
    val password = binding.password
    val localLogin = binding.buttonLoginLocal

    loginViewModel = ViewModelProvider(this).get(LoginViewModel::class.java)
    loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
      val loginState = it ?: return@Observer

      // disable login button unless both username / password is valid
      localLogin.isEnabled = loginState.isDataValid

      if (loginState.usernameError != null) {
        username.error = getString(loginState.usernameError)
      }
      if (loginState.passwordError != null) {
        password.error = getString(loginState.passwordError)
      }
    })

    setupLocalLogin(username, password, localLogin)
    setupGoogleLogin()

    observeUserLoginResponse() // used by both Anyplace and Google login
  }

  private inline fun ActivityResult.checkResultAndExecute(block: ActivityResult.() -> Unit) =
    if (resultCode == Activity.RESULT_OK) runCatching(block)
    else Result.failure(Exception("Something went wrong"))

  private val googleRegisterResult =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      result.checkResultAndExecute {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        val account = task.getResult(ApiException::class.java)

        if (account.idToken != null && account.displayName != null) {
          account.displayName?.let { name ->
            LOG.D("DIS NAME: " + account.displayName)
            val googleData = UserLoginGoogleData("google", name, account.idToken)
            loginViewModel.loginUserGoogle(googleData, account.photoUrl)
          } ?: run {
            throw Exception("can't get user's name")
          }
        } else {
          val msg = "Failed to get Google OAuth Token."
          LOG.E(msg)
          Toast.makeText(applicationContext, msg , Toast.LENGTH_LONG).show()
        }
      }.onFailure { e ->
        val msg = "Google login failed"
        val errMsg = e.message?.take(200)
        LOG.E("$msg: $errMsg")
        Toast.makeText(applicationContext, msg , Toast.LENGTH_LONG).show()
      }
    }

  private fun setupGoogleLogin() {
    binding.buttonLoginGoogle.setOnClickListener {
      // server_google_oauth_client_id is set by from local.properties (project root)
      // see lib-android's build.gradle
      val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
          .requestIdToken(getString(R.string.server_google_oauth_client_id))
          .requestProfile()
          .requestEmail()
          .build()

      mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

      // LEFTHERE:
      // 1: Implement the logout (for local user or other user..)
      // 2. on logout, if google account, then replace with the below
      // 3. on logged in, update the settings UI its load..
      // 4. remove the logic of logout below (and in the StartActivity maybe..)
      // 5. THINK about this: we could logout google user immediately.. but do we want this?

      // TODO on Settings Dialog: if from LoginActivity, send parameters..
      // so if login pressed it will hide stuff..

      // TODOs from previous times:
      // TODO: 7: Add on the settings XML: put on top some things about login..
      // TODO: 8: maybe make it an activity.. with all the settings in it..
      // TODO: 9: GoogleMaps

      val acct = GoogleSignIn.getLastSignedInAccount(this)
      if (acct != null) {
        LOG.D("User was already logged in")
      }
      val signInIntent: Intent = mGoogleSignInClient.signInIntent
      googleRegisterResult.launch(signInIntent)
    }
  }

    /**
   * Setup listeners for the username and password editext,
   * changes on the submitted text, enabling/disabling login button, etc
   */
  private fun setupLocalLogin(username: EditText, password: EditText, login: Button) {

    val loading = binding.loading
    username.afterTextChanged {
      loginViewModel.loginDataChanged(username.text.toString(), password.text.toString())
    }

    password.apply {
      afterTextChanged {
        loginViewModel.loginDataChanged(username.text.toString(), password.text.toString())
      }

      // submitted from keyboard
      setOnEditorActionListener { _, actionId, _ ->
        when (actionId) {
          EditorInfo.IME_ACTION_DONE -> {
            val user = username.text.toString()
            val pass = password.text.toString()
            loginViewModel.loginUserLocal(UserLoginLocalForm(user, pass))
          }
        }
        false
      }

      // submitted from button
      login.setOnClickListener {
        loading.visibility = View.VISIBLE
        val user = username.text.toString()
        val pass = password.text.toString()
        loginViewModel.loginUserLocal(UserLoginLocalForm(user, pass))
      }

      setOnTouchListener { _, event ->
        // val DRAWABLE_LEFT = 0
        // val DRAWABLE_TOP = 1
        val DRAWABLE_RIGHT = 2
        // val DRAWABLE_BOTTOM = 3

        val eventOnRightDrawable =
          event.rawX >= right - compoundDrawables[DRAWABLE_RIGHT].bounds.width()
        if (eventOnRightDrawable) {
          if (event.action == MotionEvent.ACTION_UP) {  // hide password
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
          } else if (event.action == MotionEvent.ACTION_DOWN) { // show password
              inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
          }
          return@setOnTouchListener true
        }

        // unhandled
        performClick()
        false
      }
    }
  }

  /**
   * Works with both local (anyplace) login and Google login,
   * as the backend returns the same, compatible user object
   */
  private fun observeUserLoginResponse() {
    loginViewModel.userLoginResponse.observe(this@LoginActivity,  { response ->
      LOG.D3(TAG, "observeUserLoginResponse: ${response.message}")
      when (response) {
        is NetworkResult.Success -> {
          binding.loading.visibility = View.GONE
          binding.imageViewError.visibility = View.INVISIBLE
          binding.textViewError.visibility = View.INVISIBLE

          // Store user in datastore
          lifecycleScope.launch {
            val user = response.data?.user
            user?.let {
              Toast.makeText(this@LoginActivity, "Welcome: " + user.name, Toast.LENGTH_SHORT).show()
              app.dataStoreUser.storeUser(user)
              signOutGoogleAuth(user) // for google logins
              openLoggedInActivity()
            }
          }
        }
        is NetworkResult.Error -> {
          // LOG.D(TAG, "observeUserLoginResponse: err")
          binding.textViewError.text = response.message
          binding.loading.visibility = View.GONE
          binding.imageViewError.visibility = View.VISIBLE
          binding.textViewError.visibility = View.VISIBLE
        }
        is NetworkResult.Loading -> {
          binding.loading.visibility = View.VISIBLE
          binding.imageViewError.visibility = View.INVISIBLE
          binding.textViewError.visibility = View.INVISIBLE
        }
      }
    })
  }

  /**
   * Sign out from the Google authentication.
   * See full Google login process below.
   *
   * For Google login we do the following:
   * 1. Sign in with Google OAuth
   * 2. Get Google Access Token alongside step 1
   * 3. Send the Google Access Token to Anyplace to verify and authenticate
   * 4. Once authenticated get the Anyplace User account and put in a datastore
   * 5. Sign out from the google authentication
   */
  private fun signOutGoogleAuth(user: User) {
    if (user.account == "google") {
      mGoogleSignInClient.signOut().addOnCompleteListener {
        LOG.D2(TAG, "Signed out Google oauth after successful Anyplace login.")
      }
    }
  }

  private fun openLoggedInActivity() {
    startActivity(Intent(this@LoginActivity, SelectSpaceActivity::class.java))
    finish()
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.main_menu_settings, menu)
    return super.onCreateOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val id = item.itemId
    if(id == R.id.item_settings) {
      MainSettingsDialog().show(supportFragmentManager, MainSettingsDialog.FROM_LOGIN)
    }
    return super.onOptionsItemSelected(item)
  }

}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
  this.addTextChangedListener(object : TextWatcher {
    override fun afterTextChanged(editable: Editable?) {
      afterTextChanged.invoke(editable.toString())
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
  })
}