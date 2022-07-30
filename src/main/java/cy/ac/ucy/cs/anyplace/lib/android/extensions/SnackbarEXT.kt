package cy.ac.ucy.cs.anyplace.lib.android.extensions

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar

/**
 * Supporting Jetpack Compose and normal XML-based UIs
 */
fun Snackbar.setGravity(value: Int) {
  // supporting compose and normal?
  if (this.view.layoutParams is FrameLayout.LayoutParams) {
    this.view.layoutParams = (this.view.layoutParams as FrameLayout.LayoutParams).apply {
      gravity = value
    }
  } else if (this.view.layoutParams is CoordinatorLayout.LayoutParams) {
    this.view.layoutParams = (this.view.layoutParams as CoordinatorLayout.LayoutParams).apply {
      gravity = value
    }
  }
}

fun Snackbar.setGravity() = setGravity(Gravity.TOP)

/** Convert a float to DP units */
fun Float.toDips(ctx: Context) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, ctx.resources.displayMetrics).toInt()

/** Convert an int to DP units */
fun Int.toDips(ctx: Context) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), ctx.resources.displayMetrics).toInt()


private fun Snackbar.setMarginBase(topMargin: Int) {
  val lp = LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT,
          LinearLayout.LayoutParams.WRAP_CONTENT)

  // position on top
  lp.topMargin=topMargin
  lp.gravity=Gravity.TOP
  this.view.layoutParams=lp

  // set min width
  this.view.minimumHeight=52f.toDips(this.view.context)

  val tv = this.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
  tv.gravity=Gravity.CENTER_VERTICAL
}

fun Snackbar.setMarginCvMap() {
  setMarginBase(12)
}

fun Snackbar.setMarginChat() {
  setMarginBase(4)
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
  tv.compoundDrawablePadding=(5).toDips(this.view.context)
}

fun Snackbar.setIconTint(tintColorId: Int) {
    val tv = this.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
    val drawables: Array<Drawable> = tv.compoundDrawables
    drawables[0].colorFilter = PorterDuffColorFilter(tintColorId, PorterDuff.Mode.SRC_ATOP)
}

fun Snackbar.setActionTextColor(colorId: Int) {
  val button= this.view.findViewById<Button>(com.google.android.material.R.id.snackbar_action)
  button.setBackgroundColor(colorId)
}


