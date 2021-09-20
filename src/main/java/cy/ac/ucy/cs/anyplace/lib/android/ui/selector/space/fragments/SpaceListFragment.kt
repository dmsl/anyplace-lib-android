package cy.ac.ucy.cs.anyplace.lib.android.ui.selector.space.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.adapters.SpacesAdapter
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.MainViewModel
import cy.ac.ucy.cs.anyplace.lib.databinding.FragmentSpacesListBinding
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SpaceListFragment : Fragment() {
  private val TAG = SpaceListFragment::class.java.simpleName
  private var _binding: FragmentSpacesListBinding? = null
  private val binding get() = _binding!!
  private val mAdapter by lazy { SpacesAdapter() }
  private lateinit var mainViewModel: MainViewModel

  // private lateinit var mView: View // CLR:PM

  // Called before onCreateView
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
    LOG.D(TAG, "SpaceListFragment")
  }

  override fun onDestroy() {
    super.onDestroy()
    mainViewModel.loadedApiData = false
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    // Inflate the layout for this fragment
    _binding = FragmentSpacesListBinding.inflate(inflater, container, false)

    binding.lifecycleOwner = this // for DataBinding
    // this.mainViewModel: initialized in onCreate, and then passed to the binding
    binding.mainViewModel = this.mainViewModel

    setHasOptionsMenu(true) //CHECK
    setupRecyclerView()
    observeSpacesResponse()

    return binding.root
  }

  private fun setupRecyclerView() {
    binding.recyclerView.adapter = mAdapter
    binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    showShimmerEffect()
  }

  private fun observeSpacesResponse() {
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