package cy.ac.ucy.cs.anyplace.lib.android.ui.selector.space.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.adapters.SpacesAdapter
import cy.ac.ucy.cs.anyplace.lib.android.data.db.SpaceTypeConverter.Companion.entityToSpaces
import cy.ac.ucy.cs.anyplace.lib.android.data.db.entities.SpaceEntity
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.userDataStoreDS
import cy.ac.ucy.cs.anyplace.lib.android.extensions.observeOnce
import cy.ac.ucy.cs.anyplace.lib.android.utils.NetworkListener
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.MainViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.SpacesViewModel
import cy.ac.ucy.cs.anyplace.lib.databinding.FragmentSpacesListBinding
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SpaceListFragment : Fragment() {
  private val TAG = SpaceListFragment::class.java.simpleName
  private var _binding: FragmentSpacesListBinding? = null
  private val binding get() = _binding!!
  private val mAdapter by lazy { SpacesAdapter() }
  private lateinit var mainViewModel: MainViewModel
  private lateinit var spaceViewModel: SpacesViewModel
  private lateinit var networkListener: NetworkListener

  private val args by navArgs<SpaceListFragmentArgs>() // delegated to NavArgs (SafeArgs plugin)

  // private lateinit var mView: View // CLR:PM

  // Called before onCreateView
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
    spaceViewModel = ViewModelProvider(requireActivity()).get(SpacesViewModel::class.java)
    LOG.D3(TAG, "SpaceListFragment: onCreate")
  }

  override fun onResume() {
    super.onResume()
    LOG.D3(TAG, "onResume")
    handleBackToFragment()
  }

  override fun onDestroy() {
    super.onDestroy()
    spaceViewModel.loadedSpaces = false // TODO:PM replace to loadedSpaces
  }

  // TODO:PM move to SpaceViewModel
  private fun handleBackToFragment() {
    if(mainViewModel.backFromSettings) {
      LOG.D2(TAG, "handleBackToFragment: from settings")
      lifecycleScope.launch {
        val user = requireActivity().userDataStoreDS.readUser.first()
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

    binding.lifecycleOwner = this // for DataBinding
    binding.spacesViewModel = this.spaceViewModel

    setHasOptionsMenu(true)
    observeNetworkChanges()
    setupRecyclerView()
    loadSpaces()
    setupFab()

    observeSearchViewChanges()

    return binding.root
  }

  private fun setupFab() {
    binding.fabFilterSpaces.setOnClickListener {
      if(spaceViewModel.loadedSpaces) {
        findNavController().navigate(R.id.action_spacesListFragment_to_spaceFilterBottomSheet)
      } else {
        Toast.makeText(context, "No spaces loaded.", Toast.LENGTH_SHORT).show()
      }
    }
  }

  private fun setupRecyclerView() {
    LOG.D3(TAG, "setupRecyclerView")
    binding.recyclerView.adapter = mAdapter
    binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    showShimmerEffect()
  }

  /**
   * Listen for network changes, and request data once back online
   * Runs on the very first time (when Activity is created), and then on Network changes.
   */
  private fun loadSpaces() {
    LOG.D(TAG, "loadSpaces")

    lifecycleScope.launchWhenStarted {
      LOG.D("loadSpaces: { launchWhenStarted }")
      readDatabase()
    }
  }

  private fun observeNetworkChanges() {
    lifecycleScope.launch {
    networkListener = NetworkListener()
      networkListener.checkNetworkAvailability(requireActivity()).collect { status ->
        LOG.D(TAG, "Network status: $status")
        mainViewModel.networkStatus = status
        // mainViewModel.showNetworkStatus()
      }
    }
  }

  private fun observeSearchViewChanges() {
    lifecycleScope.launch {
      spaceViewModel.searchViewData.observe(viewLifecycleOwner) { spaceName ->
        LOG.D(TAG, "searchview: $spaceName")

        spaceViewModel.readSpacesQuery.removeObservers(viewLifecycleOwner)
        spaceViewModel.applyQuery(spaceName)
        spaceViewModel.readSpacesQuery.observeOnce(viewLifecycleOwner) { query ->
          LOG.D(TAG, "Query: ${query.size}")
          loadDatabaseResults(query)
        }
      }
    }
  }

  private fun readDatabase() {
    LOG.E(TAG, "readDatabase:")
    lifecycleScope.launch {

      spaceViewModel.storedSpaceQuery.firstOrNull { query ->
        LOG.E(TAG, "RunFirstQu")
        spaceViewModel.saveQueryTypeTemp(query)
        spaceViewModel.runFirstQuery()

        LOG.E("readDatabase: loaded spaces: ${!spaceViewModel.loadedSpaces} : bottom: ${args.backFromBottomSheet}" +
            ": " + (!spaceViewModel.loadedSpaces || args.backFromBottomSheet) + "\n")

        // if we are back from bottom sheet we must reload
        if (!spaceViewModel.loadedSpaces || args.backFromBottomSheet) {
          if (args.backFromBottomSheet) {
            LOG.E("readDatabase: Back from bottom sheet\n")
            LOG.D(TAG, "readDatabase -> loadSpacesQuery")
            loadSpacesQuery()
          } else {
            spaceViewModel.readSpacesQuery.observeOnce(viewLifecycleOwner) { spaces ->
              if (spaces.isNotEmpty()) {
                LOG.D2(TAG, "forced query")
                loadSpacesQuery()
              } else {

                LOG.D2(TAG, "readDatabase -> requestRemoteSpaceData")
                requestRemoteSpacesData()
                observeRemoteSpacesResponse()
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
   * TODO:PM make this callable from both allSpaces and spacesQuery
   */
  private fun loadSpacesQuery() {
    spaceViewModel.readSpacesQuery.observeOnce(viewLifecycleOwner) { query ->
      LOG.D(TAG, "Query: ${query.size}")
      loadDatabaseResults(query)
    }
  }

  private fun requestRemoteSpacesData() = spaceViewModel.getSpaces()

  private fun reloadSpacesFromRemote() {
    mainViewModel.unsetBackFromSettings()
    spaceViewModel.loadedSpaces = false
    requestRemoteSpacesData()
  }

  /**
   * Loads the recycler view with content from the database
   */
  private fun loadDatabaseResults(spaces: List<SpaceEntity>) {
    LOG.D(TAG, "loadDatabaseResults")
    if (!spaceViewModel.loadedSpaces) {
      hideShimmerEffect()
      spaces.let { mAdapter.setData(entityToSpaces(spaces)) }
      if (spaces.isNotEmpty()) {
        LOG.V3(TAG, "Found ${spaces.size} spaces.")
        hideErrorMsg()
      } else {
        showErrorMsg("No results.")
        spaceViewModel.resetQuery()
      }
    } else {
      LOG.W(TAG, "loadDatabaseResults: skipped loading..")
    }
    spaceViewModel.loadedSpaces = true
  }

  private fun hideErrorMsg() {
    binding.errorImageView.visibility = View.INVISIBLE
    binding.errorTextView.visibility = View.INVISIBLE
  }

  private fun showErrorMsg(msg: String) {
    binding.errorImageView.visibility = View.VISIBLE
    binding.errorTextView.visibility = View.VISIBLE
    binding.errorTextView.text = msg
  }

  private fun observeRemoteSpacesResponse() {
    spaceViewModel.spacesResponse.observe(viewLifecycleOwner,  { response ->
      LOG.D(TAG, "observeSpacesResponse")
      when (response) {
        is NetworkResult.Success -> {
          hideShimmerEffect()
          response.data?.let { mAdapter.setData(it) }
          spaceViewModel.loadedSpaces = true // TODO Set this from db cache as welL!
        }
        is NetworkResult.Error -> {
          if(spaceViewModel.loadedSpaces) LOG.E("Error response: after showed success.")
          mAdapter.clearData()
          hideShimmerEffect()
          // if(!showedApiData) {
          //   showShimmerEffect()
          // }
          // loadDataFromCache() TODO :DB
          Toast.makeText(requireContext(), response.message.toString(), Toast.LENGTH_SHORT).show()
        }
        is NetworkResult.Loading -> { showShimmerEffect() }
      }
    })
  }

  private fun showShimmerEffect() {
    LOG.W(TAG, "showShimmerEffect")
    binding.shimmerLayout.startShimmer()
    binding.shimmerLayout.visibility = View.VISIBLE
    binding.recyclerView.visibility = View.GONE
  }

  private fun hideShimmerEffect() {
    LOG.V5(TAG, "hideShimmerEffect")
    binding.shimmerLayout.stopShimmer()
    binding.shimmerLayout.visibility = View.GONE
    binding.recyclerView.visibility = View.VISIBLE
  }
}