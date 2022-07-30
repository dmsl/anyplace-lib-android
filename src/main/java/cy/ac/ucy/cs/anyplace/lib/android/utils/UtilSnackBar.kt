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

class UtilSnackBar(val app: AnyplaceApp) {
  var snackbarForChat = false
  lateinit var rootView: View

  fun show(scope: CoroutineScope, msg: String,
           duration: Int, devMode : Boolean = false) {

    if (!DBG.DVO) {
      app.showToast(scope, msg, Toast.LENGTH_SHORT)
      return
    }

    scope.launch(Dispatchers.Main) {
      val sb = Snackbar.make(rootView, msg, duration)
      sb.setActionTextColor(app.utlColor.White())

      if (duration != Snackbar.LENGTH_SHORT || devMode) {
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

      if (devMode) {
        sb.setDrawableLeft(R.drawable.ic_dev_mode)
        sb.setBackground(R.drawable.bg_snackbar_devmode)
        sb.setActionTextColor(app.utlColor.GrayLighter())
      } else {
        sb.setBackground(R.drawable.bg_snackbar_normal)
        sb.setActionTextColor(app.utlColor.Info())

        if (duration == Snackbar.LENGTH_INDEFINITE) {
          sb.setDrawableLeft(R.drawable.ic_info)
        } else {
          sb.setDrawableLeft(R.drawable.ic_empty) // workaround for horizontal alignment
        }
      }

      sb.setIconTint(app.utlColor.White())
      sb.show()
    }
  }

}