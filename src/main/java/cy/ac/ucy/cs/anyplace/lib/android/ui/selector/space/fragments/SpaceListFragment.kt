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
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.adapters.SpacesAdapter
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.SpaceTypeConverter.Companion.entityToSpaces
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceEntity
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.extensions.dsUserAP
import cy.ac.ucy.cs.anyplace.lib.android.extensions.observeOnce
import cy.ac.ucy.cs.anyplace.lib.android.utils.NetworkListener
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.AnyplaceViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.SpacesViewModel
import cy.ac.ucy.cs.anyplace.lib.databinding.FragmentSpacesListBinding
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SpaceListFragment : Fragment() {
  private val TAG = SpaceListFragment::class.java.simpleName
  /** Adapter reponsible for rendering the spaces */
  private val mAdapter by lazy { SpacesAdapter(requireActivity().app, requireActivity(), lifecycleScope) }

  private var _binding: FragmentSpacesListBinding? = null
  private val binding get() = _binding!!
  private lateinit var VM: AnyplaceViewModel
  private lateinit var VMspaces: SpacesViewModel
  private lateinit var networkListener: NetworkListener

  private val args by navArgs<SpaceListFragmentArgs>() // delegated to NavArgs (SafeArgs plugin)

  // private lateinit var mView: View // CLR:PM

  // Called before onCreateView
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    VM = ViewModelProvider(requireActivity()).get(AnyplaceViewModel::class.java)
    VMspaces = ViewModelProvider(requireActivity()).get(SpacesViewModel::class.java)
    LOG.D3(TAG, "SpaceListFragment: onCreate")
  }

  override fun onResume() {
    super.onResume()
    LOG.D3(TAG, "onResume")
    handleBackToFragment()
  }

  override fun onDestroy() {
    super.onDestroy()
    VMspaces.loadedSpaces = false // TODO:PM replace to loadedSpaces
  }

  // TODO:PM move to SpaceViewModel
  private fun handleBackToFragment() {
    if(VM.backFromSettings) {
      LOG.D2(TAG, "handleBackToFragment: from settings")
      lifecycleScope.launch {
        val user = requireActivity().dsUserAP.readUser.first()
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
    binding.spacesViewModel = this.VMspaces

    setHasOptionsMenu(true)
    observeNetworkChanges()
    setupRecyclerView()
    loadSpaces()
    setupFab()

    observeSearchViewChanges()

    return binding.root
  }

  /**
   * This code is outdated and will crash. start from scratch..
   */
  private fun setupFab() {
    binding.fabFilterSpaces.visibility=View.GONE
    if (true) return

    binding.fabFilterSpaces.setOnClickListener {
      if(VMspaces.loadedSpaces) {
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
        VM.networkStatus = status
        // mainViewModel.showNetworkStatus()
      }
    }
  }

  private fun observeSearchViewChanges() {
    lifecycleScope.launch {
      VMspaces.searchViewData.observe(viewLifecycleOwner) { spaceName ->
        LOG.D(TAG, "searchview: $spaceName")

        VMspaces.readSpacesQuery.removeObservers(viewLifecycleOwner)
        VMspaces.applyQuery(spaceName)
        VMspaces.readSpacesQuery.observeOnce(viewLifecycleOwner) { query ->
          LOG.D(TAG, "Query: ${query.size}")
          loadDatabaseResults(query)
        }
      }
    }
  }

  private fun readDatabase() {
    LOG.E(TAG, "readDatabase:")
    lifecycleScope.launch {

      VMspaces.storedSpaceQuery.firstOrNull { query ->
        LOG.E(TAG, "RunFirstQu")
        VMspaces.saveQueryTypeTemp(query)
        VMspaces.runFirstQuery()

        LOG.E("readDatabase: loaded spaces: ${!VMspaces.loadedSpaces} : bottom: ${args.backFromBottomSheet}" +
            ": " + (!VMspaces.loadedSpaces || args.backFromBottomSheet) + "\n")

        // if we are back from bottom sheet we must reload
        if (!VMspaces.loadedSpaces || args.backFromBottomSheet) {
          if (args.backFromBottomSheet) {
            LOG.E("readDatabase: Back from bottom sheet\n")
            LOG.D(TAG, "readDatabase -> loadSpacesQuery")
            loadSpacesQuery()
          } else {
            VMspaces.readSpacesQuery.observeOnce(viewLifecycleOwner) { spaces ->
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
   * TODO make this callable from both allSpaces and spacesQuery
   */
  private fun loadSpacesQuery() {
    VMspaces.readSpacesQuery.observeOnce(viewLifecycleOwner) { query ->
      LOG.D(TAG, "Query: ${query.size}")
      loadDatabaseResults(query)
    }
  }

  private fun requestRemoteSpacesData() = VMspaces.getSpaces()

  private fun reloadSpacesFromRemote() {
    VM.unsetBackFromSettings()
    VMspaces.loadedSpaces = false
    requestRemoteSpacesData()
  }

  /**
   * Loads the recycler view with content from the database
   */
  private fun loadDatabaseResults(spaces: List<SpaceEntity>) {
    LOG.D(TAG, "loadDatabaseResults")
    if (!VMspaces.loadedSpaces) {
      hideShimmerEffect()
      spaces.let { mAdapter.setData(entityToSpaces(spaces)) }
      if (spaces.isNotEmpty()) {
        LOG.V3(TAG, "Found ${spaces.size} spaces.")
        hideErrorMsg()
      } else {
        showErrorMsg("No results.")
        VMspaces.resetQuery()
      }
    } else {
      LOG.W(TAG, "loadDatabaseResults: skipped loading..")
    }
    VMspaces.loadedSpaces = true
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
    VMspaces.spacesResponse.observe(viewLifecycleOwner) { response ->
      LOG.D(TAG, "observeSpacesResponse")
      when (response) {
        is NetworkResult.Success -> {
          hideShimmerEffect()
          response.data?.let { mAdapter.setData(it) }
          VMspaces.loadedSpaces = true // TODO Set this from db cache as welL!
        }
        is NetworkResult.Error -> {
          if (VMspaces.loadedSpaces) LOG.E("Error response: after showed success.")
          mAdapter.clearData()
          hideShimmerEffect()
          // if(!showedApiData) {
          //   showShimmerEffect()
          // }
          // loadDataFromCache() TODO :DB
          Toast.makeText(requireContext(), response.message.toString(), Toast.LENGTH_SHORT).show()
        }
        is NetworkResult.Loading -> {
          showShimmerEffect()
        }
      }
    }
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