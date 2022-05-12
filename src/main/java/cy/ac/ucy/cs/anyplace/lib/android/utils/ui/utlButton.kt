package cy.ac.ucy.cs.anyplace.lib.android.utils.ui

import android.content.Context
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.os.Build
import android.widget.Button
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.R

/**
 * Button Utils (some provide backwards compatibility)
 */
object utlButton {

  /**
   * Works for [MaterialButton]
   */
  fun changeBackgroundButtonDONT_USE(btn: Button, ctx: Context, colorId: Int) {
    val compatColor = ContextCompat.getColor(ctx, colorId)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      btn.setBackgroundColor(compatColor)
    }
  }

  /**
   * Works for [AppCompatButton]
   */
  fun changeBackgroundButtonCompat(btn: Button, ctx: Context, colorId: Int) {
    val compatColor = ContextCompat.getColor(ctx, colorId)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      btn.background.colorFilter =
        BlendModeColorFilter(compatColor, BlendMode.SRC_ATOP)
    }
  }

  fun changeMaterialButtonIcon(btn: MaterialButton, ctx: Context,  id: Int) {
    btn.icon =  ContextCompat.getDrawable(ctx, id)
  }

  fun removeMaterialButtonIcon(btn: MaterialButton) { btn.icon =  null }

}