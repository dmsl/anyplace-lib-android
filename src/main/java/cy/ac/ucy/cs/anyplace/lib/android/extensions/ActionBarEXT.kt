package cy.ac.ucy.cs.anyplace.lib.android.extensions

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.appcompat.app.ActionBar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import cy.ac.ucy.cs.anyplace.lib.R

fun ActionBar.setTextColor(color: Int) {
  val text = SpannableString(title ?: "")
  text.setSpan(ForegroundColorSpan(color),0,text.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
  title = text
}

fun ActionBar.setBackButton(ctx: Context, color: Int) {
  val icArrow = ContextCompat.getDrawable(ctx, R.drawable.ic_arrow_back)!!
  DrawableCompat.setTint(icArrow, ContextCompat.getColor(ctx, R.color.white))
  setHomeAsUpIndicator(icArrow)
}
