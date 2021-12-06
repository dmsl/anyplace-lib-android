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
 * Manages the custom top status bar in CvLoggerActivity:
 * - [tvStatus]: sticky status msg, either shown or hidden
 * - [tvMsgTitle]: title of a temporary message
 * - [tvMsgSubtitle]: details of the [tvMsgTitle]
 *
 * A msg has a [Level].
 *
 * TODO:PM if something else is showing: then hide it
 */
class StatusUpdater(
        private val ctx: Context,
        /** Stick mode in the status bar,
         * for example setting 'Localization' Mode
         */
        private val tvStatus: TextView,
        /** Message Title in the status bar */
        private val tvMsgTitle: TextView,
        /** Message Title in the status bar */
        private val tvMsgSubtitle: TextView,
        /** BG view in the status bar */
        private val tvBg: View,
        /** Overlay View on top of the others.  It may appear briefly to emphasize text. */
        private val overlay: View) {

  enum class Level {
    Warning,
    Info,
    Error
  }

  companion object {
    fun ColorWhiteB0(ctx: Context) = ContextCompat.getColor(ctx, R.color.white_B0)
    fun ColorWhite(ctx: Context) = ContextCompat.getColor(ctx, R.color.white)
    fun ColorWarning(ctx: Context) =ContextCompat.getColor(ctx, R.color.yellowDark)
    fun ColorInfo(ctx: Context) =ContextCompat.getColor(ctx, R.color.holo_light_blue)
    fun ColorPrimaryDark(ctx: Context) =ContextCompat.getColor(ctx, R.color.colorPrimaryDark)
    fun ColorError(ctx: Context) =ContextCompat.getColor(ctx, R.color.redDark)

    fun Color(level: Level, ctx: Context): Int {
      return when(level) {
        Level.Warning -> ColorWarning(ctx)
        Level.Error -> ColorError(ctx)
        Level.Info -> ColorInfo(ctx)
      }
    }

  }

  fun hideStatus() {
    tvMsgTitle.visibility = View.INVISIBLE
    tvMsgSubtitle.visibility = View.INVISIBLE
    tvBg.visibility = View.INVISIBLE
  }

  fun setStatus(text: String) {
    hideStatus()
    tvStatus.text = text
    tvStatus.visibility = View.VISIBLE

    val color= Color(Level.Info, ctx)
    overlay.setBackgroundColor(color)
    overlay.flashView(1000)
  }

  fun clearStatus() {
    tvStatus.visibility = View.GONE
  }

  fun showError(text: String) {
    tvMsgTitle.setTextColor(ColorError(ctx))
    tvBg.setBackgroundColor(ColorError(ctx))
    showMessage(text)
    tvBg.flashOut(0.7f)
  }

  fun showWarning(text: String, delay: Long = 500) {
    tvMsgTitle.setTextColor(ColorWarning(ctx))
    tvBg.setBackgroundColor(ColorWarning(ctx))
    showMessage(text)
    tvBg.flashOut(0.7f, delay)
    overlay.flashView(delay)
  }

  suspend fun showErrorAutohide(title: String, delay: Long = 500) {
    showMsgAutohide(Level.Error, title, delay)
  }
  suspend fun showErrorAutohide(title: String, subtitle: String, delay: Long = 500) {
    showMsgAutohide(Level.Error, title, subtitle, delay)
  }

  suspend fun showInfoAutohide(title: String, delay: Long = 500) {
    showMsgAutohide(Level.Info, title, delay)
  }

  suspend fun showInfoAutohide(title: String, subtitle: String, delay: Long = 500) {
    showMsgAutohide(Level.Info, title, subtitle, delay)
  }

  suspend fun showWarningAutohide(title: String, delay: Long = 500) {
    showMsgAutohide(Level.Warning, title, delay)
  }

  suspend fun showMsgAutohide(level: Level, title: String, delay: Long) {
    val color= Color(level, ctx)
    overlay.setBackgroundColor(color)
    tvMsgTitle.setTextColor(color)

    tvBg.setBackgroundColor(ColorWhite(ctx))
    showMessage(title)
    overlay.flashView(delay)
    delay(delay)
    hideStatus()
  }

  suspend fun showWarningAutohide(title: String, subtitle: String, delay: Long = 500) {
    showMsgAutohide(Level.Warning, title, subtitle, delay)
  }

  suspend fun showMsgAutohide(level:Level, title: String, subtitle: String, delay: Long) {
    val color = Color(level, ctx)
    tvMsgSubtitle.setTextColor(color)
    tvMsgTitle.setTextColor(color)
    overlay.setBackgroundColor(color)

    tvBg.setBackgroundColor(ColorWhite(ctx))
    showMessage(title, subtitle)
    overlay.flashView(delay)
    delay(delay)
    hideStatus()
  }

  fun showNormal(text: String) {
    tvMsgTitle.setTextColor(ColorPrimaryDark(ctx))
    showMessage(text)
  }

  private fun showMessage(title: String) {
    tvMsgTitle.text = title
    tvMsgTitle.alpha=1f
    tvBg.alpha=1f

    tvBg.visibility=View.VISIBLE
    tvMsgTitle.visibility = View.VISIBLE
  }

  private fun showMessage(title: String, subtitle: String) {
    showMessage(title)
    tvMsgSubtitle.text = subtitle
    tvMsgSubtitle.alpha=1f
    tvMsgSubtitle.visibility = View.VISIBLE
  }

}
