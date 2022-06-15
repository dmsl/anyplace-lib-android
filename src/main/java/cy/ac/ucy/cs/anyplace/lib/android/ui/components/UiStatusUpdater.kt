package cy.ac.ucy.cs.anyplace.lib.android.ui.components

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.extensions.flashOut
import cy.ac.ucy.cs.anyplace.lib.android.extensions.flashView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
class UiStatusUpdater(
        private val ctx: Context,
        private val scope: CoroutineScope,
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
    Normal,
    Error
  }

  init {
    setHideOnClick()
  }

  companion object { // TODO: in utlColor object
    fun ColorWhiteB0(ctx: Context) = ContextCompat.getColor(ctx, R.color.white_B0)
    fun ColorWhite(ctx: Context) = ContextCompat.getColor(ctx, R.color.white)
    fun ColorWarning(ctx: Context) =ContextCompat.getColor(ctx, R.color.yellowDark)
    fun ColorInfo(ctx: Context) =ContextCompat.getColor(ctx, R.color.holo_light_blue)
    fun ColorNormal(ctx: Context) =ContextCompat.getColor(ctx, R.color.black)
    fun ColorPrimaryDark(ctx: Context) =ContextCompat.getColor(ctx, R.color.colorPrimaryDark)
    fun ColorPrimary(ctx: Context) =ContextCompat.getColor(ctx, R.color.colorPrimary)
    fun ColorError(ctx: Context) =ContextCompat.getColor(ctx, R.color.redDark)
    fun ColorYellowDark(ctx: Context) =ContextCompat.getColor(ctx, R.color.yellowDark)
    fun ColorBlueDark(ctx: Context) =ContextCompat.getColor(ctx, R.color.lash_blue_dark)

    fun Color(level: Level, ctx: Context): Int {
      return when(level) {
        Level.Warning -> ColorWarning(ctx)
        Level.Error -> ColorError(ctx)
        Level.Info -> ColorInfo(ctx)
        Level.Normal -> ColorNormal(ctx)
      }
    }
  }

  private fun setHideOnClick() {
    tvBg.setOnClickListener {
      hideStatus()
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

  fun clearStatus() { tvStatus.visibility = View.GONE}

  fun showNormal(text: String, delay: Long = 500) = showMsgPersistent(Level.Normal, text, delay)
  fun showError(text: String, delay: Long = 500) = showMsgPersistent(Level.Error, text, delay)
  fun showWarning(text: String, delay: Long = 500) = showMsgPersistent(Level.Warning, text)
  // { // OLD CODE. CLR:PM ?
  // CLR:PM
  // tvMsgTitle.setTextColor(ColorWarning(ctx))
  // tvBg.setBackgroundColor(ColorWarning(ctx))
  // setMessageTitle(Level.Warning, text)
  // tvBg.flashOut(0.7f, delay)
  // overlay.flashView(delay)
  // }


  private fun clearMessages() {
    tvMsgSubtitle.text=""
    tvMsgTitle.text=""
  }

  private fun showMsgPersistent(level: Level, text: String, delay: Long = 500) {
    clearMessages()
    setMessageTitle(Level.Warning, text)
    tvBg.flashOut(0.7f, delay)
    overlay.flashView(delay)
  }


  fun showErrorAutohide(title: String, delay: Long = 500) = showMsgAutohide(Level.Error, title, delay)
  fun showErrorAutohide(title: String, subtitle: String, delay: Long = 500) = showMsgAutohide(Level.Error, title, subtitle, delay)
  fun showNormalAutohide(title: String, delay: Long = 500) = showMsgAutohide(Level.Normal, title, delay)
  fun showNormalAutohide(title: String, subtitle: String, delay: Long = 500) = showMsgAutohide(Level.Normal, title, subtitle, delay)
  fun showInfoAutohide(title: String, delay: Long = 500) = showMsgAutohide(Level.Info, title, delay)
  fun showInfoAutohide(title: String, subtitle: String, delay: Long = 500) = showMsgAutohide(Level.Info, title, subtitle, delay)
  fun showWarningAutohide(title: String, delay: Long = 500) = showMsgAutohide(Level.Warning, title, delay)
  fun showWarningAutohide(title: String, subtitle: String, delay: Long = 500) = showMsgAutohide(Level.Warning, title, subtitle, delay)

  private fun showMsgAutohide(level: Level, title: String, delay: Long) {
    scope.launch {
      clearMessages()
      tvBg.setBackgroundColor(ColorWhite(ctx))
      setMessageTitle(level, title)
      overlay.flashView(delay)
      delay(delay)
      hideStatus()
    }
  }

  private fun showMsgAutohide(level: Level, title: String, subtitle: String, delay: Long) {
    // TODO: make only the updates on the Main thread?
    scope.launch {
      clearMessages()
      tvBg.setBackgroundColor(ColorWhite(ctx))
      setMessageTitle(level, title, subtitle)
      overlay.flashView(delay)
      delay(delay)
      hideStatus()
    }
  }

  private fun setMessageTitle(level: Level, title: String, subtitle: String) {
    setMessageTitle(level, title)
    setMessageSubtitle(level, subtitle)
  }

  private fun setMessageSubtitle(level: Level, subtitle: String) {
    val color= Color(level, ctx)
    tvMsgSubtitle.setTextColor(color)
    tvMsgSubtitle.text = subtitle
    tvMsgSubtitle.alpha=1f
    tvMsgSubtitle.visibility = View.VISIBLE
  }

  private fun setMessageTitle(level: Level, title: String) {
    val color= Color(level, ctx)
    overlay.setBackgroundColor(color)
    tvMsgTitle.setTextColor(color)

    tvMsgTitle.text = title
    tvMsgTitle.alpha=1f
    tvBg.alpha=1f

    tvBg.visibility=View.VISIBLE
    tvMsgTitle.visibility = View.VISIBLE
  }

}
