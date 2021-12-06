/*
* Anyplace: A free and open Indoor Navigation Service with superb accuracy!
*
* Anyplace is a first-of-a-kind indoor information service offering GPS-less
* localization, navigation and search inside buildings using ordinary smartphones.
*
* Author(s): Paschalis Mpeis, Timotheos Constambeys
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
package cy.ac.ucy.cs.anyplace.lib.android.nav

import android.app.Activity
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.tasks.FetchFloorsByBuidTask
import cy.ac.ucy.cs.anyplace.lib.android.tasks.FetchFloorsByBuidTask.FetchFloorsByBuidTaskListener
import java.io.Serializable
import java.util.*

class BuildingModel(
        // init inherited fields
        private val position: LatLng,  // TODO this is not parcelable/serializable
        private val title: String,
        private val snippet: String,
        // extra fields (local to BuildingModel)
        @JvmField
        var buid: String): ClusterItem,
        Comparable<BuildingModel>, Serializable {
        // Comparable<BuildingModel>, Serializable {

  // }  CHECK:PM this is a mess.. CLR?
  // class BuildingModel(
  //         // init inherited fields
  //         private val position: LatLng,  // BUG this is not parcelable/serializable
  //         private val title: String,
  //         private val snippet: String,
  //         // extra fields (local to BuildingModel)
  //         @JvmField
  //         var buid: String)


  interface Callback {
    fun onErrorOrCancel(result: String?)
    fun onSuccess(result: String?, building: BuildingModel?)
  }

  // @JvmField
  // val buid: String // CLR
  // @JvmField
  // var name: String = "" CLR ??
  // @JvmField
  // var latitude = 0.0
  // @JvmField
  // var longitude = 0.0

  // public String description;
  // public String address;
  // public String url;

  // last fetched floors
  var loadedFloors: List<FloorModel> = ArrayList(0)
    private set

  // List index Used in SelectBuilding Activity
  var selectedFloorIndex = 0
    private set

  override fun getPosition(): LatLng { return position }
  override fun getTitle(): String { return title }
  override fun getSnippet(): String { return snippet }
  val description: String get() { return snippet }
  val name : String get() { return title }

  fun loadFloors(activity: Activity, l: FetchFloorsByBuidTaskListener, forceReload: Boolean, showDialog: Boolean) {
    LOG.D2()
    if (!forceReload && isFloorsLoaded) {
      l.onSuccess("Successfully read from cache", loadedFloors)
    } else {
      FetchFloorsByBuidTask(activity, object : FetchFloorsByBuidTaskListener {
        override fun onSuccess(result: String?, floors: List<FloorModel>?) {
          this@BuildingModel.loadedFloors= floors!!
          // LOG.D(TAG, "ObjectCache: saving fetched floors")
          // ObjectCache.saveInstance(activity.app)
          l.onSuccess(result, floors)
        }

        override fun onErrorOrCancel(result: String?) {
          l.onErrorOrCancel(result)
        }
      }, buid, showDialog).execute()
    }
  }

  val isFloorsLoaded = (loadedFloors.isNotEmpty())

  /** Custom getter (computed) */
  val selectedFloor: FloorModel?
    get() {
      return try {
        loadedFloors[selectedFloorIndex]
      } catch (ex: IndexOutOfBoundsException) {
        null
      }
    }

  fun getFloorFromNumber(floorNum: String): FloorModel? {
    val index = checkFloorIndex(floorNum) ?: return null
    return loadedFloors[index]
  }

  /** Set Currently Selected floor number */
  fun setSelectedFloor(floorNum: String): Boolean {
    val floor_index = checkFloorIndex(floorNum)
    return if (floor_index != null) {
      selectedFloorIndex = floor_index
      true
    } else {
      false
    }
  }

  fun checkIndex(floorIdx: Int): Boolean = floorIdx >= 0 && floorIdx < loadedFloors.size

  fun checkFloorIndex(floorNum: String): Int? {
    var index: Int? = null
    for (i in loadedFloors.indices) {
      val floorModel = loadedFloors[i]
      if (floorModel.floor_number == floorNum) {
        index = i
        break
      }
    }
    return index
  }

  val lat: Double get() = position.latitude
  val lon: Double get() = position.longitude
  val latitudeString: String get() = java.lang.Double.toString(position.latitude)
  val longitudeString: String get() = java.lang.Double.toString(position.longitude)

  // return name + " [" + description + "]"; (OLD CLR?)
  // override fun toString(): String = name
  override fun toString(): String = title

  override fun equals(obj: Any?): Boolean = obj is BuildingModel && buid == obj.buid

  // val latitudeString: String
  //   get() = java.lang.Double.toString(latitude)
  // val longitudeString: String
  //   get() = java.lang.Double.toString(longitude)

  // override fun compareTo(other: BuildingModel): Int {
  //   TODO("Not yet implemented")
  // }

  // fun setPosition(latitude: String, longitude: String) {
  //   position = LatLng(latitude.toDouble(), longitude.toDouble())
  // }

  override fun compareTo(other: BuildingModel): Int {
    // ascending order
    // return name.compareTo(bm.name) CHECK:PM CLR:PM
    return title.compareTo(other.title)
  }
}