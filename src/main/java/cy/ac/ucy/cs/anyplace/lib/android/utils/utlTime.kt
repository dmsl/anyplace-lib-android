package cy.ac.ucy.cs.anyplace.lib.android.utils

import android.annotation.SuppressLint
import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*

/** Time Utils */
object utlTime {

  fun currentTimePretty() : String {
    val formatter= SimpleDateFormat("HH:mm:ss")
    // formatter.set
    val date = Date()
    return  formatter.format(date)
  }

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
   *
   *  TODO:PM [timezone] read from /smas/version endpoint (preserve it in misc?)
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

  /** Get epoch in seconds (just like in Unix) */
  fun epoch(): Long = System.currentTimeMillis()/1000

  fun secondsElapsed(time: Long) : Long {
    return epoch() - time
  }

  fun minutesElapsed(time: Long) : Long {
    return secondsElapsed(time)/60
  }
}