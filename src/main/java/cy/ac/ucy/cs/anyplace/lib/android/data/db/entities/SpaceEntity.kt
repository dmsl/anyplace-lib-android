package cy.ac.ucy.cs.anyplace.lib.android.data.db.entities
import androidx.room.Entity
import androidx.room.PrimaryKey
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.DB_TBL_SPACES

enum class UserOwnership {
  PUBLIC,      // can be accessed by anyone
  OWNED,       // it is owned by user
  ACCESSIBLE,   // it is accessible by user due to admin/mod priviledges
  __DONT_UPDATE  // TODO INFO if it's this one, then dont update it
}

enum class SpaceType {
  BUILDING,
  VESSEL,
  UNKNOWN
}

// Based on Space
@Entity(tableName = DB_TBL_SPACES)
data class SpaceEntity(
  // @PrimaryKey
  @PrimaryKey(autoGenerate = false)
  var id: String,
  val bucode: String?="",
  val name: String,
  val description: String?="",
  val address: String?="",
  val coordinatesLat: String, // TODO convert to LtnLng?
  val coordinatesLon: String,
  val url: String?="",
  val ownerShip: UserOwnership  // additional entry
  )