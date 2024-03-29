package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities
import androidx.room.Entity
import androidx.room.PrimaryKey
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.DB_TBL_SPACES

/**
 * Who owns a space.
 * The backend does not provide directly this information.
 * But by fetching the spaces in this order, this information is derived
 * - user owned spaces
 * - user accessible spaces (owned & coOwned)
 * - public spaces
 *
 * All these can be put to ROOM/SQLite, along with [SpaceOwnership] information
 * - no duplicates will be inserted
 *
 * See [SpacesGetNW.storeSpaces] for a DEMO example.
 *
 */
enum class SpaceOwnership {
  ALL,         // Ignore ownwership, show all buildings
  PUBLIC,      // can be accessed by anyone
  OWNED,       // it is owned by user
  ACCESSIBLE,   // it is accessible by user due to admin/mod priviledges
  __DONT_UPDATE,  // TODO INFO if it's this one, then dont update it
}

enum class SpaceType {
  ALL,
  BUILDING,
  VESSEL,
  UNKNOWN,
}

// Based on Space
// INFO order is significant as we convert from SpaceEntity to Space and vice versa
@Entity(tableName = DB_TBL_SPACES)
data class SpaceEntity(
  @PrimaryKey(autoGenerate = false)
  var id: String,
  val type: String="unknown",
  val bucode: String?="",
  val name: String,
  val description: String?="",
  val address: String?="",
  val coordinatesLat: String,
  val coordinatesLon: String,
  val url: String?="",
  val ownerShip: SpaceOwnership  // additional entry
  )