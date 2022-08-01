package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db
import androidx.room.*
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceEntity

@Dao
interface AnyplaceDAO {
  // TODO on conflict? update all except ownership... (unless that was public..)
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSpace(space: SpaceEntity)

  @Query("DELETE FROM spaces")
  fun dropSpaces()

  @Query("SELECT * FROM spaces ORDER BY name ASC")
  fun readSpaces(): List<SpaceEntity>

  @Query("SELECT * FROM spaces WHERE name LIKE '%' || :spaceName || '%' ORDER BY name ASC")
  fun querySpaces(spaceName: String): List<SpaceEntity>

  @Query("SELECT * FROM spaces WHERE type == :spaceType AND ownerShip == :ownership AND name LIKE '%' || :spaceName || '%' ORDER BY name ASC")
  fun querySpacesOwnerAndType(ownership: String, spaceType: String, spaceName: String): List<SpaceEntity>

  @Query("SELECT * FROM spaces WHERE type == :spaceType AND name LIKE '%' || :spaceName || '%' ORDER BY name ASC")
  fun querySpacesType(spaceType: String, spaceName: String): List<SpaceEntity>

  @Query("SELECT * FROM spaces WHERE ownerShip == :ownership AND name LIKE '%' || :spaceName || '%' ORDER BY name ASC")
  fun querySpaceOwner(ownership: String, spaceName: String): List<SpaceEntity>

  @Query("SELECT COUNT(id) FROM spaces")
  fun countSpaces(): Int?
}
