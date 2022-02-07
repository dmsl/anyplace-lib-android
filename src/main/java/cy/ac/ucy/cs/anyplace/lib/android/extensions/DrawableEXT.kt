package cy.ac.ucy.cs.anyplace.lib.android.extensions

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.graphics.drawable.toBitmap

fun Drawable.resizeTo(ctx: Context, size: Int) =
        BitmapDrawable(ctx.resources, toBitmap(size, size))

fun Drawable.setColor(ctx: Context, @ColorRes colorRes: Int) {
  val color= ContextCompat.getColor(ctx, colorRes)
  colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
          color, BlendModeCompat.SRC_ATOP)
}