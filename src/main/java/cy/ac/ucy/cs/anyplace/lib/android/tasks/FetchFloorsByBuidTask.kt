/*
 * Anyplace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Paschalis Mpeis
 *
 * Supervisor: Demetrios Zeinalipour-Yazti
 *
 * URL: http://anyplace.cs.ucy.ac.cy
 * Contact: anyplace@cs.ucy.ac.cy
 *
 * Copyright (c) 2021, Data Management Systems Lab (DMSL), University of Cyprus.
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

import android.app.Activity
import android.app.ProgressDialog
import android.os.AsyncTask
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.app
import cy.ac.ucy.cs.anyplace.lib.android.consts.MSG
import cy.ac.ucy.cs.anyplace.lib.android.nav.FloorModel
import cy.ac.ucy.cs.anyplace.lib.android.utils.NetworkUtils
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class FetchFloorsByBuidTask(private val activity: Activity,
                            private val mListener: FetchFloorsByBuidTaskListener,
                            private val buid: String,
                            showDialog: Boolean) : AsyncTask<Void?, Void?, String>() {
  private val TAG = FetchFloorsByBuidTask::class.java.simpleName

  interface FetchFloorsByBuidTaskListener {
    fun onErrorOrCancel(result: String?)
    fun onSuccess(result: String?, floors: List<FloorModel>?)
  }

  // public FetchFloorsByBuidTask(FetchFloorsByBuidTaskListener fetchFloorsByBuidTaskListener, Context ctx, String buid) {
  //   this.mListener = fetchFloorsByBuidTaskListener;
  //   this.ctx = ctx;
  //   this.buid = buid;
  // }
  // init { // CLR? CHECK:PM
  //   this.showDialog = showDialog
  // }

  private val floors: MutableList<FloorModel> = ArrayList()
  private var success = false
  private lateinit var dialog: ProgressDialog
  private val showDialog = true

  override fun onPreExecute() {
    if (showDialog) {
      dialog = ProgressDialog(activity)
      dialog.isIndeterminate = true
      dialog.setTitle("Fetching floors..")
      // dialog.setMessage("Please be patient...")
      dialog.setCancelable(true)
      dialog.setCanceledOnTouchOutside(false)
      dialog.setOnCancelListener { // finishJob();
        cancel(true)
      }
      dialog.show()
    }
  }

  override fun doInBackground(vararg params: Void?): String {
    return if (!NetworkUtils.isOnline(activity)) {
      MSG.WARN_NO_NETWORK
    } else try {
      val json: JSONObject
      if(activity.app.fileCache.hasBuildingsFloors(buid)) {
        LOG.D2(TAG, "Fetch building floors: using file-cache. buid: $buid")
        json = activity.app.fileCache.readBuildingFloors(buid)
      } else {
        LOG.D2(TAG, "Fetch building floors: downloading for buid: $buid")
        val jsonStr = activity.app.api.allBuildingFloors(buid)
        json = JSONObject(jsonStr)
        if (json.has("status") && json.getString("status").equals("error", ignoreCase = true)) {
          return "ERROR: " + json.getString("message")
        }

        if(!activity.app.fileCache.storeBuildingFloors(buid, jsonStr)) {
          LOG.E(TAG, "ERROR: file-cache failed to store floors. buid: $buid")
        }
      }

      // process the buildings received
      var floor: FloorModel
      val buids_json = JSONArray(json.getString("floors"))
      if (buids_json.length() == 0) {
        return MSG.ERR_NO_FLOORS
      }
      var i = 0
      val sz = buids_json.length()
      while (i < sz) {
        val cp = buids_json[i] as JSONObject
        floor = FloorModel()
        floor.buid = cp.getString("buid")
        floor.floor_name = cp.getString("floor_name")
        floor.floor_number = cp.getString("floor_number")
        floor.description = cp.getString("description")

        // use optString() because these values might not exist of a
        // floor plan has not been set for this floor
        floor.bottom_left_lat = cp.optString("bottom_left_lat")
        floor.bottom_left_lng = cp.optString("bottom_left_lng")
        floor.top_right_lat = cp.optString("top_right_lat")
        floor.top_right_lng = cp.optString("top_right_lng")
        floors.add(floor)
        i++
      }
      floors.sort()
      success = true
      MSG.SUCC_FETCHED_FLOORS
    } catch (e: Exception) {
      activity.app.fileCache.deleteBuildingFloors(buid)
      LOG.E(TAG, e)
      "ERROR Fetch floors: " + e.message
    }
  }

  override fun onPostExecute(result: String) {
    if (showDialog) dialog.dismiss()   // hide progress dialog
    if (success) {
      mListener.onSuccess(result, floors)
    } else {  // there was an error during the process
      mListener.onErrorOrCancel(result)
    }
  }

  override fun onCancelled(result: String) {
    if (showDialog) dialog.dismiss()
    mListener.onErrorOrCancel(MSG.WARN_FLOOR_FETCH_CANCELLED)
  }

}