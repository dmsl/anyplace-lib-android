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
import cy.ac.ucy.cs.anyplace.lib.android.extensions.dataStoreUser
import cy.ac.ucy.cs.anyplace.lib.android.extensions.observeOnce
import cy.ac.ucy.cs.anyplace.lib.android.utils.NetworkListener
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.MainViewModel
import cy.ac.ucy.cs.anyplace.lib.databinding.FragmentSpacesListBinding
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SpaceListFragment : Fragment() {
  private val TAG = SpaceListFragment::class.java.simpleName
  private var _binding: FragmentSpacesListBinding? = null
  private val binding get() = _binding!!
  private val mAdapter by lazy { SpacesAdapter() }
  private lateinit var mainViewModel: MainViewModel
  private lateinit var networkListener: NetworkListener

  private val args by navArgs<SpaceListFragmentArgs>() // delegated to NavArgs (SafeArgs plugin)

  // private lateinit var mView: View // CLR:PM

  // Called before onCreateView
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java) // CLR ViewModelFactory
    LOG.D3(TAG, "SpaceListFragment: onCreate")
  }

  override fun onResume() {
    super.onResume()

    LOG.D(TAG, "onResume")
    // TODO when adding to MapFragment: make a SelectSpaceViewModel, and make it a method there.
    // and call w/ args from both SpaceList and SpaceMap
    handleBackToFragment()
  }

  override fun onDestroy() {
    super.onDestroy()
    mainViewModel.loadedApiData = false
  }

  private fun handleBackToFragment() {
    LOG.D(TAG, "handleBackToFragment")
    if(mainViewModel.backFromSettings) {
      LOG.D(TAG, "handleBackToFragment: from settings")
      lifecycleScope.launch {
        val user = requireActivity().dataStoreUser.readUser.first()
        if (user.accessToken.isBlank()) {
          requireActivity().finish()
          // startActivity(Intent(this@Select, LoginActivity::class.java)) CLR?
        } else {
          reloadSpacesFromRemote()
        }
      }
    } else if (args.backFromBottomSheet) {
      LOG.E(TAG, "BACK FROM BOTTOM SHEET!")
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    LOG.D(TAG, "onCreateView")
    LOG.D("HasLoadedSpaces: ${mainViewModel.loadedApiData}")

    // Inflate the layout for this fragment
    _binding = FragmentSpacesListBinding.inflate(inflater, container, false)

    binding.lifecycleOwner = this // for DataBinding
    // this.mainViewModel: initialized in onCreate, and then passed to the binding
    binding.mainViewModel = this.mainViewModel

    setHasOptionsMenu(true) //CHECK
    setupRecyclerView()
    loadSpaces()

    binding.fabFilterSpaces.setOnClickListener {
      if(mainViewModel.networkStatus) {
        findNavController().navigate(R.id.action_spacesListFragment_to_spaceFilterBottomSheet)
      } else {
        mainViewModel.showNetworkStatus()
      }
    }

    return binding.root
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
    LOG.D("loadSpaces")

    lifecycleScope.launchWhenStarted {
      LOG.D("loadSpaces: launchWHenStarted")
      networkListener = NetworkListener()
      // TODO:PM must NOT check for internet before checking cache!
      // CHECK cache first! and implement properly the 'back online' feature.
      networkListener.checkNetworkAvailability(requireActivity())
          .collect { status ->
            LOG.D("loadSpaces -> readDatabase")
            LOG.D(TAG, "Network status: $status")
            mainViewModel.networkStatus = status
            mainViewModel.showNetworkStatus()
            readDatabase()
          }
    }
  }

  private fun readDatabase() {
    lifecycleScope.launch {
      // if we are back from bottom sheet we must reload
      if (!mainViewModel.loadedApiData  || args.backFromBottomSheet) {

        if (args.backFromBottomSheet) {
          // TODO properly implement this query. use just one MutableLiveData (readSpaces  only)
          mainViewModel.querySpaces.observeOnce(viewLifecycleOwner) { query ->
            if (query.isNotEmpty()) {
              LOG.D2(TAG, "readDatabase: loading from cache..")
              loadDatabaseResults(query)
            } else { // HANDLE this..
              LOG.E(TAG, "EMPTY QUERY...")
            }
          }
        } else {

          mainViewModel.readSpaces.observeOnce(viewLifecycleOwner) { database ->
            if (database.isNotEmpty()) {
              LOG.D2(TAG, "readDatabase: loading from cache..")
              loadDatabaseResults(database)
            } else {
              LOG.D2(TAG, "readDatabase: requesting new data..")
              requestRemoteSpacesData()
              observeRemoteSpacesResponse()
            }
          }


        }


      }
    }
  }

  private fun requestRemoteSpacesData() {
    mainViewModel.getSpaces()
  }

  private fun reloadSpacesFromRemote() {
    mainViewModel.unsetBackFromSettings()
    mainViewModel.loadedApiData = false
    requestRemoteSpacesData()
  }

  /**
   * Loads the recycler view with content from the database
   */
  private fun loadDatabaseResults(spaces: List<SpaceEntity>) {
    LOG.D(TAG, "loadFromDatabase")
    hideShimmerEffect()
    spaces.let { mAdapter.setData(entityToSpaces(spaces)) }
    mainViewModel.loadedApiData = true
  }

  private fun observeRemoteSpacesResponse() {
    mainViewModel.spacesResponse.observe(viewLifecycleOwner,  { response ->
      LOG.D(TAG, "observeSpacesResponse")
      when (response) {
        is NetworkResult.Success -> {
          hideShimmerEffect()
          response.data?.let { mAdapter.setData(it) }
          mainViewModel.loadedApiData = true // TODO Set this from db cache as welL!
        }
        is NetworkResult.Error -> {
          if(mainViewModel.loadedApiData) LOG.E("Error response: after showed success.")
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
    LOG.W(TAG, "hideShimmerEffect")
    binding.shimmerLayout.stopShimmer()
    binding.shimmerLayout.visibility = View.GONE
    binding.recyclerView.visibility = View.VISIBLE
  }
}