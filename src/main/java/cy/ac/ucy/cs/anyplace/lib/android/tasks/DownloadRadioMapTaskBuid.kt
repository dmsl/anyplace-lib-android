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

import android.content.Context
import android.os.Handler
import android.util.Log
import cy.ac.ucy.cs.anyplace.lib.Anyplace
import cy.ac.ucy.cs.anyplace.lib.android.utils.AnyplaceUtils
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileWriter

/**
 * The main task that downloads a radio map for the specified area.
 * TODO - we should check more thoroughly the concurrency issues
 * Time consuming real time calculation of radiomaps
 */
class DownloadRadioMapTaskBuid(callback: Callback?, ctx: Context?,
                               buid: String?, floor_number: String?, forceDownload: Boolean) : AsyncTask<Void?, Void?, String?>() {
  interface Callback {
    fun onPrepareLongExecute()
    fun onErrorOrCancel(result: String?)
    fun onSuccess(result: String?)
  }

  var callbackInterface: Callback? = null
  private val ctx: Context? = null
  private var json_req: String? = null
  private var mBuildID: String? = null
  private var mFloor_number: String? = null
  private var mForceDonwload: Boolean? = null
  private var success = false

  // Sync/Run PreExecute Listener on UI Thread
  val syncListener = Any()
  var run = false

  constructor(mListener: Callback?, ctx: Context?,
              lat: String?, lon: String?, buid: String?,
              floor_number: String?, forceDownload: Boolean) : this(mListener, ctx, buid, floor_number, forceDownload) {
  }

  protected override fun doInBackground(vararg params: Void): String {
    var releaseLock = false
    return try {
      if (json_req == null) return "Error creating the request!"
      // check sdcard state
      val root: File
      root = try {
        AnyplaceUtils.getRadioMapFolder(ctx, mBuildID, mFloor_number)
      } catch (e: Exception) {
        return e.message!!
      }
      val okfile = File(root, "ok.txt")
      if (!mForceDonwload!! && okfile.exists()) {
        success = true
        return "Successfully read radio map from cache!"
      }

      // Allow only one download of the radiomap
      synchronized(downInProgress) {
        if (downInProgress == false) {
          downInProgress = true
          releaseLock = true
        } else {
          return "Already downloading radio map. Please wait..."
        }
      }
      runPreExecuteOnUI()
      okfile.delete()

      // TODO:PM CHANGE THIS
      val pref: SharedPreferences = ctx!!.getSharedPreferences("LoggerPreferences", Context.MODE_PRIVATE)
      val host: String = pref.getString("server_ip_address", "ap.cs.ucy.ac.cy")
      val port: String = pref.getString("server_port", "443")
      val client = Anyplace(host, port, ctx.cacheDir.absolutePath)
      val access_token: String = pref.getString("server_access_token", "need an access token")
      val response = client.radiomapMeanByBuildingFloor(access_token, mBuildID, mFloor_number)
      if (AnyplaceDebug.DEBUG_MESSAGES) {
        Log.d(TAG, "Anyplace client response is $response")
      }
      val filename_radiomap_download: String = AnyplaceUtils.getRadioMapFileName(mFloor_number)
      var out: FileWriter
      out = FileWriter(File(root, filename_radiomap_download))
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

  protected override fun onPostExecute(result: String) {
    if (success) {
      if (callbackInterface != null) callbackInterface.onSuccess(result)
    } else {
      // there was an error during the process
      if (callbackInterface != null) callbackInterface.onErrorOrCancel(result)
    }
  }

  private fun runPreExecuteOnUI() {
    // Get a handler that can be used to post to the main thread
    val mainHandler = Handler(ctx!!.mainLooper)
    val myRunnable: Runnable = object : Runnable {
      override fun run() {
        try {
          if (callbackInterface != null) callbackInterface.onPrepareLongExecute()
        } finally {
          synchronized(syncListener) {
            run = true
            syncListener.notifyAll()
          }
        }
      }
    }
    mainHandler.post(myRunnable)
  }

  @Throws(InterruptedException::class)
  private fun waitPreExecute() {
    synchronized(syncListener) {
      while (run == false) {
        syncListener.wait()
      }
    }
  }

  protected override fun onCancelled(result: String) {
    if (callbackInterface != null) callbackInterface.onErrorOrCancel(result)
  }

  protected override fun onCancelled() {
    if (callbackInterface != null) callbackInterface.onErrorOrCancel("Downloading RadioMap was cancelled!")
  }

  companion object {
    private val TAG = DownloadRadioMapTaskBuid::class.java.simpleName

    // Allow only one download task (real time creation of radiomap)
    @Volatile
    private var downInProgress = false
  }

  init {
    try {
      callbackInterface = callback
      this.ctx = ctx
      val j = JSONObject()
      // j.put("username", "username");
      // j.put("password", "pass");

      // add the building and floor in order to get only the necessary radio map
      j.put("buid", buid)
      j.put("floor", floor_number)
      json_req = j.toString()
      mBuildID = buid
      mFloor_number = floor_number
      mForceDonwload = forceDownload
    } catch (e: JSONException) {
      LOG.E(e)
    }
  }
} // end of radiomap download task
