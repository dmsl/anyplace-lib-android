package cy.ac.ucy.cs.anyplace.lib.android.utils

import android.content.Context
import androidx.core.content.ContextCompat
import cy.ac.ucy.cs.anyplace.lib.R

class UtilColor(private val ctx: Context) {
  fun get(id: Int) =ContextCompat.getColor(ctx, id)

  fun WhiteB0() = get(R.color.white_B0)
  fun White() = get(R.color.white)
  fun GrayDarker() = get(R.color.darker)
  fun DevMode() = get(R.color.dev_mode)
  fun Warning() = get(R.color.yellowDark)
  fun Info() = get(R.color.holo_light_blue)
  fun Normal() = Black()
  fun Black() = get(R.color.black)
  fun GrayLighter() = get(R.color.lighterGray)
  fun PrimaryDark() = get(R.color.colorPrimaryDark)
  fun PrimaryDark50() = get(R.color.colorPrimaryDark50)
  fun Primary() = get(R.color.colorPrimary)
  fun LocationSmas() = get(R.color.locationSmas)
  fun Error() = get(R.color.redDark)
  fun ActionBar() = get(R.color.action_bar)
  fun YellowDark() = get(R.color.yellowDark)
  fun BlueDark() = get(R.color.lash_blue_dark)
}