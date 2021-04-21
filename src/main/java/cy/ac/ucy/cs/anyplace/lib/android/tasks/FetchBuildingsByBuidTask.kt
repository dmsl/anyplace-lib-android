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
import com.google.android.gms.maps.model.LatLng
import cy.ac.ucy.cs.anyplace.lib.Anyplace
import cy.ac.ucy.cs.anyplace.lib.android.consts.MSG
import cy.ac.ucy.cs.anyplace.lib.android.nav.BuildingModel
import cy.ac.ucy.cs.anyplace.lib.android.utils.NetworkUtils
import org.json.JSONException
import org.json.JSONObject

class FetchBuildingsByBuidTask(
        private val mListener: FetchBuildingsByBuidTaskListener,
        private val ctx: Context,
        private val mbuid: String?) : AsyncTask<Void?, Void?, String>() {

  private var building: BuildingModel? = null
  private var success = false
  private var dialog: ProgressDialog? = null
  private var showDialog = true
  private var json_req: String? = null

  // CHECK: never used.
  constructor(fetchBuildingsTaskListener: FetchBuildingsByBuidTaskListener,
              ctx: Context, buid: String?, showDialog: Boolean)
          : this(fetchBuildingsTaskListener, ctx, buid) {
    this.showDialog = showDialog
  }

  init {
    // create the JSON object for the navigation API call
    val j = JSONObject()
    try {
      // j.put("username", "username") // CLR
      // j.put("password", "pass")   // CLR
      // insert the destination POI and the user's coordinates
      j.put("buid", mbuid)
      json_req = j.toString()
    } catch (e: JSONException) {
    }
  }

  interface FetchBuildingsByBuidTaskListener {
    fun onErrorOrCancel(result: String?)
    fun onSuccess(result: String?, building: BuildingModel?)
  }

  override fun onPreExecute() {
    if (showDialog) {
      dialog = ProgressDialog(ctx)
      dialog!!.isIndeterminate = true
      dialog!!.setTitle("Fetching Building")
      dialog!!.setMessage("Please be patient...")
      dialog!!.setCancelable(true)
      dialog!!.setCanceledOnTouchOutside(false)
      dialog!!.setOnCancelListener { cancel(true) }
      dialog!!.show()
    }
  }

  override fun doInBackground(vararg params: Void?): String {
    return if (!NetworkUtils.isOnline(ctx)) {
      MSG.WARN_NO_NETWORK
    } else try {
      if (json_req == null) return MSG.ERR_NULL_JSON_REQ
      val response: String
      // TODO:PM pass Anyplace and use it.
      val pref = ctx.getSharedPreferences("LoggerPreferences", Context.MODE_PRIVATE)
      val host = pref.getString("server_ip_address", "ap.cs.ucy.ac.cy")
      val port = pref.getString("server_port", "443")
      val client = Anyplace(host, port, ctx.cacheDir.absolutePath)
      response = client.buildingsByBuildingCode(mbuid)
      val json = JSONObject(response)
      if (json.has("status") && json.getString("status").equals("1", ignoreCase = true)) {
        return "Error Message: " + json.getString("message")
      }

      // process the buildings received
      // val b: BuildingModel
      val sLat=json.getString("coordinates_lat")
      val sLon=json.getString("coordinates_lon")
      val name=json.getString("name")
      val desc=json.getString("description")
      val buid=json.getString("buid")
      val latLng = LatLng(sLat.toDouble(), sLon.toDouble())
      val b = BuildingModel(latLng, name, desc, buid)
      // b.setPosition(json.getString("coordinates_lat"), json.getString("coordinates_lon"));
      // b.buid = json.getString("buid")
      // b.name = json.getString("name")
      building = b
      success = true
      "Successfully fetched buildings"
    } catch (e: Exception) {
      "Error fetching buildings. [ " + e.message + " ]"
    }
  }

  override fun onPostExecute(result: String) {
    if (showDialog) dialog!!.dismiss()
    if (success) {
      mListener.onSuccess(result, building)
    } else {
      // there was an error during the process
      mListener.onErrorOrCancel(result)
    }
  }

  override fun onCancelled(result: String) {
    if (showDialog) dialog!!.dismiss()
    mListener.onErrorOrCancel("Buildings Fetch cancelled...")
  }

  override fun onCancelled() { // just for < API 11
    if (showDialog) dialog!!.dismiss()
    mListener.onErrorOrCancel("Buildings Fetch cancelled...")
  }
}