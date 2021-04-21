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
package cy.ac.ucy.cs.anyplace.lib.android.cache

import android.content.Context
import cy.ac.ucy.cs.anyplace.lib.android.nav.BuildingModel.isFloorsLoaded
import cy.ac.ucy.cs.anyplace.lib.android.nav.BuildingModel.loadFloors
import cy.ac.ucy.cs.anyplace.lib.android.nav.BuildingModel.loadedFloors
import cy.ac.ucy.cs.anyplace.lib.android.nav.BuildingModel.latitudeString
import cy.ac.ucy.cs.anyplace.lib.android.nav.BuildingModel.longitudeString
import cy.ac.ucy.cs.anyplace.lib.android.cache.BackgroundFetchListener
import cy.ac.ucy.cs.anyplace.lib.android.nav.BuildingModel
import android.os.AsyncTask
import android.os.Build
import cy.ac.ucy.cs.anyplace.lib.android.tasks.FetchFloorsByBuidTask.FetchFloorsByBuidTaskListener
import cy.ac.ucy.cs.anyplace.lib.android.nav.FloorModel
import cy.ac.ucy.cs.anyplace.lib.android.tasks.FetchFloorPlanTask
import cy.ac.ucy.cs.anyplace.lib.android.tasks.FetchFloorPlanTask.FetchFloorPlanTaskListener
import cy.ac.ucy.cs.anyplace.lib.android.tasks.DownloadRadioMapTaskBuid
import cy.ac.ucy.cs.anyplace.lib.android.tasks.DownloadRadioMapTaskBuid.DownloadRadioMapListener
import java.io.File
import java.io.Serializable

// @SuppressWarnings("serial")
internal class BackgroundFetch(ctx: Context, private val l: BackgroundFetchListener, build: BuildingModel?) : Serializable, Runnable {
  var build: BuildingModel? = null
  var status = BackgroundFetchListener.Status.RUNNING
  private var error = BackgroundFetchListener.ErrorType.EXCEPTION
  private var progress_total = 0
  private var progress_current = 0

  // private Context ctx;
  private var currentTask: AsyncTask<Void, Void, String>? = null
  override fun run() {
    fetchFloors()
  }

  // Fetch Building Floors Details
  private fun fetchFloors() {
    if (!build!!.isFloorsLoaded) {
      build!!.loadFloors(app,
              object : FetchFloorsByBuidTaskListener {
                fun onSuccess(result: String?, floors: List<FloorModel?>?) {
                  progress_total = build!!.loadedFloors.size * 2
                  fetchAllFloorPlans(0)
                }

                override fun onErrorOrCancel(result: String?) {
                  status = BackgroundFetchListener.Status.STOPPED
                  l.onErrorOrCancel(result, error)
                }
              }, false, false)
    } else {
      progress_total = build!!.loadedFloors.size * 2
      fetchAllFloorPlans(0)
    }
  }

  // Fetch Floor Maps
  private fun fetchAllFloorPlans(index: Int) {
    if (build!!.isFloorsLoaded) {
      if (index < build!!.loadedFloors.size) {
        val f = build!!.loadedFloors[index]
        currentTask = FetchFloorPlanTask(ctx, build!!.buid, f.floor_number)
        (currentTask as FetchFloorPlanTask).setCallbackInterface(object : FetchFloorPlanTaskListener {
          override fun onSuccess(result: String, floor_plan_file: File) {
            l.onProgressUpdate(++progress_current, progress_total)
            fetchAllFloorPlans(index + 1)
          }

          override fun onErrorOrCancel(result: String) {
            status = BackgroundFetchListener.Status.STOPPED
            l.onErrorOrCancel(result, error)
          }

          override fun onPrepareLongExecute() {}
        })
        currentTask.execute()
      } else {
        fetchAllRadioMaps(0)
      }
    } else {
      status = BackgroundFetchListener.Status.STOPPED
      l.onErrorOrCancel("Fetch Floor Plans Error", error)
    }
  }

  // fetch All Radio Maps except from current floor(floor_number)
  private fun fetchAllRadioMaps(index: Int) {
    if (build!!.loadedFloors.size > 0) {
      if (index < build!!.loadedFloors.size) {
        val f = build!!.loadedFloors[index]
        val task: AsyncTask<Void, Void, String> = DownloadRadioMapTaskBuid(object : DownloadRadioMapListener {
          override fun onSuccess(result: String) {
            l.onProgressUpdate(progress_current++, progress_total)
            fetchAllRadioMaps(index + 1)
          }

          override fun onErrorOrCancel(result: String) {
            status = BackgroundFetchListener.Status.STOPPED
            l.onErrorOrCancel(result, BackgroundFetchListener.ErrorType.EXCEPTION)
          }

          override fun onPrepareLongExecute() {}
        }, ctx, build!!.latitudeString, build!!.longitudeString, build!!.buid, f.floor_number, false)
        val currentApiVersion = Build.VERSION.SDK_INT
        currentTask = if (currentApiVersion >= Build.VERSION_CODES.HONEYCOMB) {
          // Execute task parallel with others
          task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        } else {
          task.execute()
        }
      } else {
        status = BackgroundFetchListener.Status.SUCCESS
        l.onSuccess("Finished loading building")
      }
    } else {
      l.onErrorOrCancel("ERROR: fetchAllRadioMaps: ", error)
    }
  }

  fun cancel() {
    error = BackgroundFetchListener.ErrorType.CANCELLED
    if (currentTask != null) {
      currentTask!!.cancel(true)
    }
  }

  init {
    this.build = build
    ctx = ctx
  }
}