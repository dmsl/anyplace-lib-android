package cy.ac.ucy.cs.anyplace.lib.android.ui.selector.space.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.data.db.entities.SpaceType
import cy.ac.ucy.cs.anyplace.lib.android.data.db.entities.UserOwnership
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.MainViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.SpacesViewModel
import cy.ac.ucy.cs.anyplace.lib.databinding.BottomSheetSpaceFilterBinding

class SpaceFilterBottomSheet :  BottomSheetDialogFragment() {
  private val C by lazy { CONST(requireActivity()) }

  private var _binding: BottomSheetSpaceFilterBinding? = null
  private val binding get() = _binding!!
  private lateinit var mainViewModel: MainViewModel
  private lateinit var spacesViewModel: SpacesViewModel

  private var queryOwnershipStr = C.DEFAULT_QUERY_SPACE_OWNERSHIP
  private var queryOwnershipId = 0

  private var querySpaceTypeStr = C.DEFAULT_QUERY_SPACE_TYPE
  private var querySpaceTypeId= 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
    spacesViewModel = ViewModelProvider(requireActivity()).get(SpacesViewModel::class.java)

  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    // Inflate the layout for this fragment
    _binding = BottomSheetSpaceFilterBinding.inflate(inflater, container, false)

    spacesViewModel.storedSpaceQuery.asLiveData().observe(viewLifecycleOwner) { value ->
      queryOwnershipStr = value.ownership.toString()
      querySpaceTypeStr = value.spaceType.toString()
      val queryOwnership = UserOwnership.valueOf(queryOwnershipStr)
      val querySpaceType = SpaceType.valueOf(querySpaceTypeStr)

      // initialize the query
      spacesViewModel.saveQueryTypeTemp(
              queryOwnership, queryOwnershipId,
              querySpaceType, querySpaceTypeId)

      updateChip(value.ownershipId, binding.chipGroupOwnership)
      updateChip(value.spaceTypeId, binding.chipGroupSpaceType)

      LOG.D2("Spaces Query: Ownership: $queryOwnershipStr Type: $querySpaceTypeStr")
    }

    binding.chipGroupOwnership.setOnCheckedChangeListener { group, chipId ->
      val chip = group.findViewById<Chip>(chipId)
      queryOwnershipStr = chip.text.toString().uppercase()
      queryOwnershipId = chipId
    }

    binding.chipGroupSpaceType.setOnCheckedChangeListener { group, chipId ->
      val chip = group.findViewById<Chip>(chipId)
      querySpaceTypeStr = chip.text.toString().uppercase()
      querySpaceTypeId = chipId
    }

    binding.applyButton.setOnClickListener {
      val queryOwnership = UserOwnership.valueOf(queryOwnershipStr)
      val querySpaceType = SpaceType.valueOf(querySpaceTypeStr)

      // CHECK storing temp, and below permanent (datastore).
      // must do: if query is null, then don't store it.
      spacesViewModel.saveQueryTypeTemp(
        queryOwnership, queryOwnershipId,
        querySpaceType, querySpaceTypeId)

      // TODO: if query does not return empty results, then store it.. (after it's performed)
      // or: keep the previous query and swap it
      spacesViewModel.saveQueryTypeDataStore()

      val action = SpaceFilterBottomSheetDirections
          .actionSpaceFilterBottomSheetToSpacesListFragment(true)
      findNavController().navigate(action)
    }

    return binding.root
  }

  private fun updateChip(chipId: Int, chipGroup: ChipGroup) {
    if(chipId != 0) {
      try {
        val targetView = chipGroup.findViewById<Chip>(chipId)
        targetView.isChecked = true
        chipGroup.requestChildFocus(targetView, targetView)
      } catch(e: Exception) {
        LOG.E(TAG, "QueryTypeBottomSheet: ${e.message}")
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
