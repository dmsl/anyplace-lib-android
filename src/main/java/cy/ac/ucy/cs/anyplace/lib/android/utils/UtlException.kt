package cy.ac.ucy.cs.anyplace.lib.android.utils

import com.google.gson.stream.MalformedJsonException
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.net.RetrofitHolderBase
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw.CvLocalizeNW
import kotlinx.coroutines.CoroutineScope
import java.net.ConnectException

object utlException {

  fun handleException(
          app: AnyplaceApp,
          RHB: RetrofitHolderBase,
          scope: CoroutineScope, e: Exception?,
          tag: String="") : String {

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

      LOG.E(TAG, e.stackTraceToString())
      resultMsg="$errorMsg" // augment msg with exception info
    }

    app.showToastDEV(scope, resultMsg)
    LOG.E(TAG, resultMsg)

    return resultMsg
  }
}