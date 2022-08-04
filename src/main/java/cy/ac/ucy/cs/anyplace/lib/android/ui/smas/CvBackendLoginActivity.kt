package cy.ac.ucy.cs.anyplace.lib.android.ui.smas

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.extensions.afterTextChanged
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.extensions.notify
import cy.ac.ucy.cs.anyplace.lib.android.ui.BaseActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.StartActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.smas.models.SmasLoginReq
import cy.ac.ucy.cs.anyplace.lib.smas.models.SmasUser
import cy.ac.ucy.cs.anyplace.lib.android.ui.settings.smas.SettingsChatActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilColor
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.UtilUI
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasLoginViewModel
import cy.ac.ucy.cs.anyplace.lib.databinding.ActivityCvbackendLoginBinding
import cy.ac.ucy.cs.anyplace.lib.databinding.ActivitySmasLoginBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.annotations.TestOnly


/**
 * Copied from [SmasLoginActivity], with minor UI adjustments.
 *
 * SMAS was renamed to CvBackend
 */
@AndroidEntryPoint
class CvBackendLoginActivity: BaseActivity() {
  val TG = "act-login-cvbackend"

  private lateinit var VM: SmasLoginViewModel
  private lateinit var VMcv: CvViewModel
  private var _binding: ActivityCvbackendLoginBinding?= null
  private val binding get() = _binding!!

  private val utlButton by lazy { UtilUI(applicationContext, lifecycleScope) }
  private val C by lazy { CONST(applicationContext) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    _binding = ActivityCvbackendLoginBinding.inflate(layoutInflater)
    setContentView(binding.root)

    app.setMainView(binding.root)

    val username = binding.username
    val password = binding.password

    VM= ViewModelProvider(this)[SmasLoginViewModel::class.java]
    VMcv= ViewModelProvider(this)[CvViewModel::class.java]
    VM.loginFormState.observe(this@CvBackendLoginActivity, Observer {
      if (it == null) return@Observer
      val loginState = it

      // disable login button unless both username / password is valid
      binding.btnLogin.isEnabled = loginState.isDataValid

      if (loginState.usernameError != null) {
        username.error = getString(loginState.usernameError)
      }
      if (loginState.passwordError != null) {
        password.error = getString(loginState.passwordError)
      }
    })

    setupButtonSettings()
    setupSmasLogin(username, password)
    collectLogin()
  }

  /**
   * Setup listeners for the username and password editext,
   * changes on the submitted text, enabling/disabling login button, etc
   */
  private fun setupSmasLogin(username: EditText, password: EditText) {
    val loading = binding.loading
    username.afterTextChanged {
      VM.loginDataChanged(username.text.toString(), password.text.toString())
    }

    password.apply {
      afterTextChanged {
        VM.loginDataChanged(username.text.toString(), password.text.toString())
      }

      // submitted from keyboard
      setOnEditorActionListener { _, actionId, _ ->
        when (actionId) {
          EditorInfo.IME_ACTION_DONE -> {
            val user = username.text.toString()
            val pass = password.text.toString()
            setLoginButtonLoading()
            VM.login(SmasLoginReq(user, pass))
          }
        }
        false
      }

      // submitted from button
      binding.btnLogin.setOnClickListener {
        loading.visibility = View.VISIBLE
        val user = username.text.toString()
        val pass = password.text.toString()
        setLoginButtonLoading()
        VM.login(SmasLoginReq(user, pass))
      }

      setOnTouchListener { _, event ->
        // val DRAWABLE_LEFT = 0
        // val DRAWABLE_TOP = 1
        val DRAWABLE_RIGHT = 2
        // val DRAWABLE_BOTTOM = 3

        val eventOnRightDrawable = event.rawX >= right - compoundDrawables[DRAWABLE_RIGHT].bounds.width()
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

  private fun setLoginButtonLoading() {
    val btn = binding.btnLogin
    btn.isEnabled = false
    utlButton.changeBackgroundCompat(btn, R.color.darkGray)
  }

  private fun unsetLoginButtonLoading() {
    val btn = binding.btnLogin
    btn.isEnabled = true
    utlButton.changeBackgroundCompat(btn, R.color.colorPrimary)
  }

  /**
   * Works with both local (anyplace) login and Google login,
   * as the backend returns the same, compatible user object
   */
  @SuppressLint("SetTextI18n")
  private fun collectLogin() {
    val method = ::collectLogin.name
    lifecycleScope.launch {
      VM.resp.collect { response ->
        unsetLoginButtonLoading()
        // observe(this@SmasLoginActivity) { response ->
        LOG.D3(TG, "$method: resp: ${response.message}")
        when (response) {
          is NetworkResult.Success -> {
            // binding.loading.visibility = View.GONE
            binding.imageViewError.visibility = View.INVISIBLE
            binding.textViewError.visibility = View.INVISIBLE

            lifecycleScope.launch(Dispatchers.IO) {
              // Store user in datastore
              val user = response.data
              user?.let {
                app.dsUserSmas.storeUser(SmasUser(user.uid, user.sessionkey))
                if (!DBG.SLR) {
                  VMcv.nwCvModelFilesGet.downloadMissingModels()
                  lifecycleScope.launch(Dispatchers.Main) {
                    binding.textViewError.text = "Downloading CvModels..."
                    binding.textViewError.visibility = View.VISIBLE
                    binding.textViewError.setTextColor(UtilColor(applicationContext).Black())
                  }
                }
                openLoggedInActivity()
              }
            }
          }
          is NetworkResult.Error -> {
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
          is NetworkResult.Unset -> { } // ignore
        }
      }
    } // coroutine
  }


  private fun openLoggedInActivity() {
    lifecycleScope.launch (Dispatchers.Main) {
      val prefsCv = app.dsCvMap.read.first()
      val userAP = app.dsUserAP.read.first()
      StartActivity.openActivity(prefsCv, userAP, this@CvBackendLoginActivity)
    }
  }

  private fun setupButtonSettings() {
    binding.btnSettings.setOnClickListener {
      startActivity(Intent(this@CvBackendLoginActivity, SettingsChatActivity::class.java))
    }
  }

  @TestOnly
  private fun loginProgrammatically() {
    val MT = ::loginProgrammatically.name

    // BUG: how login affects communicating w/ version?
    // done for a different account..
    LOG.W(TG, MT)
    // val demoUser = ChatUserLoginForm(BuildConfig.LASH_DEMO_LOGIN_UID, BuildConfig.LASH_DEMO_LOGIN_PASS)
    val demoUser = SmasLoginReq("username", "password")
    VM.login(demoUser)
    lifecycleScope.launch {
      VM.resp.collect {
        LOG.D(TG, "$MT: Logged in user: ${it.data?.sessionkey}")
        LOG.D(TG, "$MT: descr: ${it.data?.descr}")
        if (it is NetworkResult.Error) {
          LOG.E(TG, "$MT: LOGIN ERROR.")
        }
      }
    }
  }
}