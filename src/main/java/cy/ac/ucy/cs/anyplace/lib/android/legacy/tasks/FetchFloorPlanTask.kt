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
package cy.ac.ucy.cs.anyplace.lib.android.legacy.tasks

import android.app.Activity
import android.os.AsyncTask
import android.os.Handler
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.consts.MSG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.legacy.AndroidUtils
import java.io.*
import java.net.SocketTimeoutException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Deprecated("must replace")
class FetchFloorPlanTask(private val activity: Activity,
                         private val buid: String,
                         private val floorNum: String?) : AsyncTask<Void?, Void?, String>() {
  private val TAG = FetchFloorPlanTask::class.java.simpleName

  interface Callback {
    fun onPrepareLongExecute()
    fun onErrorOrCancel(result: String?)
    fun onSuccess(result: String?, floor_plan_file: File?)
  }

  private var callback: Callback? = null
  private var floorplanFile: File? = null
  private var success = false

  // Sync/Run PreExecute Listener on UI Thread
  // val syncListener = Any() CLR
  private val lockCallback = ReentrantLock()
  private val condCallback = lockCallback.newCondition()
  private val lockMain= ReentrantLock()
  private val condMain= lockMain.newCondition()

  var run = false
  fun setCallbackInterface(callback: Callback?) {
    this.callback = callback
  }

  @Throws(IOException::class)
  fun copy(src: File?, dst: File?) {
    var input: InputStream? = null
    var out: OutputStream? = null
    try {
      input = FileInputStream(src)
      out = FileOutputStream(dst)
      val buf = ByteArray(1024)  // Transfer bytes from in to out
      var len: Int
      while (input.read(buf).also { len = it } > 0) {
        out.write(buf, 0, len)
      }
    } finally {
      input?.close()
      out?.close()
    }
  }

  override fun doInBackground(vararg params: Void?): String {
    var output: OutputStream? = null
    val ins: InputStream? = null
    var tempFile: File? = null
    return try {

      // // check sdcard state // CLR:PM using file cahcle
      // if (!AndroidUtils.checkExternalStorageState()) {
      //   // we cannot download the floor plan on the sdcard
      //   return "ERROR: It seems that we cannot write on your sdcard!"
      // }
      // val sdcard_root = ctx.getExternalFilesDir(null)
      //         ?: return "ERROR: It seems we cannot save the floorplan on sdcard!"

      val SEP=File.separatorChar
      val dir= File(activity.app.prefs.cacheDir, "floorplans$SEP$buid$SEP$floorNum")
      LOG.E(TAG, "Floorplan dir: $dir")
      dir.mkdirs()
      val dest_path = File(dir, "tiles_archive.zip")
      val okFile = File(dir, "ok.txt")

      // check if the file already exists and if yes return immediately
      if (dest_path.exists() && dest_path.canRead() && dest_path.isFile && okFile.exists()) {
        floorplanFile = dest_path
        success = true
        LOG.D("Floorplan in file-cache.")
        return "Read floorplan from cache."
      }
      runPreExecuteOnUI()
      okFile.delete()
      tempFile = File(activity.app.cacheDir, "tmp.floorplan." + Integer.toString((Math.random() * 100).toInt()))
      if (tempFile.exists()) throw Exception("$TAG: Temporary file already in use!")

      // SharedPreferences pref = ctx.getSharedPreferences("LoggerPreferences", MODE_PRIVATE);
      // String host = pref.getString("server_ip_address", "ap.cs.ucy.ac.cy");
      // String port = pref.getString("server_port", "443");
      // //Anyplace client = new Anyplace("ap.cs.ucy.ac.cy", "443", "");
      // Anyplace client = new Anyplace(host, port, ctx.getCacheDir().getAbsolutePath());
      //String access_token = "TOKEN_REMOVED";

      // val access_token: String = pref.getString("server_access_token", "need an access token")
      val response: ByteArray = activity.app.apiOld.floortilesByte(activity.app.prefs.access_token, buid, floorNum)
      LOG.D2(TAG, "response byteArray.size: ${response.size}")
      output = FileOutputStream(tempFile)

      // byte[] buffer = new byte[4096];
      // int bytesRead = 0;
      // while ((bytesRead = is.read(buffer, 0, buffer.length)) >= 0) {
      //   output.write(response);
      // }
      output.write(response)
      output.close()

      // Critical Block - Added for safety
      lockMain.withLock {
        copy(tempFile, dest_path)
        // unzip the tiles_archive
        AndroidUtils.unzip(dest_path.absolutePath)
        val out = FileWriter(okFile)
        out.write("ok;version:0;")
        out.close()
      }
      floorplanFile = dest_path
      LOG.D2(TAG, "Downloaded floor tiles: " + floorplanFile!!.absolutePath)
      waitPreExecute()
      success = true
      "Successfully fetched floor plan"
      // } catch (ConnectTimeoutException e) { // CLR:PM
      // 	return "Cannot connect to Anyplace service!";
    } catch (e: SocketTimeoutException) {
      "ERROR: connection timeout."
    // } catch (e: JSONException) {
      // "JSONException: " + e.message
    } catch (e: Exception) {
      "ERROR: fetch floor plan:${e.javaClass}:${e.cause}: ${e.message}"
    } finally {
      if (ins != null) try {
        ins.close()
        output!!.close()
      } catch (e: IOException) {
        LOG.E(e)
      }
      tempFile?.delete()
    }
  }

  override fun onPostExecute(result: String) {
    if (success) {
      callback!!.onSuccess(result, floorplanFile)
    } else {
      // there was an error during the process
      callback!!.onErrorOrCancel(result)
    }
  }

  private fun runPreExecuteOnUI() {
    // Get a handler that can be used to post to the main thread
    val mainHandler = Handler(activity.mainLooper)
    val myRunnable = Runnable {
      try {
        callback!!.onPrepareLongExecute()
      } finally {
        lockCallback.withLock {
          run = true
          condCallback.signalAll()
        }
      }
    }
    mainHandler.post(myRunnable)
  }

  @Throws(InterruptedException::class)
  private fun waitPreExecute() {
    lockCallback.withLock {
      while (!run) { condCallback.await() }
    }
  }

  override fun onCancelled(result: String) {
    callback!!.onErrorOrCancel(MSG.CANCELLED_FLOORPLAN_FETCH)
  }

  override fun onCancelled() { // just for < API 11
    callback!!.onErrorOrCancel(MSG.CANCELLED_FLOORPLAN_FETCH)
  }
}