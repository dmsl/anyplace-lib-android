package cy.ac.ucy.cs.anyplace.lib.android.utils

import android.annotation.SuppressLint
import android.icu.text.DateFormat
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class GenUtils {
  companion object {

    @SuppressLint("SimpleDateFormat")
    fun prettyTime() : String {
      val formatter= SimpleDateFormat("HH:mm:ss")
      // formatter.set
      val date = Date()
      return  formatter.format(date)
    }
  }
}