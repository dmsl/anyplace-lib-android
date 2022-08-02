package cy.ac.ucy.cs.anyplace.lib.android.utils

import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


enum class SnackType {
  NORMAL,
  INFO,
  WARNING,
  DEV
}

/**
 * Utility methods around [Snackbar] to provide notifications
 *
 * It supports:
 * - dev mode notifications
 * - info notifications
 * - warning
 * - long, short, infinity notifications, requiring using user ACK
 *
 * Convention:
 * - when capital: it's [Snackbar.LENGTH_INDEFINITE]
 */
class UtilSnackBarNotifier(val app: AnyplaceApp) {
  var snackbarForChat = false
  lateinit var rootView: View

  /** Short notification */
  fun short(scope: CoroutineScope, msg: String) = show(scope, msg, Snackbar.LENGTH_SHORT)
  /** Long notification */
  fun long(scope: CoroutineScope, msg: String) = show(scope, msg, Snackbar.LENGTH_LONG)
  /** Indefinite notification */
  fun INF(scope: CoroutineScope, msg: String) = show(scope, msg, Snackbar.LENGTH_INDEFINITE)
  /** Long Warning notification */
  fun warn(scope: CoroutineScope, msg: String) = show(scope, msg, Snackbar.LENGTH_LONG, SnackType.WARNING)
  /** Indefinite Warning notification */
  fun WARN(scope: CoroutineScope, msg: String) = show(scope, msg, Snackbar.LENGTH_INDEFINITE, SnackType.WARNING)
  /** Long Info notification */
  fun info(scope: CoroutineScope, msg: String) = show(scope, msg, Snackbar.LENGTH_LONG, SnackType.INFO)
  /** Indefinite Info notification */
  fun INFO(scope: CoroutineScope, msg: String) = show(scope, msg, Snackbar.LENGTH_INDEFINITE, SnackType.INFO)

  /** Short Dev notification */
  fun shortDEV(scope: CoroutineScope, msg: String) = showDEV(scope, msg, Snackbar.LENGTH_SHORT)
  /** Long Dev notification */
  fun longDEV(scope: CoroutineScope, msg: String) = showDEV(scope, msg, Snackbar.LENGTH_LONG)
  /** Indefinite Dev notification */
  fun DEV(scope: CoroutineScope, msg: String) = showDEV(scope, msg, Snackbar.LENGTH_INDEFINITE)

  fun showDEV(scope: CoroutineScope,
              msg: String,
              duration: Int = Snackbar.LENGTH_SHORT) {
    if (!DBG.DVO) {
      app.showToast(scope, msg, Toast.LENGTH_SHORT)
      return
    }

    scope.launch(Dispatchers.IO) {
      if(app.hasDevMode()) {
        show(scope, msg, duration, SnackType.DEV)
      }
    }
  }

  fun show(scope: CoroutineScope, msg: String,
           duration: Int, type: SnackType = SnackType.NORMAL) {

    if (!DBG.DVO) {
      app.showToast(scope, msg, Toast.LENGTH_SHORT)
      return
    }

    scope.launch(Dispatchers.Main) {
      val sb = Snackbar.make(rootView, msg, duration)
      sb.setActionTextColor(app.utlColor.White())

      if (duration != Snackbar.LENGTH_SHORT || type == SnackType.DEV) {
        sb.setAction("OK") { } // dismissible
      }

      // center text
      val tv = sb.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
      tv.gravity = Gravity.CENTER
      tv.setTypeface(tv.typeface, Typeface.BOLD)
      if (snackbarForChat) {
        sb.setMarginChat()
      } else {
        sb.setMarginCvMap()
      }

      tv.textAlignment = View.TEXT_ALIGNMENT_GRAVITY
      tv.maxLines=3

      when (type) {
        SnackType.DEV -> {
          sb.setDrawableLeft(R.drawable.ic_dev_mode)
          sb.setBackground(R.drawable.bg_snackbar_devmode)
          sb.setActionTextColor(app.utlColor.GrayLighter())
        }
        SnackType.WARNING -> {
          sb.setBackground(R.drawable.bg_snackbar_warning)
          sb.setActionTextColor(app.utlColor.Info())

          if (duration == Snackbar.LENGTH_INDEFINITE) {
            sb.setDrawableLeft(R.drawable.ic_warning)
          } else {
            sb.setDrawableLeft(R.drawable.ic_empty) // workaround for horizontal alignment
          }
        }
        SnackType.INFO -> {
          sb.setBackground(R.drawable.bg_snackbar_info)
          sb.setActionTextColor(app.utlColor.Info())
          sb.setDrawableLeft(R.drawable.ic_info)
        }
        SnackType.NORMAL -> {
          sb.setBackground(R.drawable.bg_snackbar_normal)
          sb.setActionTextColor(app.utlColor.Info())

          if (duration == Snackbar.LENGTH_INDEFINITE) {
            sb.setDrawableLeft(R.drawable.ic_info)
          } else {
            sb.setDrawableLeft(R.drawable.ic_empty) // workaround for horizontal alignment
          }
        }
      }


      sb.setIconTint(app.utlColor.White())
      sb.show()
    }
  }

}