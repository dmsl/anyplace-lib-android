package cy.ac.ucy.cs.anyplace.lib.android.utils.ui

import android.content.Context
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Button Utils (some provide backwards compatibility)
 * utlButton
 *
 * They execute operations the [Dispatchers.Main]:
 * - whetever touches the UI must be on the main thread
 */
open class UtilAnimations(
        private val ctx: Context,
        private val scope: CoroutineScope) {


  fun enable(v: View) = scope.launch(Dispatchers.Main) { v.isEnabled=true }
  fun disable(v: View) = scope.launch(Dispatchers.Main) { v.isEnabled=false }

  fun fadeIn(v: View) = scope.launch(Dispatchers.Main) { v.fadeIn() }
  fun fadeInAnyway(v: View) = scope.launch(Dispatchers.Main) { v.fadeInAnyway() }

  fun fadeOut(v: View) = scope.launch(Dispatchers.Main) { v.fadeOut() }

  fun animateAlpha(v: View, alpha: Float, delay: Long = 200)
    = scope.launch(Dispatchers.Main) { v.animateAlpha(alpha, delay) }

  fun flashOut(v: View, alphaStart: Float, delay: Long = 200)
          = scope.launch(Dispatchers.Main) { v.flashOut(alphaStart, delay) }

  fun flashingLoop(v: View)
          = scope.launch(Dispatchers.Main) { v.flashingLoop() }

  fun recordingCameraLoop(v: View)
          = scope.launch(Dispatchers.Main) { v.recordingCameraLoop() }

  fun flashView(v: View, delay: Long = 200)
          = scope.launch(Dispatchers.Main) { v.flashView(delay) }

  fun attentionZoom(v: View)
          = scope.launch(Dispatchers.Main) {
    // v.attentionZoom() Bug?
  }

  fun gone(v: View) = scope.launch(Dispatchers.Main) { v.visibility = View.GONE }
  fun invisible(v: View) = scope.launch(Dispatchers.Main) { v.visibility = View.INVISIBLE }
  fun visible(v: View) =scope.launch(Dispatchers.Main) { v.visibility = View.VISIBLE}

  fun alpha(v: View, a: Float) = scope.launch(Dispatchers.Main) { v.alpha=a }
  fun clearAnimation(v: View) = scope.launch(Dispatchers.Main) { v.clearAnimation() }
}

private fun View.attentionZoom() {
    visibility = View.VISIBLE
    isEnabled = true
    val anim = AnimationUtils.loadAnimation(context, R.anim.zoom_attention)
    startAnimation(anim)
}

/**
 * NEVER USE DIRECTLY.
 *
 * Use a wrapper like [UtilAnimations] or [UtilUI] that does the animation on the main thread
 */
private fun View.fadeIn() {
  if (visibility != View.VISIBLE) {
    visibility = View.VISIBLE
    isEnabled = true
    val anim = AnimationUtils.loadAnimation(context, R.anim.zoom_in)
    startAnimation(anim)
  }
}

private fun View.fadeInAnyway() {
  visibility = View.INVISIBLE
  visibility = View.VISIBLE
  isEnabled = true
  val anim = AnimationUtils.loadAnimation(context, R.anim.zoom_in)
  startAnimation(anim)
}

/**
 * NEVER USE DIRECTLY.
 * Use a wrapper like [UtilAnimations] or [UtilUI] that does the animation on the main thread
 */
private fun View.fadeOut() {
  if (visibility == View.VISIBLE) {
    val anim = AnimationUtils.loadAnimation(context, R.anim.zoom_out)
    startAnimation(anim)
    visibility = View.INVISIBLE
    isEnabled = false
  }
}

private fun View.animateAlpha(alpha: Float, delay: Long = 200) {
  animate().apply {
    duration = delay
    alpha(alpha)
  }
}

private fun View.flashOut(alphaStart: Float, delay: Long = 1000) {
  alpha = alphaStart
  visibility = View.VISIBLE

  animate().apply {
    duration = delay
    alpha(0f)
  }
}

private fun View.flashingLoop() {
  val anim= AnimationUtils.loadAnimation(context, R.anim.flash_fade40)
  startAnimation(anim)
}

private fun View.recordingCameraLoop() {
  val anim= AnimationUtils.loadAnimation(context, R.anim.flash_recording)
  startAnimation(anim)
}

private fun View.flashView(delay: Long = 200) {
  alpha=0f
  visibility = View.VISIBLE

  this.alpha = 0.3f
  this.animate()
          .alpha(0f)
          .setDuration(delay)
          .setInterpolator(DecelerateInterpolator())
          .start()
}