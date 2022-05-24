package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db
import androidx.room.*
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnyplaceDao {
  // TODO on conflict? update all except ownership... (unless that was public..)
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSpace(space: SpaceEntity)

  // instead of LiveData. When it reaches the relevant ViewModel
  // it will be converted to LiveData
  @Query("SELECT * FROM spaces ORDER BY name ASC")
  fun readSpaces(): Flow<List<SpaceEntity>>

  @Query("SELECT * FROM spaces WHERE name LIKE '%' || :spaceName || '%' ORDER BY name ASC")
  fun querySpaces(spaceName: String): Flow<List<SpaceEntity>>

  @Query("SELECT * FROM spaces WHERE type == :spaceType AND ownerShip == :ownership AND name LIKE '%' || :spaceName || '%' ORDER BY name ASC")
  fun querySpacesOwnerType(ownership: String, spaceType: String, spaceName: String): Flow<List<SpaceEntity>>

  @Query("SELECT * FROM spaces WHERE type == :spaceType AND name LIKE '%' || :spaceName || '%' ORDER BY name ASC")
  fun querySpacesType(spaceType: String, spaceName: String): Flow<List<SpaceEntity>>

  @Query("SELECT * FROM spaces WHERE ownerShip == :ownership AND name LIKE '%' || :spaceName || '%' ORDER BY name ASC")
  fun querySpaceOwner(ownership: String, spaceName: String): Flow<List<SpaceEntity>>

}
