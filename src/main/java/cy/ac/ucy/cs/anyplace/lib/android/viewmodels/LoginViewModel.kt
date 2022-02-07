package cy.ac.ucy.cs.anyplace.lib.android.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.data.Repository
import cy.ac.ucy.cs.anyplace.lib.android.data.LoginFormState
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.utils.network.RetrofitHolder
import cy.ac.ucy.cs.anyplace.lib.models.UserLoginGoogleData
import cy.ac.ucy.cs.anyplace.lib.models.UserLoginLocalForm
import cy.ac.ucy.cs.anyplace.lib.models.UserLoginResponse
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.Response
import java.lang.Exception
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
  application: Application,
  private val repository: Repository,
  private val retrofitHolder: RetrofitHolder) : AndroidViewModel(application) {

  private val C by lazy { CONST(application.applicationContext) }
  private val _loginForm = MutableLiveData<LoginFormState>()
  val loginFormState: LiveData<LoginFormState> = _loginForm // CHECK:PM

  //// RETROFIT
  ////// Mutable Data
  val userLoginResponse: MutableLiveData<NetworkResult<UserLoginResponse>> = MutableLiveData()

  fun loginUserLocal(obj: UserLoginLocalForm) =
    viewModelScope.launch { loginLocalUserSafeCall(obj) }

  fun loginUserGoogle(obj: UserLoginGoogleData, photoUri: Uri?) =
    viewModelScope.launch { loginGoogleUserSafeCall(obj, photoUri) }

  private suspend fun loginLocalUserSafeCall(userLoginLocalForm: UserLoginLocalForm) {
    userLoginResponse.value = NetworkResult.Loading() // TODO
    var exception : Exception? = null
    // var userResponse : NetworkResult<UserResponse>? = null
      if (app.hasInternetConnection()) {
      try {
        val response = repository.remote.userLoginLocal(userLoginLocalForm)
        userLoginResponse.value = handleUserLoginResponse(response, null)

        if (userLoginResponse.value is NetworkResult.Error) {
         exception = Exception(userLoginResponse.value?.message)
        // } else { // CLR? isnt this cached already?
        //   // TODO:PM
        //   LOG.W("TODO: cache logged in user")
        //   LOG.W("$userLoginResponse.value.data")
        }
      } catch(e: Exception) {
        exception = e
        val msg = "Login failed"
        handleSafecallError(msg, e)
        e.let {
          if (e.message?.contains(C.EXCEPTION_MSG_HTTP_FORBIDEN) == true) {
            exception = Exception(C.MSG_ERR_ONLY_SSL)
          } else if (e.message?.contains(C.EXCEPTION_MSG_ILLEGAL_STATE) == true) {
            exception = Exception(C.MSG_ERR_ILLEGAL_STATE)
          } else if (e.message?.contains(C.EXCEPTION_MSG_NPE) == true) {
            exception = Exception(C.MSG_ERR_NPE)
          }
        }
        LOG.E(TAG, "Exception: ${exception!!.message}")
      }
      } else {
        exception = Exception("No Internet Connection.")
      }

    exception?.let { it ->
      val msg = it.message.toString()
      LOG.E(TAG, msg)
      userLoginResponse.value = NetworkResult.Error(exception?.message)
    }
  }

  // CHECK this is almost identical with: loginLocalUserSafeCall
  private suspend fun loginGoogleUserSafeCall(obj: UserLoginGoogleData, photoUri: Uri?) {
    userLoginResponse.value = NetworkResult.Loading()
    var exception : Exception? = null
    if (app.hasInternetConnection()) {
      try {
        val response = repository.remote.userLoginGoogle(obj)
        userLoginResponse.value = handleUserLoginResponse(response, photoUri)

        if (userLoginResponse.value is NetworkResult.Error) {
          exception = Exception(userLoginResponse.value?.message)
        }
      } catch(e: Exception) {
        exception = e
        val msg = "Google Login failed"
        handleSafecallError(msg, e)
        e.let {
          if (e.message?.contains(C.EXCEPTION_MSG_HTTP_FORBIDEN) == true) {
            exception = Exception(C.MSG_ERR_ONLY_SSL)
          } else if (e.message?.contains(C.EXCEPTION_MSG_ILLEGAL_STATE) == true) {
            exception = Exception(C.MSG_ERR_ILLEGAL_STATE)
          } else if (e.message?.contains(C.EXCEPTION_MSG_NPE) == true) {
            exception = Exception(C.MSG_ERR_NPE)
          }
        }
        LOG.E(TAG, "Exception: ${exception!!.message}")
      }
    } else {
      exception = Exception("No Internet Connection.")
    }

    exception?.let { it ->
      val msg = it.message.toString()
      LOG.E(TAG, msg)
      userLoginResponse.value = NetworkResult.Error(exception?.message)
    }
  }

  private fun handleSafecallError(msg: String, e: Exception) {
    if (e is IllegalStateException) {
      userLoginResponse.value = NetworkResult.Error(e.javaClass.name)
    } else {
      userLoginResponse.value = NetworkResult.Error(msg)
    }
    LOG.E(msg)
    LOG.E(TAG, e)
  }

  private fun handleUserLoginResponse(response: Response<UserLoginResponse>, photoUri: Uri?): NetworkResult<UserLoginResponse> {
    return when {
      response.message().toString().contains("timeout") -> NetworkResult.Error("Timeout.")
      !response.isSuccessful-> {
        val rawErrorResponse = response.errorBody()!!.string()
        val errorReponse = Gson().fromJson(rawErrorResponse, UserLoginResponse::class.java)
        NetworkResult.Error(errorReponse.message)
      }
      response.isSuccessful -> {
        val user = response.body()!!
        photoUri?.let { user.user.photoUri=photoUri.toString() }
        NetworkResult.Success(user)
      } // can be nullable
      else -> NetworkResult.Error(response.message())
    }
  }

  fun loginDataChanged(username: String, password: String) {
    if (!isUserNameValid(username)) {
      _loginForm.value = LoginFormState(usernameError = R.string.invalid_username)
    } else if (!isPasswordValid(password)) {
      _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
    } else {
      _loginForm.value = LoginFormState(isDataValid = true)
    }
  }

  private fun isUserNameValid(username: String): Boolean {
    return username.isNotBlank()
    // return if (username.contains('@')) {
    //   Patterns.EMAIL_ADDRESS.matcher(username).matches()
    // } else {
    //   username.isNotBlank()
    // }
  }

  private fun isPasswordValid(password: String): Boolean {
    return password.length > 3
  }
}