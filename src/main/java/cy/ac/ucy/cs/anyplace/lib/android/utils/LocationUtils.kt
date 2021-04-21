package cy.ac.ucy.cs.anyplace.lib.android.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import java.util.*

//TODO: Request permissions then call from onStart and onConnected.
//TODO: Move all fake gps here.
class GPSUtils {
  companion object {
    fun IsLocationEnabled(ctx: Context) : Boolean {
      val locationManager = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
      return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    fun PrettyLocation(loc: Location, ctx: Context) : String {
      val addresses: List<Address>?
      val geoCoder = Geocoder(ctx, Locale.getDefault())
      addresses = geoCoder.getFromLocation(
              loc.latitude,
              loc.longitude,
              1)
      if (addresses != null && addresses.isNotEmpty()) {
        val address: String = addresses[0].getAddressLine(0)
        val city: String = addresses[0].locality
        val state: String = addresses[0].adminArea
        val country: String = addresses[0].countryName
        val postalCode: String = addresses[0].postalCode
        val knownName: String = addresses[0].featureName
        return "location: $address $city $state $postalCode $country $knownName"
      }

     return "location: <empty>"
    }
  }
}