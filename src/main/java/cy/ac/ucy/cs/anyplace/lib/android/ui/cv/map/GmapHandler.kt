package cy.ac.ucy.cs.anyplace.lib.android.ui.components

import android.content.Context
import androidx.annotation.IntegerRes
import androidx.fragment.app.FragmentManager
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.CvMapActivity

class GmapHandler(private val ctx: Context) {

  lateinit var gmap: GoogleMap

  fun attach(act: CvMapActivity, layout_id: Int) {
    val mapFragment = SupportMapFragment.newInstance()
    act.supportFragmentManager
            .beginTransaction()
            .add(layout_id, mapFragment)
            .commit()
    mapFragment.getMapAsync(act)
  }

  fun setup(googleMap: GoogleMap) {
    gmap = googleMap

    // VMB.markers = Markers(applicationContext, gmap)
    // val maxZoomLevel = gmap.maxZoomLevel // may be different from device to device
    // map.addMarker(MarkerOptions().position(latLng).title("Ucy Building"))
    // map.moveCamera(CameraUpdateFactory.newLatLng(latLng))

    // TODO Space has to be sent to this activity (SafeArgs?) using a previous "Select Space" activity.
    // (maybe using Bundle is easier/better)
    // loadSpaceAndFloor()

    // TODO:PM this must be moved to earlier activity
    // along with Space/Floors loading (that also needs implementation).
    // lifecycleScope.launch(Dispatchers.IO) { VMB.floorsH.fetchAllFloorplans() }

    // place some restrictions on the map
    // gmap.moveCamera(CameraUpdateFactory.newCameraPosition(
    //         CameraAndViewport.loggerCamera(VMB.spaceH.latLng(), maxZoomLevel)))
    // gmap.setMinZoomPreference(maxZoomLevel-3)

    gmap.uiSettings.apply {
      isZoomControlsEnabled = false
      isMapToolbarEnabled = false
      isTiltGesturesEnabled = false
      isCompassEnabled = false
      isIndoorLevelPickerEnabled = true
    }
    // onMapReadySpecialize() // TODO
  }
}