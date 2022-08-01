package cy.ac.ucy.cs.anyplace.lib.android.utils.imu

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.toCoord
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.CvMapActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.GmapWrapper
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationResult
import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationResult.Companion.ENGINE_IMU
import kotlinx.coroutines.flow.update
import java.util.ArrayList
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class IMU(
        private val act: CvMapActivity,
        private val VM: CvViewModel,
        private val map: GmapWrapper) {

  private val app : AnyplaceApp = act.application as AnyplaceApp

  companion object{
    private const val earthRadius = 6371000
    private const val stepSize = 0.37 //previously 0.74
  }


  var started=false
  fun start(){
    if (started || !DBG.uim) return
    started=true

    checkPermission()

    LOG.W(TAG, "Starting..")

    var azimuth = 330.0
    act.VMsensor.azimuthRotationVector.observe(act) {
      azimuth = it
    }

    //every time the value of stepsDetected changes the new position of the object is calculated

    act.VMsensor.stepsDetected.observe(act, Observer{ stepCount->
      // MODE 1
      LOG.W(TAG, "Observing..: cnt: $stepCount")

      if (VM.imuEnabled && DBG.uim) { // TODO:PMX: option in VM?
        val lastCoord= app.locationSmas.value.coord
        if (lastCoord == null) {
          LOG.W(TAG, "OBSERVE: ret: no last coord")
          return@Observer
        }

        val lastPos =LatLng(lastCoord.lat, lastCoord.lon)

        val tmp = findNewPosition(lastPos, azimuth)
        val floorNum = app.wLevel?.floorNumber()!!
        val polyOpts = map.lines.getPolyopts(floorNum)
        if (polyOpts == null) {
          LOG.W(TAG, "OBSERVE: ret: null polyopts")
          return@Observer
        }

        val latLong = findClosestPoint(tmp, polyOpts)

        LOG.E(TAG, "Found NEW MM point: $latLong")
        app.locationSmas.update { LocalizationResult.Success(latLong.toCoord(floorNum), ENGINE_IMU) }
      }
    })
  }

  // SKIP THIS
  private fun checkPermission(){
    val permissions: MutableList<String> = ArrayList()

    // if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
    //         ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
    //   permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)

    if (ActivityCompat.checkSelfPermission(act,
                    Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED)
      permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)


    val ALL_PERMISSIONS = 101
    if (permissions.isNotEmpty())
      ActivityCompat.requestPermissions(act, permissions.toTypedArray(), ALL_PERMISSIONS)
  }


  /**
   * Calculates the new position with the use of Dead Reckoning
   */
  fun findNewPosition(oldPosition: LatLng, azimuth: Double): LatLng {
    val azimuthRad = Math.toRadians(azimuth)

    val Lat = Math.toRadians(oldPosition.latitude)
    val Lng = Math.toRadians(oldPosition.longitude)

    val newLat: Double = asin(sin(Lat) * cos(stepSize / earthRadius) +cos(Lat) * sin(stepSize / earthRadius) * cos(azimuthRad))
    val newLng: Double = Lng + atan2(sin(azimuthRad) * sin(stepSize / earthRadius) * cos(Lat), cos(stepSize / earthRadius) - sin(Lat) * sin(newLat))

    val newLatDegrees = Math.toDegrees(newLat)
    val newLngDegrees = Math.toDegrees(newLng)

    return LatLng(newLatDegrees,newLngDegrees)
  }

  /**
   * Finds the closest point on the route with the use of Map Matching
   */
  fun findClosestPoint(point: LatLng, polylinesArray: MutableList<PolylineOptions>): LatLng {
    val match = MapMatching()
    return match.find_point(point, polylinesArray)
  }
}