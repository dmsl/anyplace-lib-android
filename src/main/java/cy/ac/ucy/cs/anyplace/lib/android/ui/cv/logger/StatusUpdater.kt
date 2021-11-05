package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.extensions.flashOut
import cy.ac.ucy.cs.anyplace.lib.android.extensions.flashView
import kotlinx.coroutines.delay

/**
 * Manages the custom top status bar (tvStatusTitle) in CvLoggerActivity
 */
class StatusUpdater(
  /** TV in the status bar */
  private val tv: TextView,
  /** BG view in the status bar */
  private val tvBg: View,
  /** Overlay View on top of the others.  It may appear briefly to emphasize text. */
  private val overlay: View,
  private val ctx: Context) {

  companion object {
    fun ColorWarning(ctx: Context) =ContextCompat.getColor(ctx, R.color.yellowDark)
    fun ColorPrimaryDark(ctx: Context) =ContextCompat.getColor(ctx, R.color.colorPrimaryDark)
    fun ColorError(ctx: Context) =ContextCompat.getColor(ctx, R.color.redDark)
  }

  fun hideStatus() {
    tv.visibility = View.INVISIBLE
    tvBg.visibility = View.INVISIBLE
  }

  fun showError(text: String) {
    tv.setTextColor(ColorError(ctx))
    tvBg.setBackgroundColor(ColorError(ctx))
    showMessage(text)
    tvBg.flashOut(0.7f)
  }

  fun showWarning(text: String) {
    tv.setTextColor(ColorWarning(ctx))
    tvBg.setBackgroundColor(ColorWarning(ctx))
    showMessage(text)
    tvBg.flashOut(0.7f)
    overlay.flashView(500)
  }

  suspend fun showWarningAutohide(text: String) {
    showWarning(text)
    delay(500)
    hideStatus()
  }


  fun showNormal(text: String) {
    tv.setTextColor(ColorPrimaryDark(ctx))
    showMessage(text)
  }

  private fun showMessage(text: String) {
    tv.visibility = View.VISIBLE
    tv.text = text
  }


}
