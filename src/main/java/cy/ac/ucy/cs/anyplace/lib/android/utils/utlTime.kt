package cy.ac.ucy.cs.anyplace.lib.android.utils

import android.annotation.SuppressLint
import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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

  fun getSecondsPretty(seconds: Long): String {
    return when {
      seconds >= 60*60*24 -> {
        val days: Int = (seconds/(60*60*24)).toInt()
        return "$days days"
      }
      seconds >= 60*60 -> {
        val hours: Int = (seconds/(60*60)).toInt()
        val remaining = seconds%(60*60)
        val mins : Int = (remaining/60).toInt()
        "${hours}h ${mins}m"
      }
      seconds >= 60 -> {
        val mins : Int = (seconds/60).toInt()
        "${mins}m"
      }
      else -> {
        "${seconds}s"
      }
    }
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


  fun getPrettyEpochCY(epoch: Long) = getPrettyEpoch(epoch, TIMEZONE_CY)

  fun epoch(): Long = System.currentTimeMillis()/1000

  fun secondsElapsed(time: Long) : Long {
    return epoch() - time
  }

  fun minutesElapsed(time: Long) : Long {
    return secondsElapsed(time)/60
  }





  //returns the current date and formats in the pattern [Jan 01, 2022]
  fun getLocalDateString() : String {
    val currDate =  LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val formattedDate = currDate.format(formatter)
    return formattedDate
  }

  fun getLocalTimeString() : String {
    return LocalTime.now().toString().substringBeforeLast('.').substringBeforeLast(":")
  }

  fun getDateFromStr(date : String) : String{
    return date.substringBeforeLast(' ')
  }

  fun getTimeFromStr(date : String) : String{
    return date.substringAfterLast(' ').substringBeforeLast(":")
  }

  fun getTimeFromStrFull(date : String) : String{
    return date.substringAfterLast(' ')
  }

  //checks if the current date is the same as the one specified in timestr
  fun isSameDay(timestr: String) : Boolean {
    val currDate = getLocalDateString()
    val date = getDateFromStr(timestr)

    if (currDate.equals(date))
      return true
    return false
  }

  /**
   * Returns whether if [time] (a given epoch/unix time)
   * is within [minutes] minutes from current time
   */
  fun isWithinMinutes(time: Long, minutes: Int) : Boolean {
    val curTime = epoch()
    val seconds = minutes*60

    val timeRange = curTime - seconds
    return time >= timeRange
  }

}