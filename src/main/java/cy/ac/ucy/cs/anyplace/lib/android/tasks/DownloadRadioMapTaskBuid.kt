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

import android.app.Activity
import android.os.AsyncTask
import android.os.Handler
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * The main task that downloads a radio map for the specified area.
 * TODO - we should check more thoroughly the concurrency issues
 * Time consuming real time calculation of radiomaps
 *
 *
 * INFO there was another constructor here with lat/long these values where not used.
 *
 */
@Deprecated("must replace")
class DownloadRadioMapTaskBuid(private var activity: Activity,
                               private var callback: Callback,
                               private var buid: String,
                               private var floorNum: String?,
                               private val forceDownload: Boolean)
  : AsyncTask<Void?, Void?, String?>() {
  interface Callback {
    fun onPrepareLongExecute()
    fun onErrorOrCancel(result: String?)
    fun onSuccess(result: String?)
  }


  // init {
  //   try {
  //     // callbackInterface = callback
  //     // this.ctx = ctx
  //     // j.put("username", "username");
  //     // j.put("password", "pass");
  //     // json_req = j.toString()
  //     // mBuildID = buid
  //     // mFloor_number = floor_number
  //     // mForceDonwload = forceDownload
  //   } catch (e: JSONException) {
  //     LOG.E(e)
  //   }
  // }

  companion object {
    // Allow only one download task (real time creation of radiomap)
    @Volatile
    private var downInProgress = false
  }

  private val TAG = DownloadRadioMapTaskBuid::class.java.simpleName

  private val lock = ReentrantLock()
  private val condition = lock.newCondition()

  var callbackInterface: Callback? = null
  // private val ctx: Context? = null
  // private var json_req: String? = null
  // private var mBuildID: String? = null
  // private var mFloor_number: String? = null
  // private var mForceDonwload: Boolean? = null
  private var success = false

  // Sync/Run PreExecute Listener on UI Thread
  // val syncListener = Any()
  var run = false


  override fun doInBackground(vararg params: Void?): String {
    var releaseLock = false
    return try {
      val j = JSONObject()
      j.put("buid", buid)
      j.put("floor", floorNum)
      val jsonReq = j.toString()

      // check sdcard state
      val root: File = try {
        // AnyplaceUtils1.getRadioMapFolder(ctx, mBuildID, mFloor_number)
        activity.app.fileCache.radiomapsFolder(buid, floorNum)
      } catch (e: Exception) {
        LOG.E(TAG, e)
        return e.message!!
      }
      val okfile = File(root, "ok.txt")
      if (!forceDownload !! && okfile.exists()) {
        success = true
        return "Successfully read radiomap from cache!"
      }

      // Allow only one download of the radiomap
      lock.withLock {
        if (downInProgress == false) {
          downInProgress = true
          releaseLock = true
        } else {
          return "Already downloading radiomap. Please wait.."
        }
      }
      runPreExecuteOnUI()
      okfile.delete()
      // TODO:PM CHANGE THIS
      val accessToken = activity.app.prefs.access_token
      val response = activity.app.apiOld.radiomapMeanByBuildingFloor(accessToken, buid, floorNum)
      LOG.D2(TAG, "response: $response")
      // ReponseUtil.process(activity, response)

      // val filename_radiomap_download: String = AnyplaceUtils1.getRadioMapFileName(mFloor_number)
      val radiomapFilename = activity.app.fileCache.radiomapFilename(floorNum)
      var out: FileWriter
      out = FileWriter(File(root, radiomapFilename))
      out.write(response)
      out.close()
      out = FileWriter(okfile)
      out.write("ok;version:0;")
      out.close()
      waitPreExecute()
      success = true
      LOG.D2(TAG, "DownloadRadiomap: Successfully got radiomap")
      "Saved radiomaps"
    } catch (e: Exception) {
      "Error downloading radio maps [ " + e.message + " ]"
    } finally {
      if (releaseLock) downInProgress = false
    }
  }

  override fun onPostExecute(result: String?) {
    if (success) {
      callback.onSuccess(result)
    } else {
      // there was an error during the process
      callback.onErrorOrCancel(result)
    }
  }

  private fun runPreExecuteOnUI() {
    // Get a handler that can be used to post to the main thread
    val mainHandler = Handler(activity.mainLooper)
    val myRunnable: Runnable = object : Runnable {
      override fun run() {
        try {
          if (callbackInterface != null) callback.onPrepareLongExecute()
        } finally {
          lock.withLock {
            run = true
            condition.signalAll()
          }
        }
      }
    }
    mainHandler.post(myRunnable)
  }

  @Throws(InterruptedException::class)
  private fun waitPreExecute() {
    lock.withLock {
      while (run == false) { condition.await() }
    }
  }

  override fun onCancelled(result: String?) {
    if (callbackInterface != null) callback.onErrorOrCancel(result)
  }

  override fun onCancelled() {
    if (callbackInterface != null) callback.onErrorOrCancel("Downloading RadioMap was cancelled!")
  }
}
// end of radiomap download task
