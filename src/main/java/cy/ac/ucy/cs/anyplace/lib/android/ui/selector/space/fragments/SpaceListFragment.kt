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
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
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
  private val TAG = SpaceListFragment::class.java.simpleName
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

    LOG.D3()
  }

  override fun onResume() {
    super.onResume()
    LOG.D3()
    handleBackToFragment()
  }

  override fun onDestroy() {
    super.onDestroy()
    VM.dbqSpaces.loaded=false
  }

  private fun handleBackToFragment() {
    if(VM.backFromSettings) {
      LOG.D2(TAG, "$METHOD: from settings")
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
    LOG.D(TAG, "onCreateView")

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
    LOG.D3()
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
    LOG.D3()

    lifecycleScope.launchWhenStarted { readDatabase() }
  }

  private fun collectNetwork() {
    lifecycleScope.launch {
    networkListener = NetworkListener()
      networkListener.checkNetworkAvailability(requireActivity()).collect { status ->
        LOG.D(TAG, "Network status: $status")
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
        LOG.D(TAG, "searchview: $spaceName")

        VM.dbqSpaces.applyQuery(spaceName)
        lifecycleScope.launch(Dispatchers.IO) {
          VM.dbqSpaces.readSpacesQuery.collect { query ->
            LOG.E(TAG, "Query: ${query.size}")
            loadDatabaseResults(query)
          }
        }
      }
  }

  private fun readDatabase() {
    LOG.E()
    lifecycleScope.launch {
      VM.dbqSpaces.storedQuery.firstOrNull { query ->
        VM.dbqSpaces.saveQueryTypeTemp(query)
        VM.dbqSpaces.runInitialQuery()

        LOG.E("$METHOD: loaded spaces: ${!VM.dbqSpaces.loaded} : back from bottom: ${args.backFromBottomSheet}" +
            ": " + (!VM.dbqSpaces.loaded || args.backFromBottomSheet) + "\n")

        // if we are back from bottom sheet we must reload
        if (!VM.dbqSpaces.loaded || args.backFromBottomSheet) {
          if (args.backFromBottomSheet) {
            LOG.E("$METHOD: Back from bottom sheet\n")
            LOG.D(TAG, "  -> loadSpacesQuery")
            loadSpacesQuery()
          } else {
            VM.dbqSpaces.readSpacesQuery.collect { spaces ->
              if (spaces.isNotEmpty()) {
                LOG.D2(TAG, "forced query")
                loadSpacesQuery()
              } else {
                LOG.D2(TAG, "  -> requestRemoteSpaceData")
                requestRemoteSpacesData()
                collectSpaces()
              }
            }
          }
        } else {
          LOG.W(TAG, "Not reloading...")
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
    lifecycleScope.launch(Dispatchers.IO) {
      VM.dbqSpaces.readSpacesQuery.collect { query ->
        LOG.D(TAG, "Query: ${query.size}")
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
    LOG.W()
    // if (!VM.dbqSpaces.loaded || !VM.dbqSpaces.runnedInitialQuery) {
    hideShimmerEffect()
    spaces.let { mAdapter.setData(toSpaces(spaces)) }
    if (spaces.isNotEmpty()) {
      LOG.V3(TAG, "Found ${spaces.size} spaces.")
      hideErrorMsg()
    } else {
      showErrorMsg("No results.")
      VM.dbqSpaces.resetQuery()
    }
    // } else {
    //   LOG.W(TAG, "loadDatabaseResults: skipped loading..")
    // }
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
    if (collectingRemoteSpaces) return
    collectingRemoteSpaces=true

    val method = METHOD
    lifecycleScope.launch(Dispatchers.IO) {
      VM.nwSpacesGet.resp.collectLatest { response ->
        LOG.D(TAG, method)

        when (response) {
          is NetworkResult.Success -> {
            hideShimmerEffect()
            response.data?.let { mAdapter.setData(it) }
            VM.dbqSpaces.loaded = true // TODO set this from db cache as welL!
          }
          is NetworkResult.Error -> {
            if (VM.dbqSpaces.loaded) LOG.E("Error response: after showed success.")
            mAdapter.clearData()
            hideShimmerEffect()
            // if(!showedApiData) { showShimmerEffect() }
            // loadDataFromCache() TODO: DB ?
            val errMsg = response.message.toString()
            if (activity != null) {
              app.showToast(lifecycleScope, errMsg)
            }
            LOG.E(TAG, errMsg)
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
    LOG.V5()

    lifecycleScope.launch(Dispatchers.Main) {
      binding.shimmerLayout.startShimmer()
      binding.shimmerLayout.visibility = View.VISIBLE
      binding.recyclerView.visibility = View.GONE
    }
  }

  private fun hideShimmerEffect() {
    LOG.V5()
    lifecycleScope.launch(Dispatchers.Main) {
      binding.shimmerLayout.stopShimmer()
      binding.shimmerLayout.visibility = View.GONE
      binding.recyclerView.visibility = View.VISIBLE
    }
  }
}