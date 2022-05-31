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
package cy.ac.ucy.cs.anyplace.lib.android.legacy.tasks

import android.app.Activity
import android.app.ProgressDialog
import android.os.AsyncTask
import cy.ac.ucy.cs.anyplace.lib.android.consts.MSG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.legacy.nav.PoisModel
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.net.OLDNetworkUtils
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Deprecated

/**
 * Returns the POIs according to a given Building and Floor
 */
@Deprecated
class FetchPoisByBuidTask: AsyncTask<Void?, Void?, String> {
  private val TAG = FetchPoisByBuidTask::class.java.simpleName

  interface Callback {
    fun onErrorOrCancel(result: String)
    fun onSuccess(result: String, poisMap: Map<String, PoisModel>)
  }

  private var activity: Activity
  private var callback: Callback
  // private var mCtx: Context
  private val poisMap: MutableMap<String, PoisModel> = HashMap()
  private var buid: String
  private var floorNum: String? = null
  private var dialog: ProgressDialog? = null
  private var success = false

  // constructor(activity: Activity, l: Callback, buid: String, floorNum: String?) {
  //   this.activity=activity
  //   callback = l
  //   this.buid = buid
  //   this.floorNum= floorNum
  // }

  constructor(activity: Activity, l: Callback, buid: String) {
    this.activity = activity
    callback = l
    this.buid = buid
  }

  override fun onPreExecute() {
    dialog = ProgressDialog(activity)
    dialog!!.isIndeterminate = true
    dialog!!.setTitle("Fetching POIs")
    // dialog!!.setMessage("Please be patient...") CLR:PM
    dialog!!.setCancelable(true)
    dialog!!.setCanceledOnTouchOutside(false)
    dialog!!.setOnCancelListener { cancel(true) }
    dialog!!.show()
  }

  override fun doInBackground(vararg params: Void?): String {
    return if (!OLDNetworkUtils.isOnline(activity)) {
      MSG.WARN_NO_NETWORK
    } else try {
      val fetchType = if (floorNum!=null) "floor" else "all"
      var json : JSONObject?
      if(activity.app.fileCache.hasBuildingsPOIs(buid, floorNum)) {
        LOG.D2(TAG, "Fetch pois ($fetchType): using file-cache. buid: $buid")
        json = activity.app.fileCache.readBuildingFloors(buid)
      } else {
        val jsonStr: String = if (floorNum != null) { // fetch floor POIS
          activity.app.apiOld.allBuildingFloorPOIs(buid, floorNum)
        } else {  // fetch all building POIs
          activity.app.apiOld.allBuildingPOIs(buid)
        }
        json = JSONObject(jsonStr)
        // TODO:PM Handle request error method (make a method..)
        if (json.has("status") && json.getString("status").equals("error", ignoreCase = true)) {
          return "ERROR: " + json.getString("message")
        }

        if(!activity.app.fileCache.storeBuildingPOIs(buid, floorNum, jsonStr)) {
          LOG.E(TAG, "ERROR: file-cache failed to store pois ($fetchType). buid: $buid")
        }
      }

      // process the buildings received
      val poisJson = JSONArray(json.getString("pois"))
      var i = 0
      val sz = poisJson.length()
      while (i < sz) {
        val poiJs= poisJson[i] as JSONObject

        // skip POIs without meaning. CHECK:PM What?!
        if (poiJs.getString("pois_type") == "None") {
          i++
          continue
        }
        val poi = PoisModel()
        poi.lat = poiJs.getString("coordinates_lat")
        poi.lng = poiJs.getString("coordinates_lon")
        poi.buid = poiJs.getString("buid")
        poi.floor_name = poiJs.getString("floor_name")
        poi.floor_number = poiJs.getString("floor_number")
        poi.description = poiJs.getString("description")
        poi.name = poiJs.getString("name")
        poi.pois_type = poiJs.getString("pois_type")
        poi.puid = poiJs.getString("puid")
        if (poiJs.has("is_building_entrance")) {
          poi.is_building_entrance = json.getBoolean("is_building_entrance")
        }
        poisMap[poi.puid] = poi // add the POI to the hashmap
        i++
      }
      success = true
      MSG.OK_FETCHED_POIS
    } catch (e: Exception) {
      LOG.E(TAG, e)
      activity.app.fileCache.deleteBuildingPOIs(buid, floorNum)
      "ERROR: $TAG: ${e.javaClass}: ${e.cause}: ${e.message}"
    }
  }

  override fun onPostExecute(result: String) {
    dialog!!.dismiss()
    if (success) { callback.onSuccess(result, poisMap)
    } else { callback.onErrorOrCancel(result) }
  }

  override fun onCancelled(result: String) {
    dialog!!.dismiss()
    activity.app.fileCache.deleteBuildingPOIs(buid, floorNum)
  }

  override fun onCancelled() {
    dialog!!.dismiss()
    activity.app.fileCache.deleteBuildingPOIs(buid, floorNum)
  }
}
