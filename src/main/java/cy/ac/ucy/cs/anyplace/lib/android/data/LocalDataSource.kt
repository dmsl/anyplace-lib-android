package cy.ac.ucy.cs.anyplace.lib.android.data

import cy.ac.ucy.cs.anyplace.lib.android.data.db.AnyplaceDao
import cy.ac.ucy.cs.anyplace.lib.android.data.db.SpaceTypeConverter.Companion.spaceToEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.db.entities.SpaceEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.db.entities.UserOwnership
import cy.ac.ucy.cs.anyplace.lib.models.Space
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LocalDataSource @Inject constructor(
  private val anyplaceDao: AnyplaceDao) {

  // TODO must be converted to multiple objects...
  fun readSpaces(): Flow<List<SpaceEntity>> {
    return anyplaceDao.readSpaces()
  }

  fun querySpaces(): Flow<List<SpaceEntity>> {
    return anyplaceDao.querySpaces() // TODO
  }

  suspend fun insertSpace(space: Space, ownership: UserOwnership) {
    anyplaceDao.insertSpace(spaceToEntity(space, ownership))
  }

}
