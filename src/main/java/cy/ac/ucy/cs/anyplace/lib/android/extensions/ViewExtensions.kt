package cy.ac.ucy.cs.anyplace.lib.android.extensions

import android.view.View
import android.view.animation.*
import cy.ac.ucy.cs.anyplace.lib.R

// fun View.toggleAlpha(isShow: Boolean, delay: Long = 200, invisibleMode: Int = View.GONE) {
//   val alpha = when (visibility) {
//     View.GONE, View.INVISIBLE -> 0f
//     View.VISIBLE -> 1f
//     else -> 1f
//   }
//   if (isShow) animateAlpha(View.VISIBLE, alpha, delay) else animateAlpha(invisibleMode, alpha, delay)
// }

// visibility: Int,
fun View.animateAlpha(alpha: Float, delay: Long = 200) {
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


fun View.flashView(delay: Long = 200) {
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


fun View.fadeIn() {
  visibility = View.VISIBLE
  isEnabled = true
  val anim = AnimationUtils.loadAnimation(context, R.anim.zoom_in)
  startAnimation(anim)
}

fun View.fadeOut() {
  if (visibility == View.VISIBLE) {
    val anim = AnimationUtils.loadAnimation(context, R.anim.zoom_out)
    startAnimation(anim)
    visibility = View.INVISIBLE
    isEnabled = false
  }
}
