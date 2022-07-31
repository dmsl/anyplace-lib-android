package cy.ac.ucy.cs.anyplace.lib.android.utils.ui

import android.content.Context
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Button Utils (some provide backwards compatibility)
 * utlButton
 */
class UtilUI(
        val ctx: Context,
        val scope: CoroutineScope) : UtilAnimations(ctx, scope) {

  fun text(btn: Button, txt: String) = scope.launch(Dispatchers.Main) { btn.text=txt }
  fun text(tv: TextView, txt: String) = scope.launch(Dispatchers.Main) { tv.text=txt }

  companion object {
    /** unsafe as it is not using a scope (must run on UI/Main thread) */
    fun _changeBackgroundMaterial(ctx: Context, btn: MaterialButton, colorId: Int) {
        val compatColor = ContextCompat.getColor(ctx, colorId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          btn.setBackgroundColor(compatColor)
        }
      }
  }

  /**
   * Works for [MaterialButton]
   */
  fun changeBackgroundMaterial(btn: MaterialButton, colorId: Int) {
    scope.launch(Dispatchers.Main) {
      _changeBackgroundMaterial(ctx, btn, colorId)
    }
  }

  fun changeBackgroundDrawable(btn: Button, drawableId: Int) {
    scope.launch(Dispatchers.Main) {
      btn.background= AppCompatResources.getDrawable(ctx, drawableId)
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

  fun textColor(tv: TextView, colorId: Int) {
    scope.launch(Dispatchers.Main) {
      val compatColor = ContextCompat.getColor(ctx, colorId)
      tv.setTextColor(compatColor)
    }
  }

  fun setTextSizeSp(b: Button, v: Float) {
    scope.launch(Dispatchers.Main) {
      b.setTextSize(TypedValue.COMPLEX_UNIT_SP, v)
    }
  }

  fun attentionInvalidOption(v: View) {
    scope.launch(Dispatchers.IO) {
      animateAlpha(v, 0.3f,250)
      delay(500)
      animateAlpha(v, 1f,250)
    }
  }

}