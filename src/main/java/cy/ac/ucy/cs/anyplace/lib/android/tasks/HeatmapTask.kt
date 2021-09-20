package cy.ac.ucy.cs.anyplace.lib.android.tasks

import android.app.Activity
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng
import cy.ac.ucy.cs.anyplace.lib.android.DBG
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.googlemap.MapTileProvider
import java.io.File

interface PreviousRunningTask {
  fun disableSuccess()
}

/**
 * TODO:PM coroutine
 *
 * used only by logger
 */

@Deprecated("must replace")
class HeatmapTask(private val activity: Activity,
                  private var tileProvider: HeatmapTileProvider?,
                  private var gmap: GoogleMap?)
: AsyncTask<File?, Int?, Collection<WeightedLatLng>?>() {

  val TAG = HeatmapTask::class.java.simpleName

  override fun doInBackground(vararg params: File?): Collection<WeightedLatLng>? {
    if (DBG.D1) {
      LOG.D(TAG, "doInBackground")
      if (params[0] == null) {
        LOG.D(TAG, "null params")
      } else {
        LOG.D(TAG, "params: " + params[0]!!.absolutePath)
      }
    }
    val res = MapTileProvider.readRadioMapLocations(params[0])
    if (DBG.D1 && res == null) {
      LOG.E(TAG, "HeatmapTask doInBackground has a null result")
    }
    return res
  }

  override fun onPostExecute(result: Collection<WeightedLatLng>?) {
    // Check if need to instantiate (avoid setData etc twice)
    if (tileProvider == null) {
      if (result == null) {
        val msg = "No radiomap for selected building."
        Log.d(TAG, msg)
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
        return
      }
      tileProvider = HeatmapTileProvider.Builder().weightedData(result).build()
    } else {
      tileProvider?.setWeightedData(result)
    }
    LOG.D2(TAG, "Adding heatmap: " + result!!.size)
    // CHECK
    val mHeapOverlay = gmap?.addTileOverlay(TileOverlayOptions().tileProvider(tileProvider).zIndex(1f))
  }
}