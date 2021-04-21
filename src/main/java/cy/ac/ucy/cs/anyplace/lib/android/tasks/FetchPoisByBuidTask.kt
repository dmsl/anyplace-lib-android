/*
* AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
*
* Anyplace is a first-of-a-kind indoor information service offering GPS-less
* localization, navigation and search inside buildings using ordinary smartphones.
*
* Author(s): Timotheos Constambeys, Lambros Petrou
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

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.os.AsyncTask
import cy.ac.ucy.cs.anyplace.lib.Anyplace
import cy.ac.ucy.cs.anyplace.lib.android.nav.PoisModel
import cy.ac.ucy.cs.anyplace.lib.android.utils.NetworkUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

/**
 * Returns the POIs according to a given Building and Floor
 *
 */
class FetchPoisByBuidTask : AsyncTask<Void?, Void?, String> {
  interface FetchPoisListener {
    fun onErrorOrCancel(result: String?)
    fun onSuccess(result: String?, poisMap: Map<String, PoisModel>?)
  }

  private var mListener: FetchPoisListener
  private var mCtx: Context
  private val poisMap: MutableMap<String, PoisModel> = HashMap()
  private var buid: String
  private var floor_number: String? = null
  private var dialog: ProgressDialog? = null
  private var success = false

  constructor(l: FetchPoisListener, ctx: Context, buid: String, floor_number: String?) {
    mCtx = ctx
    mListener = l
    this.buid = buid
    this.floor_number = floor_number
  }

  constructor(activity: Activity?, l: FetchPoisListener, buid: String) {
    mCtx = ctx
    mListener = l
    this.buid = buid
  }

  override fun onPreExecute() {
    dialog = ProgressDialog(mCtx)
    dialog!!.isIndeterminate = true
    dialog!!.setTitle("Fetching POIs")
    dialog!!.setMessage("Please be patient...")
    dialog!!.setCancelable(true)
    dialog!!.setCanceledOnTouchOutside(false)
    dialog!!.setOnCancelListener { cancel(true) }
    dialog!!.show()
  }

  protected override fun doInBackground(vararg params: Void): String {
    return if (!NetworkUtils.isOnline(mCtx)) {
      "No connection available!"
    } else try {
      val j = JSONObject()
      try {
        j.put("username", "username")
        j.put("password", "pass")
        j.put("buid", buid)
        j.put("floor_number", floor_number)
      } catch (e: JSONException) {
        return "Error Message: Could not create the request for the POIs!"
      }
      val response: String
      val pref = mCtx.getSharedPreferences("LoggerPreferences", Context.MODE_PRIVATE)
      val host = pref.getString("server_ip_address", "ap.cs.ucy.ac.cy")
      val port = pref.getString("server_port", "443")
      val client = Anyplace(host, port, mCtx.cacheDir.absolutePath)
      response = if (floor_number != null) {
        // fetch the pois of this floor
        //response = NetworkUtils.downloadHttpClientJsonPost(AnyplaceAPI.getFetchPoisByBuidFloorUrl(), j.toString());
        client.allBuildingFloorPOIs(buid, floor_number)
      } else {
        // fetch the pois for the whole building
        //response = NetworkUtils.downloadHttpClientJsonPost(AnyplaceAPI.getFetchPoisByBuidUrl(), j.toString());
        client.allBuildingPOIs(buid)
      }
      val json_all = JSONObject(response)
      if (json_all.has("status") && json_all.getString("status").equals("error", ignoreCase = true)) {
        return "Error Message: " + json_all.getString("message")
      }
      // process the buildings received
      val buids_json = JSONArray(json_all.getString("pois"))
      var i = 0
      val sz = buids_json.length()
      while (i < sz) {
        val json = buids_json[i] as JSONObject

        // skip POIS without meaning
        if (json.getString("pois_type") == "None") {
          i++
          continue
        }
        val poi = PoisModel()
        poi.lat = json.getString("coordinates_lat")
        poi.lng = json.getString("coordinates_lon")
        poi.buid = json.getString("buid")
        poi.floor_name = json.getString("floor_name")
        poi.floor_number = json.getString("floor_number")
        poi.description = json.getString("description")
        poi.name = json.getString("name")
        poi.pois_type = json.getString("pois_type")
        poi.puid = json.getString("puid")
        if (json.has("is_building_entrance")) {
          poi.is_building_entrance = json.getBoolean("is_building_entrance")
        }
        poisMap[poi.puid] = poi // add the POI to the hashmap
        i++
      }
      success = true
      "Successfully fetched Points of Interest"
    } catch (e: JSONException) {
      "Not valid response from the server! Contact the admin."
    } catch (e: Exception) {
      "Error fetching Points of Interest. Exception[ " + e.message + " ]"
    }
  }

  override fun onPostExecute(result: String) {
    dialog!!.dismiss()
    if (success) {
      mListener.onSuccess(result, poisMap)
    } else {
      mListener.onErrorOrCancel(result)
    }
  }

  override fun onCancelled(result: String) {
    dialog!!.dismiss()
    mListener.onErrorOrCancel(result)
  }

  override fun onCancelled() {
    dialog!!.dismiss()
    mListener.onErrorOrCancel("Fetching POIs was cancelled!")
  }
} // end of fetch POIS by building and floor
