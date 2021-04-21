/*
 * Anyplace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Timotheos Constambeys
 *
 * Supervisor: Demetrios Zeinalipour-Yazti
 *
 * URL: http://anyplace.cs.ucy.ac.cy
 * Contact: anyplace@cs.ucy.ac.cy
 *
 * Copyright (c) 2015, Data Management Systems Lab (DMSL), University of Cyprus.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 */
package cy.ac.ucy.cs.anyplace.lib.android.tasks

import android.app.ProgressDialog
import android.content.Context
import android.os.AsyncTask
import cy.ac.ucy.cs.anyplace.lib.Anyplace
import cy.ac.ucy.cs.anyplace.lib.android.consts.DEFAULT.Companion.IP
import cy.ac.ucy.cs.anyplace.lib.android.consts.DEFAULT.Companion.PORT
import cy.ac.ucy.cs.anyplace.lib.android.nav.FloorModel
import cy.ac.ucy.cs.anyplace.lib.android.utils.NetworkUtils
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class FetchFloorsByBuidTask(private val anyplace: Anyplace, private val mListener: FetchFloorsByBuidTaskListener,
                            private val ctx: Context, private val buid: String, showDialog: Boolean) : AsyncTask<Void?, Void?, String>() {
  interface FetchFloorsByBuidTaskListener {
    fun onErrorOrCancel(result: String?)
    fun onSuccess(result: String?, floors: List<FloorModel>?)
  }

  private val floors: MutableList<FloorModel> = ArrayList()
  private var success = false
  private var dialog: ProgressDialog? = null
  private val showDialog = true
  override fun onPreExecute() {
    if (showDialog) {
      dialog = ProgressDialog(ctx)
      dialog!!.isIndeterminate = true
      dialog!!.setTitle("Fetching floors")
      dialog!!.setMessage("Please be patient...")
      dialog!!.setCancelable(true)
      dialog!!.setCanceledOnTouchOutside(false)
      dialog!!.setOnCancelListener { // finishJob();
        cancel(true)
      }
      dialog!!.show()
    }
  }

  protected override fun doInBackground(vararg params: Void): String {
    return if (!NetworkUtils.isOnline(ctx)) {
      "No connection available!"
    } else try {
      //Uses GZIP encoding
      // String response = NetworkUtils.downloadHttpClientJsonPost(AnyplaceAPI.getFetchFloorsByBuidUrl(ctx), j.toString()); // CLR?
      // TODO:PM prefs class TODO PM: anyplace lib is used here...
      val pref = ctx.getSharedPreferences("LoggerPreferences", Context.MODE_PRIVATE)
      val host = pref.getString("server_ip_address", IP)
      val port = pref.getString("server_port", PORT)
      val client = Anyplace(host, port, ctx.cacheDir.absolutePath)
      val response = client.allBuildingFloors(buid)
      val json = JSONObject(response)
      if (json.has("status") && json.getString("status").equals("error", ignoreCase = true)) {
        return "Error Message: " + json.getString("message")
      }

      // process the buildings received
      var b: FloorModel
      val buids_json = JSONArray(json.getString("floors"))
      if (buids_json.length() == 0) {
        return "Error: 0 Floors found"
      }
      var i = 0
      val sz = buids_json.length()
      while (i < sz) {
        val cp = buids_json[i] as JSONObject
        b = FloorModel()
        b.buid = cp.getString("buid")
        b.floor_name = cp.getString("floor_name")
        b.floor_number = cp.getString("floor_number")
        b.description = cp.getString("description")

        // use optString() because these values might not exist of a
        // floor plan has not been set for this floor
        b.bottom_left_lat = cp.optString("bottom_left_lat")
        b.bottom_left_lng = cp.optString("bottom_left_lng")
        b.top_right_lat = cp.optString("top_right_lat")
        b.top_right_lng = cp.optString("top_right_lng")
        floors.add(b)
        i++
      }
      Collections.sort(floors)
      success = true
      "Successfully fetched floors"
    } catch (e: Exception) {
      // Log.d("fetching floors task", e.getMessage());
      "Error fetching floors. [ " + e.message + " ]"
    }
  }

  override fun onPostExecute(result: String) {
    // removes the progress dialog
    if (showDialog) dialog!!.dismiss()
    if (success) {
      mListener.onSuccess(result, floors)
    } else {
      // there was an error during the process
      mListener.onErrorOrCancel(result)
    }
  }

  override fun onCancelled(result: String) {
    if (showDialog) dialog!!.dismiss()
    mListener.onErrorOrCancel("Floor fetching cancelled...")
  }

  override fun onCancelled() { // just for < API 11
    if (showDialog) dialog!!.dismiss()
    mListener.onErrorOrCancel("Floor fetching cancelled...")
  }

  // public FetchFloorsByBuidTask(FetchFloorsByBuidTaskListener fetchFloorsByBuidTaskListener, Context ctx, String buid) {
  //   this.mListener = fetchFloorsByBuidTaskListener;
  //   this.ctx = ctx;
  //   this.buid = buid;
  // }
  init {
    this.showDialog = showDialog
  }
}