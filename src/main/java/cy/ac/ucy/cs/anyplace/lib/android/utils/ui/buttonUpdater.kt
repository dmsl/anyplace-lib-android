package cy.ac.ucy.cs.anyplace.lib.android.utils.ui

import android.content.Context
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.os.Build
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import cy.ac.ucy.cs.anyplace.lib.R

object buttonUpdater {

  fun changeBackgroundCompatButton(btn: Button, ctx: Context, colorId: Int) {
    val compatColor = ContextCompat.getColor(ctx, colorId)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      btn.background.colorFilter =
        BlendModeColorFilter(compatColor, BlendMode.SRC_ATOP)
    }
  }
}