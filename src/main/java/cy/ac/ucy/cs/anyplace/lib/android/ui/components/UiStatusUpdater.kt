package cy.ac.ucy.cs.anyplace.lib.android.ui.components

import android.content.Context
import android.view.View
import android.widget.TextView
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilColor
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.UtilUI
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

  init { setHideOnClick() }

  private val utlColor by lazy { UtilColor(ctx)}
  private val utlUi by lazy { UtilUI(ctx, scope) }

  fun Color(level: Level): Int {
    return when(level) {
      Level.Warning -> utlColor.ColorWarning()
      Level.Error -> utlColor.ColorError()
      Level.Info -> utlColor.ColorInfo()
      Level.Normal -> utlColor.ColorNormal()
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

    val color= Color(Level.Info)
    overlay.setBackgroundColor(color)
    utlUi.flashView(overlay, 1000)
  }

  fun clearStatus() { tvStatus.visibility = View.GONE}

  fun showNormal(text: String, delay: Long = 500) = showMsgPersistent(Level.Normal, text, delay)
  fun showError(text: String, delay: Long = 500) = showMsgPersistent(Level.Error, text, delay)
  fun showWarning(text: String, delay: Long = 500) = showMsgPersistent(Level.Warning, text)

  private fun clearMessages() {
    tvMsgSubtitle.text=""
    tvMsgTitle.text=""
  }

  private fun showMsgPersistent(level: Level, text: String, delay: Long = 500) {
    clearMessages()
    setMessageTitle(Level.Warning, text)
    utlUi.flashOut(tvBg, 0.7f, delay)
    utlUi.flashView(overlay, delay)
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
      tvBg.setBackgroundColor(utlColor.ColorWhite())
      setMessageTitle(level, title)
      utlUi.flashView(overlay, delay)
      delay(delay)
      hideStatus()
    }
  }

  private fun showMsgAutohide(level: Level, title: String, subtitle: String, delay: Long) {
    // TODO: make only the updates on the Main thread?
    scope.launch {
      clearMessages()
      tvBg.setBackgroundColor(utlColor.ColorWhite())
      setMessageTitle(level, title, subtitle)
      utlUi.flashView(overlay, delay)
      delay(delay)
      hideStatus()
    }
  }

  private fun setMessageTitle(level: Level, title: String, subtitle: String) {
    setMessageTitle(level, title)
    setMessageSubtitle(level, subtitle)
  }

  private fun setMessageSubtitle(level: Level, subtitle: String) {
    val color= Color(level)
    tvMsgSubtitle.setTextColor(color)
    tvMsgSubtitle.text = subtitle
    tvMsgSubtitle.alpha=1f
    tvMsgSubtitle.visibility = View.VISIBLE
  }

  private fun setMessageTitle(level: Level, title: String) {
    val color= Color(level)
    overlay.setBackgroundColor(color)
    tvMsgTitle.setTextColor(color)

    tvMsgTitle.text = title
    tvMsgTitle.alpha=1f
    tvBg.alpha=1f

    tvBg.visibility=View.VISIBLE
    tvMsgTitle.visibility = View.VISIBLE
  }

}
