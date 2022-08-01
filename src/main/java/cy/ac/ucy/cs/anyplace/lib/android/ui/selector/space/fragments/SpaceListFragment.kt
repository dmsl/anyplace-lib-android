package cy.ac.ucy.cs.anyplace.lib.android.ui.selector.space.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.adapters.SpacesAdapter
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.extensions.dsUserAP
import cy.ac.ucy.cs.anyplace.lib.android.ui.selector.space.SelectSpaceActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.NetworkListener
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.AnyplaceViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Space
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Spaces
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.databinding.FragmentSpacesListBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

fun RecyclerView.executeSafely(func : () -> Unit) {
  if (scrollState != RecyclerView.SCROLL_STATE_IDLE) {
    val animator = itemAnimator
    itemAnimator = null
    func()
    itemAnimator = animator
  } else {
    func()
  }
}

/**
 * This implementation should be discarded..
 * Some things are obsolete. (Observers..)
 * Some things are crashing (and are disabled)
 *
 * It uses NavController
 */
@AndroidEntryPoint
class SpaceListFragment : Fragment() {
  val TG = "frgmt-spacelist"
  /** Adapter responsible for rendering the spaces */
  private val mAdapter by lazy {
    SpacesAdapter(requireActivity().app,
            requireActivity() as SelectSpaceActivity, lifecycleScope) }

  private var _binding: FragmentSpacesListBinding? = null
  private val binding get() = _binding!!
  private lateinit var VM: AnyplaceViewModel
  private lateinit var networkListener: NetworkListener
  private val app by lazy { requireActivity().app }

  private val args by navArgs<SpaceListFragmentArgs>() // delegated to NavArgs (SafeArgs plugin)

