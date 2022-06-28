package cy.ac.ucy.cs.anyplace.lib.android.utils.ui

import android.content.Context
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import cy.ac.ucy.cs.anyplace.lib.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Button Utils (some provide backwards compatibility)
 * utlButton
 */
open class UtilAnimations(
        private val ctx: Context,
        private val scope: CoroutineScope) {

  fun fadeIn(v: View) = scope.launch(Dispatchers.Main) { v.fadeIn() }

  fun fadeOut(v: View) = scope.launch(Dispatchers.Main) { v.fadeOut() }

  fun animateAlpha(v: View, alpha: Float, delay: Long = 200)
    = scope.launch(Dispatchers.Main) { v.animateAlpha(alpha, delay) }

  fun flashOut(v: View, alphaStart: Float, delay: Long = 200)
          = scope.launch(Dispatchers.Main) { v.flashOut(alphaStart, delay) }

  fun flashingLoop(v: View)
          = scope.launch(Dispatchers.Main) { v.flashingLoop() }

  fun flashView(v: View, delay: Long = 200)
          = scope.launch(Dispatchers.Main) { v.flashView(delay) }

  fun gone(v: View)  =scope.launch(Dispatchers.Main) { v.visibility = View.GONE }

  fun visible(v: View)  =scope.launch(Dispatchers.Main) { v.visibility = View.VISIBLE}

}



/////////
// PRIVATE EXTENSION METHODS
// Not exposed so they are not used directly.
// Using them through [UtilView] (or it's child methods) ensures that they are on the main thread
// TODO:PM CLR:PM cleanup comments in here
/////////

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

/**
 * NEVER USE DIRECTLY.
 *
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

// fun View.toggleAlpha(isShow: Boolean, delay: Long = 200, invisibleMode: Int = View.GONE) {
//   val alpha = when (visibility) {
//     View.GONE, View.INVISIBLE -> 0f
//     View.VISIBLE -> 1f
//     else -> 1f
//   }
//   if (isShow) animateAlpha(View.VISIBLE, alpha, delay) else animateAlpha(invisibleMode, alpha, delay)
// }

// visibility: Int,
private fun View.animateAlpha(alpha: Float, delay: Long = 200) {
  // if (visibility == View.VISIBLE) {
  //   setVisibility(View.VISIBLE)
  // }

  animate().apply {
    duration = delay
    alpha(alpha)
    // withEndAction { CHECK
    //   setVisibility(visibility)
    // }
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

private fun View.flashView(delay: Long = 200) {
  alpha=0f
  visibility = View.VISIBLE

  this.alpha = 0.3f
  this.animate()
          .alpha(0f)
          .setDuration(delay)
          .setInterpolator(DecelerateInterpolator())
          .start()

  // val fadeIn = AlphaAnimation(0f, 1f)
  // fadeIn.interpolator = DecelerateInterpolator()
  // fadeIn.duration = delay
  //
  // val fadeOut = AlphaAnimation(1f, 0f)
  // fadeOut.interpolator = AccelerateInterpolator()
  // fadeOut.startOffset = delay
  // fadeOut.duration = delay
  //
  // val animation = AnimationSet(false) //change to false
  // animation.addAnimation(fadeIn)
  // animation.addAnimation(fadeOut)
  // this.animation = animation

  // animate().apply {
  //   duration = delay
  //   alpha(1f)
  //   // withEndAction {
  //   //   visibility = View.INVISIBLE
  //   // }
  // }.apply {
  //   duration = delay
  //   alpha(0f)
  // }
}