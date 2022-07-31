package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.source

import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.QuerySelectSpace
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.AnyplaceDAO
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.SpaceTypeConverter.Companion.spaceToEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceType
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.UserOwnership
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Space
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DataSourceLocal @Inject constructor(
  private val DAO: AnyplaceDAO) {

  // TODO must be converted to multiple objects...
  fun readSpaces(): Flow<List<SpaceEntity>> {
    return DAO.readSpaces()
  }

  fun querySpaces(query: QuerySelectSpace): Flow<List<SpaceEntity>> {
    LOG.W(TAG, "querySpaces: name'${query.spaceName}'")

    // val name = query.spaceName.isNotEmpty()
    val ownership = query.ownership != UserOwnership.PUBLIC
    val type = query.spaceType != SpaceType.ALL

    val ownershipStr = query.ownership.toString().uppercase()
    val typeStr = query.spaceType.toString().uppercase()
    if (ownership && type) {
      return DAO.querySpacesOwnerType(
        query.ownership.toString().uppercase(),
        query.spaceType.toString().uppercase(),
        query.spaceName)
    } else if (ownership) {
      LOG.E("QUERY: ownership: $ownershipStr")
      return DAO.querySpaceOwner(ownershipStr, query.spaceName)
    } else if (type) {
      LOG.E("QUERY: space type: $typeStr")
      return DAO.querySpacesType(typeStr, query.spaceName)
    }

    return DAO.querySpaces(query.spaceName)  // spaceName
  }

  suspend fun insertSpace(space: Space, ownership: UserOwnership) {
    LOG.V5("insert: ${space.name} : ${space.type}")
    DAO.insertSpace(spaceToEntity(space, ownership))
  }

  fun dropSpaces() {
    DAO.dropSpaces()
  }
}