  // Called before onCreateView
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val MT = ::onCreate.name
    LOG.W(TG, MT)
    VM = ViewModelProvider(requireActivity())[AnyplaceViewModel::class.java]
  }

  override fun onResume() {
    super.onResume()
    val MT = ::onResume.name
    LOG.W(TG, MT)
    LOG.D3()
    handleBackToFragment()
  }

  override fun onDestroy() {
    super.onDestroy()
    // app.dbqSpaces.resultsLoaded=false
  }

  private fun handleBackToFragment() {
    val MT = ::handleBackToFragment.name
    if(VM.backFromSettings) {
      LOG.E(TG, "$MT: from settings")
      lifecycleScope.launch {
        val user = requireActivity().dsUserAP.read.first()
        if (user.accessToken.isBlank()) {
          requireActivity().finish()
        } else {
          reloadSpacesFromRemote()
        }
      }
    }
  }

  override fun onCreateView(
          inflater: LayoutInflater, container: ViewGroup?,
          savedInstanceState: Bundle?
  ): View {
    val MT = ::onCreateView.name

    LOG.E(TG, "$MT: from settings")

    // Inflate the layout for this fragment
    _binding = FragmentSpacesListBinding.inflate(inflater, container, false)

    // for DataBinding
    binding.lifecycleOwner = this
    binding.bindingVMap=this.VM

    setHasOptionsMenu(true)
    collectNetwork()
    setupRecyclerView()
    setupSpaceDataSources()
    collectTextQueriesFromToolbar()

    return binding.root
  }


  private fun setupRecyclerView() {
    val MT = ::setupRecyclerView.name
    LOG.D2(TG, MT)
    binding.recyclerView.adapter = mAdapter
    binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    // recycler view was randomly crashing
    binding.recyclerView.itemAnimator = null  // BUGFIX: https://stackoverflow.com/a/58540280/776345

    showShimmerEffect()
  }

  /**
   *
   * Reads spaces (either from local or remote)
   * and setups collectors for getting those spaces (either local or remote)
   */
  private fun setupSpaceDataSources() {
    val MT = ::setupSpaceDataSources.name
    LOG.E(TG, MT)

    lifecycleScope.launch (Dispatchers.IO){
      readSpaces()
      LOG.W(TG, "$MT: wiiill setup collect spaces query...")
      collectSpacesQuery()
      collectRemoteResponse()
    }
  }

  private fun collectNetwork() {
    lifecycleScope.launch {
      networkListener = NetworkListener()
      networkListener.checkNetworkAvailability(requireActivity()).collect { status ->
        LOG.D(TG, "Network status: $status")
        VM.networkStatus = status
        // VM.showNetworkStatus()
      }
    }
  }

  var collectingQueries=false
  /**
   * Collecting text queries from the [searchView] in the toolbar
   */
  private fun collectTextQueriesFromToolbar() {
    val MT = ::collectTextQueriesFromToolbar.name
    if (collectingQueries) return
    collectingQueries=true

    lifecycleScope.launch(Dispatchers.IO) {
      app.dbqSpaces.txtQuery.collect { spaceName ->
        LOG.D(TG, "$MT: txtQuery changed: $spaceName")
        if (args.backFromBottomSheet || app.dbqSpaces.firstQuery) {
          LOG.E(TG, "$MT: ignoring (back from bottomsheet)")
          return@collect
        }
        app.dbqSpaces.runTextQuery(spaceName)
      }
    }

    // CLR:PM
    // app.dbqSpaces.searchViewData.observe(viewLifecycleOwner) { spaceName ->
    //   app.dbqSpaces.runTextQuery(spaceName)
    //   // COLLECT:CLR
    //   // lifecycleScope.launch(Dispatchers.IO) {
    //   //   app.dbqSpaces.spacesLocal.collect { spaces ->
    //   //     LOG.E(TG, "Query: ${spaces.size}")
    //   //     updateRecyclerView(spaces)
    //   //   }
    //   // }
    // }
  }

  private fun readSpaces() {
    val MT = ::readSpaces.name
    lifecycleScope.launch(Dispatchers.IO) {

      LOG.E(TG,"$MT: running read spaces...")
      // app.dbqSpaces.storedQuery.firstOrNull { query ->
      // app.dbqSpaces.saveFiltersInMem(query) // WHYT?!?!
      // app.dbqSpaces.readSpaces()

      // if we are back from bottom sheet we must reload
      // !app.dbqSpaces.loaded || THIS WAS  liek that.....

      if (!app.dbqSpaces.hasSpaces()) {
        LOG.E(TG, "$MT: no spaces: remote request")
        readSpacesFromRemote()
        app.dbqSpaces.runQuery("readSpaces1")
      } else if (args.backFromBottomSheet) {
        LOG.E(TG, "$MT: back from bottom sheet")
        app.dbqSpaces.runQuery("readSpaces2")
      } else if (app.dbqSpaces.firstQuery) {
        LOG.E(TG, "$MT: reloading: first query..")
        // INFO was not reloading..
        app.dbqSpaces.runQuery("readSpaces3")
        // LOG.E(TG, "$MT: Not reloading..")
      } else {
        LOG.E(TG, "$MT: Not reloading..")
      }
      // true
      // }
    }
  }

  var collectingSpacesQuery = false
  /**
   * If null then its ignored
   * TODO make this callable from both allSpaces and spacesQuery
   */
  private suspend fun collectSpacesQuery() {
    val MT = ::collectSpacesQuery.name
    if (collectingSpacesQuery) return
    collectingSpacesQuery = true

    app.dbqSpaces.spaces.collect { spaces ->
      LOG.E(TG, "$MT: collected: spaces: ${spaces.size}")
      if (app.dbqSpaces.firstQuery && spaces.isEmpty()) {
        // this is normal
        LOG.W(TG, "$MT: ignoring: no query run yet (so no spaces)")
        return@collect
      }

      updateRecyclerView(spaces)
    }
  }

  private suspend fun readSpacesFromRemote()  {
    val MT = ::readSpacesFromRemote.name
    LOG.E(TG, MT)
    VM.nwSpacesGet.safeCall()
  }

  private suspend fun reloadSpacesFromRemote() {
    val MT = ::reloadSpacesFromRemote.name
    LOG.E(TG, MT)

    VM.unsetBackFromSettings()
    // app.dbqSpaces.resultsLoaded = false
    readSpacesFromRemote()
  }

  /**
   * Loads the recycler view with content from the database
   */
  private fun updateRecyclerView(spaces: List<Space>) {
    val MT = ::updateRecyclerView.name
    // LOG.E(TG, MT)

    // if (!app.dbqSpaces.resultsLoaded) {
    // LOG.E(TG, "$MT: results not loaded: hide shimmer")
    hideShimmerEffect()
    // LOG.E(TG, "$MT: setting data: sp: ${spaces.size}")
    spaces.let { mAdapter.setData(Spaces(spaces)) }

    if (spaces.isNotEmpty()) {
      LOG.E(TG, "$MT: Found ${spaces.size} spaces. (filter persisted)")
      hideErrorMsg()
      lifecycleScope.launch(Dispatchers.IO) {
        app.dbqSpaces.persistFilters()
      }
    } else {
      LOG.E(TG, "$MT: no results. (filter reset)")
      lifecycleScope.launch(Dispatchers.IO) {
        app.dbqSpaces.resetFilters()
      }
      showNoResults()
    }
  }

  private fun hideErrorMsg() {
    lifecycleScope.launch(Dispatchers.Main) {
      binding.errorImageView.visibility = View.INVISIBLE
      binding.errorTextView.visibility = View.INVISIBLE
    }
  }

  private fun showNoResults() {
    lifecycleScope.launch(Dispatchers.Main) {
      binding.errorImageView.visibility = View.VISIBLE
      binding.errorTextView.visibility = View.VISIBLE
      binding.errorTextView.text = getText(R.string.no_results)
    }
  }

  var collectingRemoteSpaces=false

  /**
   * When a remote request (API) was made, collect the response
   * - on success: means we fetched new spaces and stored them locally
   *   - in that case, run a query so they can finally be presented
   * - otherwise: show relevant error.
   */
  private suspend fun collectRemoteResponse() {
    val MT = ::collectRemoteResponse.name
    LOG.E(TG, "$MT: setup collector")
    if (collectingRemoteSpaces) return
    collectingRemoteSpaces=true

    LOG.W(TG, "$MT: setup collector: st 2")

    VM.nwSpacesGet.resp.collect { response ->
      LOG.W(TG, "$MT: collected $response")
      LOG.W(TG, "$MT: collected $response")

      when (response) {
        is NetworkResult.Success -> {
          // don't hide the shimmer effect now, as still have to query the DB
          // before showing any results
          LOG.E(TG, "$MT: success: running query")
          LOG.W(TG, "success: running query")
          app.dbqSpaces.runQuery("collectedRemoteResp: success")
        }
        is NetworkResult.Error -> {
          LOG.E(TG, "$MT: error")
          // if (app.dbqSpaces.resultsLoaded) LOG.E(TG, "$MT: Error response: after showed success.")
          mAdapter.clearData()
          hideShimmerEffect()
          val errMsg = response.message.toString()
          if (activity != null) {
            app.showToast(lifecycleScope, errMsg)
          }

          // app.dbqSpaces.runQuery()
          LOG.E(TG, "$MT: $errMsg")
        }
        is NetworkResult.Loading -> {
          LOG.E(TG, "$MT: loading")
          showShimmerEffect()
        }
        else -> {}
      }
    }
  }

  /**
   * Showing a Shimmer Effect while buildings are being fetched/loaded
   */
  private fun showShimmerEffect() {
    lifecycleScope.launch(Dispatchers.Main) {
      binding.shimmerLayout.startShimmer()
      binding.shimmerLayout.visibility = View.VISIBLE
      binding.recyclerView.visibility = View.GONE
    }
  }

  /**
   * Hiding the shimmer effect and showing the recycler view
   */
  private fun hideShimmerEffect() {
    lifecycleScope.launch(Dispatchers.Main) {
      binding.shimmerLayout.stopShimmer()
      binding.shimmerLayout.visibility = View.GONE
      binding.recyclerView.visibility = View.VISIBLE
    }
  }
}