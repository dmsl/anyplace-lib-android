package cy.ac.ucy.cs.anyplace.lib.android.utils

import android.app.Activity
import android.widget.Toast
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import org.json.JSONObject

class ResponseUtils {
  companion object {
    fun process(activity: Activity, jsonRes: String) {
      val obj = JSONObject(jsonRes)
      if(obj.has("status_code") && obj.getInt("status_code")!=200) {
        val code = obj.getInt("status_code")
        val message = obj.getString("message")
        val info = "$code: $message"
        LOG.E(info)
        Toast.makeText(activity, ""+info, Toast.LENGTH_LONG).show() // TODO RED
      }
    }
  }
}