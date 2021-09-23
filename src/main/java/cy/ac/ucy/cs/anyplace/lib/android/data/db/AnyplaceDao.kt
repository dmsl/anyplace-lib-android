package cy.ac.ucy.cs.anyplace.lib.android.data.db
import androidx.room.*
import cy.ac.ucy.cs.anyplace.lib.android.data.db.entities.SpaceEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.db.entities.UserOwnership
import cy.ac.ucy.cs.anyplace.lib.models.Space
// import com.example.foody.data.db.entities.FavouritesEntity
// import com.example.foody.data.db.entities.FoodJokeEntity
// import com.example.foody.data.db.entities.RecipesEntity
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

  @Query("SELECT * FROM spaces WHERE name LIKE '%Computer%' ORDER BY name ASC")
  fun querySpaces(): Flow<List<SpaceEntity>>

}
