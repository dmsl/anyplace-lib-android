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
 *
 * TODO:PM if something else is showing: then hide it
 */
class StatusUpdater(
  /** TV in the status bar */
  private val tvTitle: TextView,
  private val tvSubtitle: TextView,
  /** BG view in the status bar */
  private val tvBg: View,
  /** Overlay View on top of the others.  It may appear briefly to emphasize text. */
  private val overlay: View,
  private val ctx: Context) {

  companion object {
    fun ColorWhiteB0(ctx: Context) = ContextCompat.getColor(ctx, R.color.white_B0)
    fun ColorWhite(ctx: Context) = ContextCompat.getColor(ctx, R.color.white)
    fun ColorWarning(ctx: Context) =ContextCompat.getColor(ctx, R.color.yellowDark)
    fun ColorPrimaryDark(ctx: Context) =ContextCompat.getColor(ctx, R.color.colorPrimaryDark)
    fun ColorError(ctx: Context) =ContextCompat.getColor(ctx, R.color.redDark)
  }

  fun hideStatus() {
    tvTitle.visibility = View.INVISIBLE
    tvSubtitle.visibility = View.INVISIBLE
    tvBg.visibility = View.INVISIBLE
  }

  fun showError(text: String) {
    tvTitle.setTextColor(ColorError(ctx))
    tvBg.setBackgroundColor(ColorError(ctx))
    showMessage(text)
    tvBg.flashOut(0.7f)
  }

  fun showWarning(text: String, delay: Long = 500) {
    tvTitle.setTextColor(ColorWarning(ctx))
    tvBg.setBackgroundColor(ColorWarning(ctx))
    showMessage(text)
    tvBg.flashOut(0.7f, delay)
    overlay.flashView(delay)
  }


  suspend fun showWarningAutohide(title: String, delay: Long = 500) {
    tvTitle.setTextColor(ColorWarning(ctx))
    tvBg.setBackgroundColor(ColorWhite(ctx))
    showMessage(title)
    overlay.flashView(delay)
    delay(delay)
    // tvBg.flashOut(0.7f, 500)
    // tv.flashOut(0.7f, 500)
    hideStatus()
  }


  suspend fun showWarningAutohide(title: String, subtitle: String, delay: Long = 500) {
    tvTitle.setTextColor(ColorWarning(ctx))
    tvSubtitle.setTextColor(ColorWarning(ctx))
    tvBg.setBackgroundColor(ColorWhite(ctx))
    showMessage(title, subtitle)
    overlay.flashView(delay)
    delay(delay)
    hideStatus()
  }

  fun showNormal(text: String) {
    tvTitle.setTextColor(ColorPrimaryDark(ctx))
    showMessage(text)
  }

  private fun showMessage(title: String) {
    tvTitle.text = title
    tvTitle.alpha=1f
    tvBg.alpha=1f

    tvBg.visibility=View.VISIBLE
    tvTitle.visibility = View.VISIBLE
  }

  private fun showMessage(title: String, subtitle: String) {
    showMessage(title)
    tvSubtitle.text = subtitle
    tvSubtitle.alpha=1f
    tvSubtitle.visibility = View.VISIBLE
  }


}
