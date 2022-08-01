package cy.ac.ucy.cs.anyplace.lib.android.ui.selector.space.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceType
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceOwnership
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.AnyplaceViewModel
import cy.ac.ucy.cs.anyplace.lib.databinding.BottomSheetSpaceFilterBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Filtering the [Spaces] of the [SpaceListFragment].
 */
class SpaceFilterBottomSheet :  BottomSheetDialogFragment() {
  val TG = "bsheet-space-filter"

  private var _binding: BottomSheetSpaceFilterBinding? = null
  private val binding get() = _binding!!
  private lateinit var VM: AnyplaceViewModel

  private lateinit var C : CONST
  private lateinit var ownershipStr : String
  private lateinit var spaceTypeStr : String
  /** These are chip IDs, used to filter on user ownership of a space:
   * - public: viewed by all
   * - owner: user has created this building
   * - accessible: user is co-owner
   * */
  private var ownershipId = 0
  /** These are chip IDs, used to filter on the type space (building, vessel) */
  private var spaceTypeId= 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val MT = ::onCreate.name
    LOG.D2(TG, MT)
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val MT = ::onCreateView.name
    LOG.D2(TG, MT)

    // Inflate the layout for this fragment
    _binding = BottomSheetSpaceFilterBinding.inflate(inflater, container, false)

    app.setMainView(binding.root)

    val activity = requireActivity()
    VM = ViewModelProvider(activity)[AnyplaceViewModel::class.java]
    C=CONST(activity)
    ownershipStr = C.DEFAULT_QUERY_SPACE_OWNERSHIP
    spaceTypeStr = C.DEFAULT_QUERY_SPACE_TYPE

    observeSpaceFilter()

    binding.chipGroupOwnership.setOnCheckedChangeListener { group, chipId ->
      val chip = group.findViewById<Chip>(chipId)
      ownershipStr = chip.text.toString().uppercase()
      ownershipId = chipId
    }

    binding.chipGroupSpaceType.setOnCheckedChangeListener { group, chipId ->
      val chip = group.findViewById<Chip>(chipId)
      spaceTypeStr = chip.text.toString().uppercase()
      spaceTypeId = chipId
    }

    binding.applyButton.setOnClickListener {
      val queryOwnership = SpaceOwnership.valueOf(ownershipStr)
      val querySpaceType = SpaceType.valueOf(spaceTypeStr)

      VM.dbqSpaces.saveQueryTypeTemp(
        queryOwnership, ownershipId,
        querySpaceType, spaceTypeId)

      // TODO: if query does not return empty results, then store it.. (after it's performed)
      // or: keep the previous query and swap it
      VM.dbqSpaces.saveQueryTypeDataStore()

      val action = SpaceFilterBottomSheetDirections
          .actionSpaceFilterBottomSheetToSpacesListFragment(true)
      findNavController().navigate(action)
    }

    return binding.root
  }

  /**
   * Observe space filter in order to:
   * - initialize the query in memory
   * - update the chips ([Chip])
   */
  private fun observeSpaceFilter() {
    val MT = ::observeSpaceFilter.name

    VM.dbqSpaces.spaceFilter.asLiveData().observe(viewLifecycleOwner) { value ->
      ownershipStr = value.ownership.toString()
      spaceTypeStr = value.spaceType.toString()
      val queryOwnership = SpaceOwnership.valueOf(ownershipStr)
      val querySpaceType = SpaceType.valueOf(spaceTypeStr)

      // initialize the query
      VM.dbqSpaces.saveQueryTypeTemp(
              queryOwnership, ownershipId,
              querySpaceType, spaceTypeId)

      updateChip(value.ownershipId, binding.chipGroupOwnership)
      updateChip(value.spaceTypeId, binding.chipGroupSpaceType)

      LOG.D2(TG, "$MT: Spaces Query: Ownership: $ownershipStr Type: $spaceTypeStr")
    }
  }

  private fun updateChip(chipId: Int, chipGroup: ChipGroup) {
    if(chipId != 0) {
      try {
        val targetView = chipGroup.findViewById<Chip>(chipId)
        targetView.isChecked = true
        chipGroup.requestChildFocus(targetView, targetView)
      } catch(e: Exception) {
        LOG.D2(TAG, "$METHOD: ${e.message}")
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
