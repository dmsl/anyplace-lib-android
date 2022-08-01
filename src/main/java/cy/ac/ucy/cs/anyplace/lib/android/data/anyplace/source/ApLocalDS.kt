package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.source

import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.SpaceFilter
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.AnyplaceDAO
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.SpaceTypeConverter.Companion.toEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceType
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceOwnership
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Space
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Anyplace Local DataStore (uses SQLite)
 */
class ApLocalDS @Inject constructor(
  private val DAO: AnyplaceDAO) {
  val TG = "ds-local-ap"

  fun hasSpaces(): Boolean {
    return when (DAO.countSpaces()) {
      null, 0 -> false
      else -> true
    }
  }

  fun querySpaces(queryFilter: SpaceFilter): Flow<List<SpaceEntity>> {
    val MT = ::querySpaces.name
    LOG.V3(TG, "$MT: name'${queryFilter.spaceName}'")

    // val name = query.spaceName.isNotEmpty()
    val filterOwnership = queryFilter.ownership != SpaceOwnership.ALL
    val filterType = queryFilter.spaceType != SpaceType.ALL

    val ownershipStr = queryFilter.ownership.toString().uppercase()
    val typeStr = queryFilter.spaceType.toString().uppercase()

    return when  {
      // filter on both user ownership and type
      filterOwnership && filterType -> {
        DAO.querySpacesOwnerAndType(
                queryFilter.ownership.toString().uppercase(),
                queryFilter.spaceType.toString().uppercase(),
                queryFilter.spaceName)
      }

      // filter only on ownership
      filterOwnership -> {
        LOG.V2(TG, "$MT: query: ownership: $ownershipStr")
        DAO.querySpaceOwner(ownershipStr, queryFilter.spaceName)
      }

      // filter only on type
      filterType -> {
        LOG.V2(TG, "$MT: query: space type: $typeStr")
        DAO.querySpacesType(typeStr, queryFilter.spaceName)
      }

      // read all spaces
      queryFilter.spaceName.isNotEmpty() -> { DAO.querySpaces(queryFilter.spaceName) }

      // no filter: read all spaces
      else -> { DAO.readSpaces() }
    }
  }

  suspend fun insertSpace(space: Space, ownership: SpaceOwnership) {
    LOG.V5("insert: ${space.name} : ${space.type}")
    DAO.insertSpace(toEntity(space, ownership))
  }

  fun dropSpaces() {
    DAO.dropSpaces()
  }
}
