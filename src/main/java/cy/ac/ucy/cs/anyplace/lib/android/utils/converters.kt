package cy.ac.ucy.cs.anyplace.lib.android.utils

import com.google.android.gms.maps.model.LatLng
import cy.ac.ucy.cs.anyplace.lib.models.Coord
import cy.ac.ucy.cs.anyplace.lib.models.CvLocation

object converters {
  fun toLatLng(coord: Coord) = LatLng(coord.lat, coord.lon)
  fun toLatLng(cvLoc: CvLocation) = LatLng(cvLoc.lat.toDouble(), cvLoc.lon.toDouble())
}