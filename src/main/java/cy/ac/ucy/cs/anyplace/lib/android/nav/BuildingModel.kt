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
package cy.ac.ucy.cs.anyplace.lib.android.nav

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import cy.ac.ucy.cs.anyplace.lib.android.cache.AnyplaceCache
import cy.ac.ucy.cs.anyplace.lib.android.tasks.FetchFloorsByBuidTask
import cy.ac.ucy.cs.anyplace.lib.android.tasks.FetchFloorsByBuidTask.FetchFloorsByBuidTaskListener
import java.io.Serializable
import java.util.*

class BuildingModel : Comparable<BuildingModel>, ClusterItem, Serializable {
  interface FetchBuildingTaskListener {
    fun onErrorOrCancel(result: String?)
    fun onSuccess(result: String?, building: BuildingModel?)
  }

  var buid = ""
  var name: String? = null

  // public String description;
  // public String address;
  // public String url;
  var latitude = 0.0
  var longitude = 0.0

  // last fetched floors
  var floors: List<FloorModel> = ArrayList(0)
    private set

  // List index Used in SelectBuilding Activity
  var selectedFloorIndex = 0
    private set

  fun loadFloors(l: FetchFloorsByBuidTaskListener, ctx: Context, forceReload: Boolean, showDialog: Boolean) {
    if (!forceReload && isFloorsLoaded) {
      l.onSuccess("Successfully read from cache", floors)
    } else {
      FetchFloorsByBuidTask(object : FetchFloorsByBuidTaskListener {
        override fun onSuccess(result: String, floors: List<FloorModel>) {
          this.floors = floors
          AnyplaceCache.saveInstance(ctx.applicationContext)
          l.onSuccess(result, floors)
        }

        override fun onErrorOrCancel(result: String) {
          l.onErrorOrCancel(result)
        }
      }, ctx, buid, showDialog).execute()
    }
  }

  val isFloorsLoaded: Boolean
    get() = if (floors.size == 0) false else true
  val selectedFloor: FloorModel?
    get() {
      var f: FloorModel? = null
      try {
        f = floors[selectedFloorIndex]
      } catch (ex: IndexOutOfBoundsException) {
      }
      return f
    }

  fun getFloorFromNumber(floor_number: String): FloorModel? {
    val index = checkFloorIndex(floor_number) ?: return null
    return floors[index]
  }

  // Set Currently Selected floor number
  fun setSelectedFloor(floor_number: String): Boolean {
    val floor_index = checkFloorIndex(floor_number)
    return if (floor_index != null) {
      selectedFloorIndex = floor_index
      true
    } else {
      false
    }
  }

  // Set Currently Selected floor number (array index)
  fun checkIndex(floor_index: Int): Boolean {
    return if (floor_index >= 0 && floor_index < floors.size) {
      true
    } else {
      false
    }
  }

  fun checkFloorIndex(floor_number: String): Int? {
    var index: Int? = null
    for (i in floors.indices) {
      val floorModel = floors[i]
      if (floorModel.floor_number == floor_number) {
        index = i
        break
      }
    }
    return index
  }

  override fun toString(): String {
    // return name + " [" + description + "]";
    return name!!
  }

  override fun equals(object2: Any?): Boolean {
    return object2 is BuildingModel && buid == object2.buid
  }

  val latitudeString: String
    get() = java.lang.Double.toString(latitude)
  val longitudeString: String
    get() = java.lang.Double.toString(longitude)

  override fun getPosition(): LatLng {
    return LatLng(latitude, longitude)
  }

  override fun getTitle(): String? {
    return null
  }

  override fun getSnippet(): String? {
    return null
  }

  // TODO: UPDATE MAPS CLUSTER API
  // @Nullable
  // @Override
  // public String getTitle() {
  // 	return null;
  // }
  // @Nullable
  // @Override
  // public String getSnippet() {
  // 	return null;
  // }
  fun setPosition(latitude: String, longitude: String) {
    this.latitude = latitude.toDouble()
    this.longitude = longitude.toDouble()
  }

  override fun compareTo(arg0: BuildingModel): Int {
    // ascending order
    return name!!.compareTo(arg0.name!!)
  }
}