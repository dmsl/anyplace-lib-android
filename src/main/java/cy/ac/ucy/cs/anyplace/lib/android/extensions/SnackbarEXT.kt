package cy.ac.ucy.cs.anyplace.lib.android.extensions

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import cy.ac.ucy.cs.anyplace.lib.R

fun Snackbar.gravityTop() {
  // supporting compose and normal?
  if (this.view.layoutParams is FrameLayout.LayoutParams) {
    this.view.layoutParams = (this.view.layoutParams as FrameLayout.LayoutParams).apply {
      gravity = Gravity.TOP
    }
  } else if (this.view.layoutParams is CoordinatorLayout.LayoutParams) {
    this.view.layoutParams = (this.view.layoutParams as CoordinatorLayout.LayoutParams).apply {
      gravity = Gravity.TOP
    }
  }
}

fun Snackbar.setBackgroundColor(bgColorId: Int) {
  this.view.setBackgroundColor(bgColorId)
}

fun Snackbar.setBackground(drawableId: Int) {
  this.view.background=ContextCompat.getDrawable(this.context, drawableId)
}

fun Snackbar.setDrawableLeft(id: Int) {
  val tv = this.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
  tv.setCompoundDrawablesWithIntrinsicBounds(id, 0, 0, 0)
}

fun Snackbar.setIconTint(tintColorId: Int) {
    val tv = this.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
    val drawables: Array<Drawable> = tv.compoundDrawables
    drawables[0].colorFilter = PorterDuffColorFilter(tintColorId, PorterDuff.Mode.SRC_ATOP)
}
//
// fun Snackbar.setActionTextColor(colorId: Int) {
//   val button= this.view.findViewById<Button>(com.google.android.material.R.id.snackbar_action)
//   button.setBackgroundColor(colorId)
// }


