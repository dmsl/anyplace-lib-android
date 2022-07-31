package cy.ac.ucy.cs.anyplace.lib.android.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.maps.model.LatLng
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Coord
import java.util.*

// extension functions
fun Coord.toLatLng() = LatLng(this.lat, this.lon)
fun Location.toLatLng() = LatLng(latitude, longitude)

object utlLoc {

  fun toLatLng(coord: Coord) = LatLng(coord.lat, coord.lon)

  /**
   * Returns an address (nullable) given a location
   */
  private fun getAddress(latLng: LatLng, ctx: Context) : Address? {

    val addresses: List<Address>?
    val geoCoder = Geocoder(ctx, Locale.getDefault())
    try {
      addresses = geoCoder.getFromLocation(
              latLng.latitude,
              latLng.longitude,
              1)
      if (addresses != null && addresses.isNotEmpty()) {
        return addresses[0]
      }
    } catch (e: Exception) {
    }
    return null
  }

  fun prettyLocation(loc: Location, ctx: Context) : String {
    val address = getAddress(loc.toLatLng(), ctx)
    address?.let {
      val addressLine: String = address.getAddressLine(0)
      val city: String = address.locality
      val state: String = address.adminArea
      val country: String = address.countryName
      // sometimes is null
      // var postalCode: String = addresses[0].postalCode
      // if(postalCode==null) postalCode="po:null"
      val knownName: String = address.featureName
      return "$address $city $state $country $knownName"
    }
    return "<empty>"
  }

  fun getTown(latLng: LatLng, ctx: Context) : String? {
    val address = getAddress(latLng, ctx)
    return address?.locality
  }

  fun getCountry(latLng: LatLng, ctx: Context) : String? {
    val address = getAddress(latLng, ctx)
    return address?.countryName
  }
}