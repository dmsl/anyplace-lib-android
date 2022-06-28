package cy.ac.ucy.cs.anyplace.lib.android.utils.ui

import android.content.Context
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.os.Build
import android.widget.Button
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Button Utils (some provide backwards compatibility)
 * utlButton
 */
class UtilUI(
        val ctx: Context,
        val scope: CoroutineScope) : UtilAnimations(ctx, scope) {

  fun text(btn: Button, txt: String) = scope.launch(Dispatchers.Main) { btn.text=txt }

  /**
   * Works for [MaterialButton]
   */
  fun changeBackgroundMaterial(btn: MaterialButton, colorId: Int) {
    scope.launch(Dispatchers.Main) {
      val compatColor = ContextCompat.getColor(ctx, colorId)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        btn.setBackgroundColor(compatColor)
      }
    }
  }

  /**
   * Works for [AppCompatButton]
   */
  fun changeBackgroundCompat(btn: Button, colorId: Int) {
    scope.launch(Dispatchers.Main) {
      val compatColor = ContextCompat.getColor(ctx, colorId)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        btn.background.colorFilter =
                BlendModeColorFilter(compatColor, BlendMode.SRC_ATOP)
      }
    }
  }

  fun changeMaterialIcon(btn: MaterialButton, id: Int) {
    scope.launch(Dispatchers.Main) {
      btn.icon =  ContextCompat.getDrawable(ctx, id)
    }
  }

  fun removeMaterialIcon(btn: MaterialButton) {
    scope.launch(Dispatchers.Main) {
      btn.icon =  null
    }
  }
}