package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.query

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceType
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceOwnership
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.MiscDS
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.SpaceFilter
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.AnyplaceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Querying the [Space]s from SQLITE
 * This code (along with SpaceSelector) follows a more complicated logic..
 * (this is mentioned in [SelectSpaceActivity])
 */
class SpacesQueryDB(
  VM: AnyplaceViewModel,
  private val repo: RepoAP,
  private val dsMisc: MiscDS) {
  val TG = "dbq-spaces"

  val scope = VM.viewModelScope

  @Deprecated("outdated structure? use something else?")
  val searchViewData: MutableLiveData<String> = MutableLiveData()

  /** workaround to force re-running the query when changing a space
   * This is because the app stays open and some state is invalid.
   * (terrible workaround, on terrible code)
   */
  var loaded = false
  private var querySelectSpace = SpaceFilter()

  /**
   * Persistently saves the query (in Misc DataStore)
   */
  fun saveQueryTypeDataStore() = scope.launch(Dispatchers.IO) { dsMisc.saveQuerySpace(querySelectSpace) }

  fun resetQuery()  {
    scope.launch(Dispatchers.IO) { dsMisc.saveQuerySpace(SpaceFilter()) }
  }

  /**
   * Saves the query in the ViewModel (not persistent).
   */
  fun saveQueryTypeTemp(spaceOwnership: SpaceOwnership, ownershipId: Int,
                        spaceType: SpaceType, spaceTypeId: Int) {
    LOG.D(TG,  "Saving query type: $spaceOwnership $spaceType")
    querySelectSpace= SpaceFilter(spaceOwnership, ownershipId, spaceType, spaceTypeId)
  }

  fun saveQueryTypeTemp(queryFilter: SpaceFilter) { querySelectSpace=queryFilter }

  private fun saveQuerySpaceName(newText: String) {
    querySelectSpace.spaceName = newText
    spacesQuery = repo.local.querySpaces(querySelectSpace)
    loaded=false
  }

  fun applyQuery(newText: String) = saveQuerySpaceName(newText)

  var spacesQuery: Flow<List<SpaceEntity>> = MutableStateFlow(emptyList())

  /** Storing the predicates for this query */
  var spaceFilter = dsMisc.spaceFilter

  var runnedInitialQuery = false
  /**
   * Read all spaces
   */
  suspend fun tryInitialQuery() {
    val MT = ::tryInitialQuery.name
    if (!runnedInitialQuery) {
      LOG.W(TG, "$MT: ${querySelectSpace.spaceType}")
      repo.local.querySpaces(spaceFilter.first())
      spacesQuery = repo.local.querySpaces(spaceFilter.first())
    }
    runnedInitialQuery = true
  }
}




