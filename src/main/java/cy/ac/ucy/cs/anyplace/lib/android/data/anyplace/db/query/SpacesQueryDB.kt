package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.query

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceType
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.UserOwnership
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.MiscDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.QuerySelectSpace
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.AnyplaceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SpacesQueryDB(
        private val app: AnyplaceApp,
        VM: AnyplaceViewModel,
        private val repo: RepoAP,
        private val dsMisc: MiscDataStore,
        ) {

  val scope = VM.viewModelScope

  /** This is outdated.. */
  @Deprecated("outdated structure? use something else..")
  val searchViewData: MutableLiveData<String> = MutableLiveData()
  private val C by lazy { CONST(app.applicationContext) }

  var loaded = false // TODO move in SpaceVM
  private var querySelectSpace = QuerySelectSpace()

  //// DATASTORE
  /**
   * Persistently saves the query (in Misc DataStore)
   */
  fun saveQueryTypeDataStore() =
          scope.launch(Dispatchers.IO) { dsMisc.saveQuerySpace(querySelectSpace) }

  fun resetQuery()  {
    scope.launch(Dispatchers.IO) {
      dsMisc.saveQuerySpace(QuerySelectSpace())
    }
  }

  /**
   * Saves the query in the ViewModel (not persistent).
   */
  fun saveQueryTypeTemp(userOwnership: UserOwnership, ownershipId: Int,
                        spaceType: SpaceType, spaceTypeId: Int) {
    LOG.D(TAG, "Saving query type: $userOwnership $spaceType")
    querySelectSpace= QuerySelectSpace(userOwnership, ownershipId, spaceType, spaceTypeId)
  }

  fun saveQueryTypeTemp(query: QuerySelectSpace) { querySelectSpace=query }

  private fun saveQuerySpaceName(newText: String) {
    querySelectSpace.spaceName = newText
    readSpacesQuery = repo.local.querySpaces(querySelectSpace)
    loaded=false
  }

  fun applyQuery(newText: String) = saveQuerySpaceName(newText)

  //// ROOM
  var readSpacesQuery: Flow<List<SpaceEntity>> = MutableStateFlow(emptyList())

  /** Storing the predicates for this query */
  var storedQuery = dsMisc.readQuerySpace

  private var runnedFirstedQuery = false
  /**
   * Read all spaces
   */
  fun runFirstQuery() {
    if (!runnedFirstedQuery) {
      LOG.E(TAG, "$METHOD: ${querySelectSpace.spaceType}")
      readSpacesQuery = repo.local.readSpaces() // just read ll spaces..

      // scope.launch {
      //  repo.local.querySpaces(storedSpaceQuery.first())
      // }
    }
    runnedFirstedQuery = true
  }
}




