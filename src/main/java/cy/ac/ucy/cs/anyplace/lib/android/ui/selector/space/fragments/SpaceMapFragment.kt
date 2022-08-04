package cy.ac.ucy.cs.anyplace.lib.android.ui.selector.space.fragments

import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import cy.ac.ucy.cs.anyplace.lib.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * Just for reference (and hidden in bottom nav menu)
 * - In future, someone could add some points in the map, so the building can be chosen based on that
 * - could also acquire nearby location, so the user can pick spaces in vicinity
 */
@AndroidEntryPoint
class SpaceMapFragment : Fragment() {

  private val callback = OnMapReadyCallback { googleMap ->
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * In this case, we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to
     * install it inside the SupportMapFragment. This method will only be triggered once the
     * user has installed Google Play services and returned to the app.
     */
    val sydney = LatLng(-34.0, 151.0)
    googleMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
    googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_spaces_map, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
    mapFragment?.getMapAsync(callback)
  }
}