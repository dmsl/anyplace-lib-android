package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.source

import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.QuerySelectSpace
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.AnyplaceDAO
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.SpaceTypeConverter.Companion.spaceToEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceType
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceOwnership
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Space
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DataSourceLocal @Inject constructor(
  private val DAO: AnyplaceDAO) {

  // CLR:PM
  // fun readSpacesAll(): Flow<List<SpaceEntity>> {
  //   return DAO.readSpaces()
  // }

  fun querySpaces(query: QuerySelectSpace): Flow<List<SpaceEntity>> {
    LOG.W(TAG, "$METHOD: name'${query.spaceName}'")

    // val name = query.spaceName.isNotEmpty()
    val filterOwnership = query.ownership != SpaceOwnership.ALL
    val filterType = query.spaceType != SpaceType.ALL

    val ownershipStr = query.ownership.toString().uppercase()
    val typeStr = query.spaceType.toString().uppercase()

    return when  {
      // filter on both user ownership and type
      filterOwnership && filterType -> {
        DAO.querySpacesOwnerAndType(
                query.ownership.toString().uppercase(),
                query.spaceType.toString().uppercase(),
                query.spaceName)
      }

      // filter only on ownership
      filterOwnership -> {
        LOG.E("QUERY: ownership: $ownershipStr")
        DAO.querySpaceOwner(ownershipStr, query.spaceName)
      }

      // filter only on type
      filterType -> {
        LOG.E("QUERY: space type: $typeStr")
        DAO.querySpacesType(typeStr, query.spaceName)
      }

      // read all spaces
      query.spaceName.isNotEmpty() -> { DAO.querySpaces(query.spaceName) }

      // no filter: read all spaces
      else -> { DAO.readSpaces() }
    }
  }

  suspend fun insertSpace(space: Space, ownership: SpaceOwnership) {
    LOG.V5("insert: ${space.name} : ${space.type}")
    DAO.insertSpace(spaceToEntity(space, ownership))
  }

  fun dropSpaces() {
    DAO.dropSpaces()
  }
}
