package cy.ac.ucy.cs.anyplace.lib.android.ui.user

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
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
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.ui.BaseActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.selector.space.SelectSpaceActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.settings.SettingsCvActivity
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.AnyplaceLoginViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.UserAP
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.UserLoginGoogleData
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.UserLoginLocalForm
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.databinding.ActivityLoginAnyplaceBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Logins to the Anyplace backend
 *
 * Not in use, but can be used as a reference.
 * Things are now deprecated and probably wont work
 * (i.e. google login does not work, might need some changes)
 *
 */
@AndroidEntryPoint
class AnyplaceLoginActivity : BaseActivity() {
  val TG = "act-login-ap"

  private lateinit var VMlogin: AnyplaceLoginViewModel
  private lateinit var mGoogleSignInClient: GoogleSignInClient
  private var _binding: ActivityLoginAnyplaceBinding?= null
  private val binding get() = _binding!!

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    _binding = ActivityLoginAnyplaceBinding.inflate(layoutInflater)
    setContentView(binding.root)

    app.setMainView(binding.root)
    app.showToast(lifecycleScope, "Please login to Anyplace also!")

    val username = binding.username
    val password = binding.password
    val localLogin = binding.buttonLoginLocal

    VMlogin = ViewModelProvider(this)[AnyplaceLoginViewModel::class.java]
    VMlogin.loginFormState.observe(this@AnyplaceLoginActivity, Observer {
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
            val MT = "registerForActivityResult"
            LOG.E(TG,"$MT: here...")

            result.checkResultAndExecute {

              LOG.E(TG,"$MT: t1")
              val task = GoogleSignIn.getSignedInAccountFromIntent(data)
              val account = task.getResult(ApiException::class.java)
              LOG.E(TG,"$MT: t2")

              if (account.idToken != null && account.displayName != null) {

                LOG.E(TG,"$MT: t4")
                account.displayName?.let { name ->
                  LOG.E(TG, "DIS NAME: " + account.displayName)
                  val googleData = UserLoginGoogleData("google", name, account.idToken)
                  VMlogin.loginUserGoogle(googleData, account.photoUrl)
                } ?: run {
                  throw Exception("can't get user's name")
                }
              } else {
                val msg = "Failed to get Google OAuth Token."
                LOG.E(msg)
                app.showToast(lifecycleScope, msg)
              }
            }.onFailure { e ->
              LOG.E(TG,"$MT: on failure")
              e.printStackTrace()
              LOG.E(TG, "$MT: MSG: ${e.localizedMessage}")
              LOG.E(TG,"$MT: ${e.message}")
              val msg = "Google login failed"
              val errMsg = e.message?.take(200)
              LOG.E(TG, "$MT: $msg: $errMsg")
              Toast.makeText(applicationContext, msg , Toast.LENGTH_LONG).show()
            }
          }

  private fun setupGoogleLogin() {
    val MT = ::setupGoogleLogin.name
    // NOT WORKING..
    // if (app.isNavigator()) {
    //   binding.buttonLoginGoogle.visibility=View.VISIBLE
    // }

    binding.buttonLoginGoogle.setOnClickListener {
      lifecycleScope.launch {
        // server_google_oauth_client_id is set by from local.properties (project root)
        // see lib-android's build.gradle
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(app.getGoogleOAuthClientID())
                // .requestProfile()
                // .requestEmail()
                .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this@AnyplaceLoginActivity, gso)

        val acct = GoogleSignIn.getLastSignedInAccount(this@AnyplaceLoginActivity)
        if (acct != null) {
          LOG.D(TG, "$MT: user was already logged in")
        }
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        googleRegisterResult.launch(signInIntent)
      }
    }
  }

  /**
   * Setup listeners for the username and password editext,
   * changes on the submitted text, enabling/disabling login button, etc
   */
  private fun setupLocalLogin(username: EditText, password: EditText, login: Button) {

    val loading = binding.loading
    username.afterTextChanged {
      VMlogin.loginDataChanged(username.text.toString(), password.text.toString())
    }

    password.apply {
      afterTextChanged {
        VMlogin.loginDataChanged(username.text.toString(), password.text.toString())
      }

      // submitted from keyboard
      setOnEditorActionListener { _, actionId, _ ->
        when (actionId) {
          EditorInfo.IME_ACTION_DONE -> {
            val user = username.text.toString()
            val pass = password.text.toString()
            VMlogin.loginUserLocal(UserLoginLocalForm(user, pass))
          }
        }
        false
      }

      // submitted from button
      login.setOnClickListener {
        loading.visibility = View.VISIBLE
        val user = username.text.toString()
        val pass = password.text.toString()
        VMlogin.loginUserLocal(UserLoginLocalForm(user, pass))
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
    VMlogin.userLoginResponse.observe(this@AnyplaceLoginActivity) { response ->
      LOG.D3(TAG_METHOD, "${response.message}")
      when (response) {
        is NetworkResult.Success -> {
          binding.loading.visibility = View.GONE
          binding.imageViewError.visibility = View.INVISIBLE
          binding.textViewError.visibility = View.INVISIBLE

          // Store user in datastore
          lifecycleScope.launch {
            val user = response.data?.userAP
            user?.let {
              LOG.W(TG, "Logged in successfully: user.name. [storing user]")
              app.dsUserAP.storeUser(user)
              signOutGoogleAuth(user) // for google logins
              openLoggedInActivity()
            }
          }
        }
        is NetworkResult.Error -> {
          LOG.D(TG, "observeUserLoginResponse: err")
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
    }
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
  private fun signOutGoogleAuth(userAP: UserAP) {
    if (userAP.account == "google") {
      mGoogleSignInClient.signOut().addOnCompleteListener {
        LOG.D2(TG, "Signed out Google oauth after successful Anyplace login.")
      }
    }
  }

  private fun openLoggedInActivity() {
    startActivity(Intent(this@AnyplaceLoginActivity, SelectSpaceActivity::class.java))
    finish()
  }

  fun setupSettings() {
    binding.btnSettings.setOnClickListener {
      startActivity(Intent(this, SettingsCvActivity::class.java))
    }
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