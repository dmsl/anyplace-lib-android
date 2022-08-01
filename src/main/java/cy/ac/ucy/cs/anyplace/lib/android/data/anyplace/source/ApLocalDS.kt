package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.source

import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.FilterSpaces
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.AnyplaceDAO
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.SpaceTypeConverter.Companion.toEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.SpaceTypeConverter.Companion.toSpaces
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceType
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceOwnership
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Space
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
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

  // CLR:PM
  // fun querySpaces(query: FilterSpaces): List<SpaceEntity> {
  //   val MT = ::querySpaces.name
  //   val queryResult = querySpacesInternal(query).collect().
  //   LOG.E(TG, "$MT: rows: ${queryResult.size} query: $query")
  //   return queryResult
  // }

  fun querySpaces(query: FilterSpaces): List<Space> {
    val MT = ::querySpaces.name
    // LOG.W(TG, "$MT: txtFilter: '${query.spaceName}'")

    // val name = query.spaceName.isNotEmpty()
    val filterOwnership = query.ownership != SpaceOwnership.ALL
    val filterType = query.spaceType != SpaceType.ALL

    val ownershipStr = query.ownership.toString().uppercase()
    val typeStr = query.spaceType.toString().uppercase()

    return when  {
      // filter on both user ownership and type
      filterOwnership && filterType -> {
        toSpaces(DAO.querySpacesOwnerAndType(
                query.ownership.toString().uppercase(),
                query.spaceType.toString().uppercase(),
                query.spaceName)).spaces
      }

      filterOwnership -> {  // filter only on ownership
        LOG.E("QUERY: ownership: $ownershipStr")
        toSpaces(DAO.querySpaceOwner(ownershipStr, query.spaceName)).spaces
      }

      filterType -> {  // filter only on type
        LOG.E("QUERY: space type: $typeStr")
        toSpaces(DAO.querySpacesType(typeStr, query.spaceName)).spaces
      }

      // read all spaces
      query.spaceName.isNotEmpty() -> { toSpaces(DAO.querySpaces(query.spaceName)).spaces }

      // no filter: read all spaces
      else -> { toSpaces(DAO.readSpaces()).spaces }
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
