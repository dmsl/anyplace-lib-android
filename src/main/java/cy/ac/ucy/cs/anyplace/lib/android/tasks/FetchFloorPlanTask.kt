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
import android.os.AsyncTask
import android.os.Handler
import cy.ac.ucy.cs.anyplace.lib.android.LOG.Companion.D2
import cy.ac.ucy.cs.anyplace.lib.android.utils.AndroidUtils
import org.json.JSONException
import java.io.*
import java.net.SocketTimeoutException

class FetchFloorPlanTask(private val ctx: Context, private val buid: String, private val floor_number: String) : AsyncTask<Void?, Void?, String>() {
  interface Callback {
    fun onPrepareLongExecute()
    fun onErrorOrCancel(result: String?)
    fun onSuccess(result: String?, floor_plan_file: File?)
  }

  private var callback: Callback? = null
  private var floor_plan_file: File? = null
  private var success = false

  // Sync/Run PreExecute Listener on UI Thread
  val syncListener = Any()
  var run = false
  fun setCallbackInterface(callback: Callback?) {
    this.callback = callback
  }

  @Throws(IOException::class)
  fun copy(src: File?, dst: File?) {
    var `in`: InputStream? = null
    var out: OutputStream? = null
    try {
      `in` = FileInputStream(src)
      out = FileOutputStream(dst)
      // Transfer bytes from in to out
      val buf = ByteArray(1024)
      var len: Int
      while (`in`.read(buf).also { len = it } > 0) {
        out.write(buf, 0, len)
      }
    } finally {
      `in`?.close()
      out?.close()
    }
  }

  protected override fun doInBackground(vararg params: Void): String {
    var output: OutputStream? = null
    val `is`: InputStream? = null
    var tempFile: File? = null
    return try {

      // check sdcard state
      if (!AndroidUtils.checkExternalStorageState()) {
        // we cannot download the floor plan on the sdcard
        return "ERROR: It seems that we cannot write on your sdcard!"
      }
      val sdcard_root = ctx.getExternalFilesDir(null)
              ?: return "ERROR: It seems we cannot save the floorplan on sdcard!"
      val root = File(sdcard_root, "floor_plans" + File.separatorChar + buid + File.separatorChar + floor_number)
      root.mkdirs()
      val dest_path = File(root, "tiles_archive.zip")
      val okfile = File(root, "ok.txt")

      // check if the file already exists and if yes return immediately
      if (dest_path.exists() && dest_path.canRead() && dest_path.isFile && okfile.exists()) {
        floor_plan_file = dest_path
        success = true
        return "Successfully read floor plan from cache!"
      }
      runPreExecuteOnUI()
      okfile.delete()
      tempFile = File(ctx.cacheDir, "FloorPlan" + Integer.toString((Math.random() * 100).toInt()))
      if (tempFile.exists()) throw Exception("Temp File already in use")

      // SharedPreferences pref = ctx.getSharedPreferences("LoggerPreferences", MODE_PRIVATE);
      // String host = pref.getString("server_ip_address", "ap.cs.ucy.ac.cy");
      // String port = pref.getString("server_port", "443");
      // //Anyplace client = new Anyplace("ap.cs.ucy.ac.cy", "443", "");
      // Anyplace client = new Anyplace(host, port, ctx.getCacheDir().getAbsolutePath());
      //String access_token = "TOKEN_REMOVED";
      val access_token: String = pref.getString("server_access_token", "need an access token")
      val response: ByteArray = client.floortilesByte(access_token, buid, floor_number)
      // LOG.i(TAG, response );
      output = FileOutputStream(tempFile)

      // byte[] buffer = new byte[4096];
      // int bytesRead = 0;
      // while ((bytesRead = is.read(buffer, 0, buffer.length)) >= 0) {
      //   output.write(response);
      // }
      output.write(response)
      output.close()

      // Critical Block - Added for safety
      synchronized(sync) {
        copy(tempFile, dest_path)
        // unzip the tiles_archive
        AndroidUtils.unzip(dest_path.absolutePath)
        val out = FileWriter(okfile)
        out.write("ok;version:0;")
        out.close()
      }
      floor_plan_file = dest_path
      D2(TAG, "Downloaded floor tiles: " + floor_plan_file!!.absolutePath)
      waitPreExecute()
      success = true
      "Successfully fetched floor plan"
      // } catch (ConnectTimeoutException e) {
      // 	return "Cannot connect to Anyplace service!";
    } catch (e: SocketTimeoutException) {
      "Communication with the server is taking too long!"
    } catch (e: JSONException) {
      "JSONException: " + e.message
    } catch (e: Exception) {
      "Error fetching floor plan. [ " + e.message + " ]"
    } finally {
      if (`is` != null) try {
        `is`.close()
      } catch (e: IOException) {
      }
      if (output != null) try {
        output.close()
      } catch (e: IOException) {
      }
      tempFile?.delete()
    }
  }

  override fun onPostExecute(result: String) {
    if (success) {
      callback!!.onSuccess(result, floor_plan_file)
    } else {
      // there was an error during the process
      callback!!.onErrorOrCancel(result)
    }
  }

  private fun runPreExecuteOnUI() {
    // Get a handler that can be used to post to the main thread
    val mainHandler = Handler(ctx.mainLooper)
    val myRunnable = Runnable {
      try {
        callback!!.onPrepareLongExecute()
      } finally {
        synchronized(syncListener) {
          run = true
          syncListener.notifyAll()
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

  override fun onCancelled(result: String) {
    callback!!.onErrorOrCancel("Floor plan loading cancelled...")
  }

  override fun onCancelled() { // just for < API 11
    callback!!.onErrorOrCancel("Floor plan loading cancelled...")
  }

  companion object {
    private val TAG = FetchFloorPlanTask::class.java.simpleName
    private val sync = Any()
  }
}