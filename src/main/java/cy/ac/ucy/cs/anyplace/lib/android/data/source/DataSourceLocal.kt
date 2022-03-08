package cy.ac.ucy.cs.anyplace.lib.android.data.source

import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.store.QuerySelectSpace
import cy.ac.ucy.cs.anyplace.lib.android.data.db.AnyplaceDao
import cy.ac.ucy.cs.anyplace.lib.android.data.db.SpaceTypeConverter.Companion.spaceToEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.db.entities.SpaceEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.db.entities.SpaceType
import cy.ac.ucy.cs.anyplace.lib.android.data.db.entities.UserOwnership
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.models.Space
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DataSourceLocal @Inject constructor(
  private val anyplaceDao: AnyplaceDao) {

  // TODO must be converted to multiple objects...
  fun readSpaces(): Flow<List<SpaceEntity>> {
    return anyplaceDao.readSpaces()
  }

  fun querySpaces(query: QuerySelectSpace): Flow<List<SpaceEntity>> {
    LOG.D3(TAG, "querySpaces: name'${query.spaceName}'")

    // val name = query.spaceName.isNotEmpty()
    val ownership = query.ownership != UserOwnership.PUBLIC
    val type = query.spaceType != SpaceType.ALL

    var ownershipStr = query.ownership.toString().uppercase()
    val typeStr = query.spaceType.toString().uppercase()
    if (ownership && type) {
      return anyplaceDao.querySpacesOwnerType(
        query.ownership.toString().uppercase(),
        query.spaceType.toString().uppercase(),
        query.spaceName)
    } else if (ownership) {
      LOG.E("QUERY: ownership: $ownershipStr")
      return anyplaceDao.querySpaceOwner(ownershipStr, query.spaceName)
    } else if (type) {
      LOG.E("QUERY: space type: $typeStr")
      return anyplaceDao.querySpacesType(typeStr, query.spaceName)
    }

    return anyplaceDao.querySpaces(query.spaceName)  // spaceName
  }

  suspend fun insertSpace(space: Space, ownership: UserOwnership) {
    LOG.V5("insert: ${space.name} : ${space.type}")
    anyplaceDao.insertSpace(spaceToEntity(space, ownership))
  }
}
