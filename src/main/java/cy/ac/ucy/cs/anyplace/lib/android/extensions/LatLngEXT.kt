package cy.ac.ucy.cs.anyplace.lib.android.extensions

import com.google.android.gms.maps.model.LatLng
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Coord

fun LatLng.toCoord(level: Int) : Coord = Coord(latitude, longitude, level)
