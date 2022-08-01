package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.query

import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.SpaceSelectorDS
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.FilterSpaces
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Space
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

class SpacesQueryDB(
        // VM: AnyplaceViewModel,
        private val repo: RepoAP,
        private val dsSpaceSelector: SpaceSelectorDS,
        ) {
  private val TG = "db-query-spaces"

  // val scope = VM.viewModelScope

  /** The spaces that are queried locally, and will end up in the [RecyclerView] */
  var spaces: MutableStateFlow<List<Space>> = MutableStateFlow(emptyList())

  /** Storing the predicates for this query */
  var storedFilter = dsSpaceSelector.readSpaceFilter

  /** This is outdated.. */
  // @Deprecated("outdated structure? use something else..")
  // val searchViewData: MutableLiveData<String> = MutableLiveData()
  var txtQuery: MutableStateFlow<String> = MutableStateFlow("")

  // var resultsLoaded = false
  private var tempFilter = FilterSpaces()

  /** Persist the query (to restore the ui chips - [Chip]) */
  suspend fun persistFilters() {
    dsSpaceSelector.persistFilter(tempFilter)
  }

  var wasReset=false
  suspend fun resetFilters()  {
    val MT = ::resetFilters.name
    LOG.E(TG, MT)
    wasReset=true
    dsSpaceSelector.persistFilter(FilterSpaces())
  }

  /**
   * Saves the query in the ViewModel (not persistent).
   * tempSaveQueryFilters
   */
  fun updateTempFilter(filter: FilterSpaces) {
    val method = ::updateTempFilter.name
    LOG.E(TG, "$method: ${filter.ownership} ${filter.spaceType}")
    tempFilter = filter
  }

  fun printTempFilter() {
    LOG.E(TG, "tempFilter: $tempFilter")
  }

  fun filterChanged(filter: FilterSpaces) = filter != tempFilter

  // fun saveFiltersInMem(query: QuerySelectSpace) { querySelectSpace=query }

  suspend fun runTextQuery(newText: String) {
    tempFilter.spaceName = newText
    // resultsLoaded=false
    runQuery("runTextQuery")
  }

  var firstQuery=true
  // var runInitialQuery = false
  /**
   * Read all spaces
   */
  suspend fun runQuery(from: String) {
    val MT = ::runQuery.name
    val filter = if (firstQuery) storedFilter.first() else tempFilter
    LOG.E(TG, "$MT: FROM: $from: $filter")
    // if (runInitialQuery) {
    //   LOG.E(TG, "$MT: running initial query")
    //   LOG.E(TG, "$MT: ${querySelectSpace.spaceType}")
    //   scope.launch {
    //     runQuery()
    //   repo.local.querySpaces(storedQuery.first())
      spaces.update { repo.local.querySpaces(filter) }
      // resultsLoaded = true
      firstQuery=false
    // }
    // }
    // runInitialQuery = false
  }

  fun hasSpaces() = repo.local.hasSpaces()
}




