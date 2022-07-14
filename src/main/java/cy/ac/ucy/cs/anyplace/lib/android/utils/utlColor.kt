package cy.ac.ucy.cs.anyplace.lib.android.utils

import android.content.Context
import androidx.core.content.ContextCompat
import cy.ac.ucy.cs.anyplace.lib.R

class UtilColor(private val ctx: Context) {
  fun get(id: Int) =ContextCompat.getColor(ctx, id)

  fun ColorWhiteB0() = get(R.color.white_B0)
  fun ColorWhite() = get(R.color.white)
  fun ColorWarning() = get(R.color.yellowDark)
  fun ColorInfo() = get(R.color.holo_light_blue)
  fun ColorNormal() = Black()
  fun Black() = get(R.color.black)
  fun GrayLighter() = get(R.color.lighterGray)
  fun ColorPrimaryDark() = get(R.color.colorPrimaryDark)
  fun ColorPrimaryDark50() = get(R.color.colorPrimaryDark50)
  fun ColorPrimary() = get(R.color.colorPrimary)
  fun ColorError() = get(R.color.redDark)
  fun ColorYellowDark() = get(R.color.yellowDark)
  fun ColorBlueDark() = get(R.color.lash_blue_dark)
}