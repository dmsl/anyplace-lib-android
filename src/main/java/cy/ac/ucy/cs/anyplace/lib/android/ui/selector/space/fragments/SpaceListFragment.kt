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
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.adapters.SpacesAdapter
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.SpaceTypeConverter.Companion.toSpaces
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceEntity
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.extensions.dsUserAP
import cy.ac.ucy.cs.anyplace.lib.android.ui.selector.space.SelectSpaceActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.NetworkListener
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.AnyplaceViewModel
import cy.ac.ucy.cs.anyplace.lib.databinding.FragmentSpacesListBinding
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
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
  private val TG = "frgmt-space-list"
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
    VM = ViewModelProvider(requireActivity())[AnyplaceViewModel::class.java]
    val MT = ::onCreate.name
    LOG.D(TG, MT)
  }

  override fun onResume() {
    super.onResume()
    val MT = ::onResume.name
    LOG.D(TG, MT)
    
    handleBackToFragment()
  }

  override fun onDestroy() {
    super.onDestroy()
    VM.dbqSpaces.loaded=false
  }

  private fun handleBackToFragment() {
    val MT = ::handleBackToFragment.name

    if(VM.backFromSettings) {
      LOG.D2(TG, "$MT: from settings")
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
    LOG.D(TG, "onCreateView")

    // Inflate the layout for this fragment
    _binding = FragmentSpacesListBinding.inflate(inflater, container, false)

    // for DataBinding
    binding.lifecycleOwner = this
    binding.bindingVMap=this.VM

    setHasOptionsMenu(true)
    collectNetwork()
    setupRecyclerView()
    loadSpaces()
    collectSearchView()

    return binding.root
  }

  private fun setupRecyclerView() {
    val MT = ::setupRecyclerView.name
    LOG.D3(TG, MT)
    binding.recyclerView.adapter = mAdapter
    binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    binding.recyclerView.itemAnimator = null  // BUGFIX: https://stackoverflow.com/a/58540280/776345

    showShimmerEffect()
  }

  /**
   * Listen for network changes, and request data once back online
   * Runs on the very first time (when Activity is created), and then on Network changes.
   */
  private fun loadSpaces() {
    val MT = ::loadSpaces.name
    LOG.D3(TG, MT)

    lifecycleScope.launchWhenStarted { readDatabase() }
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
  private fun collectSearchView() {
    if (collectingQueries) return
    collectingQueries=true
      VM.dbqSpaces.searchViewData.observe(viewLifecycleOwner) { spaceName ->
        LOG.D(TG, "searchview: $spaceName")

        VM.dbqSpaces.applyQuery(spaceName)
        lifecycleScope.launch(Dispatchers.IO) {
          VM.dbqSpaces.spacesQuery.collect { query ->
            LOG.W(TG, "Query: ${query.size}")
            loadDatabaseResults(query)
          }
        }
      }
  }

  private fun readDatabase() {
    val MT = ::readDatabase.name
    LOG.W(TG, MT)
    lifecycleScope.launch(Dispatchers.IO) {
      VM.dbqSpaces.spaceFilter.firstOrNull { query ->
        VM.dbqSpaces.saveQueryTypeTemp(query)
        VM.dbqSpaces.tryInitialQuery()
        val emptyDB = !app.repoAP.local.hasSpaces()

        LOG.E("$MT: must load spaces: ${!VM.dbqSpaces.loaded}. Back from bsheet: ${args.backFromBottomSheet}")

        if (emptyDB) { // workaround: emptydb
          LOG.E(TG, "$MT: db was empty. reloading from remote..")
          reloadSpacesFromRemote()
          collectSpaces()
          VM.dbqSpaces.runnedInitialQuery=false
          VM.dbqSpaces.tryInitialQuery()
          loadSpacesQuery()
        } else if (app.backToSpaceSelectorFromOtherActivities) { // workaround: force run query again
          app.backToSpaceSelectorFromOtherActivities=false
          VM.dbqSpaces.runnedInitialQuery=false
          VM.dbqSpaces.tryInitialQuery()
          loadSpacesQuery()
          // if we are back from bottom sheet we must reload
        } else if (!VM.dbqSpaces.loaded || args.backFromBottomSheet) {
          if (args.backFromBottomSheet) {
            LOG.E("$MT: Back from bottom sheet\n")
            LOG.E(TG, "$MT:  -> loadSpacesQuery")
            loadSpacesQuery()
          } else {
            VM.dbqSpaces.spacesQuery.collect { spaces ->
              if (spaces.isNotEmpty()) {
                LOG.E(TG, "$MT: forced query")
                loadSpacesQuery()
              } else {
                LOG.E(TG, "$MT:  -> requestRemoteSpaceData")
                requestRemoteSpacesData()
                collectSpaces()
              }
            }
          }
        } else {
          LOG.W(TG, "$MT: Not reloading...")
        }
        true
      }
    }
  }

  /**
   * If null then its ignored
   * TODO make this callable from both allSpaces and spacesQuery
   */
  private fun loadSpacesQuery() {
    val MT = ::loadSpacesQuery.name
    lifecycleScope.launch(Dispatchers.IO) {
      VM.dbqSpaces.spacesQuery.collect { query ->
        LOG.D2(TG, "$MT: query: ${query.size}")
        loadDatabaseResults(query)
      }
    }
  }

  private fun requestRemoteSpacesData() = VM.nwSpacesGet.safeCall()

  private fun reloadSpacesFromRemote() {
    VM.unsetBackFromSettings()
    VM.dbqSpaces.loaded = false
    requestRemoteSpacesData()
  }

  /**
   * Loads the recycler view with content from the database
   */
  private fun loadDatabaseResults(spaces: List<SpaceEntity>) {
    val MT = ::loadDatabaseResults.name
    LOG.D2(TG, MT)

    hideShimmerEffect()
    spaces.let { mAdapter.setData(toSpaces(spaces)) }
    if (spaces.isNotEmpty()) {
      LOG.V3(TG, "MT: Found ${spaces.size} spaces.")
      hideErrorMsg()
    } else {
      showErrorMsg("No results.")
      VM.dbqSpaces.resetQuery()
    }
    VM.dbqSpaces.loaded = true
  }

  private fun hideErrorMsg() {
    lifecycleScope.launch(Dispatchers.Main) {
      binding.errorImageView.visibility = View.INVISIBLE
      binding.errorTextView.visibility = View.INVISIBLE
    }
  }

  private fun showErrorMsg(msg: String) {
    lifecycleScope.launch(Dispatchers.Main) {
      binding.errorImageView.visibility = View.VISIBLE
      binding.errorTextView.visibility = View.VISIBLE
      binding.errorTextView.text = msg
    }
  }

  var collectingRemoteSpaces=false
  private fun collectSpaces() {
    val MT = ::collectSpaces.name
    if (collectingRemoteSpaces) return
    collectingRemoteSpaces=true

    lifecycleScope.launch(Dispatchers.IO) {
      VM.nwSpacesGet.resp.collectLatest { response ->
        LOG.D(TG, MT)

        when (response) {
          is NetworkResult.Success -> {
            hideShimmerEffect()
            response.data?.let { mAdapter.setData(it) }
            VM.dbqSpaces.loaded = true
          }
          is NetworkResult.Error -> {
            if (VM.dbqSpaces.loaded) LOG.E("Error response: after showed success.")
            mAdapter.clearData()
            hideShimmerEffect()
            val errMsg = response.message.toString()
            if (activity != null) {
              app.showToast(lifecycleScope, errMsg)
            }
            LOG.E(TG, errMsg)
          }
          is NetworkResult.Loading -> {
            showShimmerEffect()
          }
          else -> {}
        }
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

  private fun hideShimmerEffect() {
    lifecycleScope.launch(Dispatchers.Main) {
      binding.shimmerLayout.stopShimmer()
      binding.shimmerLayout.visibility = View.GONE
      binding.recyclerView.visibility = View.VISIBLE
    }
  }
}