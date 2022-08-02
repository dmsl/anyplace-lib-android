package cy.ac.ucy.cs.anyplace.lib.android.utils

import com.google.gson.stream.MalformedJsonException
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.di.RetrofitHolderBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.ConnectException

class UtilErr() {
  private val TG = "utl-exception"
  companion object {
    private val MAX_CONSECUTIVE_SAME_ERRORS: Int = 5
  }

  fun handle(
    app: AnyplaceApp,
    RHB: RetrofitHolderBase,
    scope: CoroutineScope, e: Exception?,
    tag: String="") : String {
    val MT = ::handle.name

    var resultMsg = tag
    if (e == null) {
      resultMsg="$tag: Something went wrong"
    }

    if (e != null)  {
      val errorMsg = when (e) {
        is MalformedJsonException -> "Malformed server answer (json/$tag)"
        is ConnectException -> "Connection failed ($tag) \n${RHB.retrofit.baseUrl()}"
        else -> {
          "Something went wrong ($tag/${e.javaClass}/${e.message})"
        } // generic
      }

      LOG.E(TG, e.stackTraceToString())
      resultMsg="$MT: $errorMsg" // augment msg with exception info
    }
    app.notify.DEV(scope, "$MT: resultMsg")
    LOG.E(TG, "$MT: resultMsg")

    return resultMsg
  }

  var errCnt = 0
  var errNoInternetShown = false
  var prevErrorMsg = ""
  /**
   * Dont flood user with error messages:
   *
   * When short polling, we might get the same error again and again.
   * To avoid spamming the users we use this functionality
   */
  fun handlePollingRequest(app: AnyplaceApp, scope: CoroutineScope, msg: String) {
    val MT = ::handlePollingRequest.name
    // avoid showing same error again
    if (errCnt <MAX_CONSECUTIVE_SAME_ERRORS && msg != prevErrorMsg) {
      prevErrorMsg=msg
      errCnt=0 // different error
      app.notify.WARN(scope, msg)
      LOG.W(TG, "$TG: $msg")

      // dismiss the message asnyc, after 10 seconds
      scope.launch(Dispatchers.IO) {
        delay(1000*10L)
        prevErrorMsg=""
      }
    } else {
      errCnt++
      if(errCnt>=MAX_CONSECUTIVE_SAME_ERRORS) errCnt=MAX_CONSECUTIVE_SAME_ERRORS
      LOG.E(TG, "$MT: [SUPPRESSING ERR MSGS]")
      LOG.E(TG, "$MT: $msg")
    }
  }
}