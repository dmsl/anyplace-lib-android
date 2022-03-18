package cy.ac.ucy.cs.anyplace.lib.android.utils

import android.annotation.SuppressLint
import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*

/** Time Utils */
object utlTime {

  fun getSecondsRounded(num: Float, maxAllowed: Int): String {
    var rounded = num.toInt() + 1
    if (rounded > maxAllowed) rounded = maxAllowed
    return rounded.toString()
  }

  fun getSecondsPretty(num: Float): String {
    val res = "%.1f".format(num) + "s"
    if (res.length > 4) return "0.0s"
    return res
  }

  const val TIMEZONE_CY = "Asia/Nicosia"

  /**
   * [epoch]: not in ms
   */
  @SuppressLint("SimpleDateFormat")
  fun getPrettyEpoch(epoch: Long, timezone: String): String {
    val epochMs = epoch * 1000
    val stamp = Timestamp(epochMs)
    val date = Date(stamp.time)

    val sdf = SimpleDateFormat("HH:mm:ss - dd/MM/yy")
    sdf.timeZone = TimeZone.getTimeZone(timezone)

    return sdf.format(date)
  }
}