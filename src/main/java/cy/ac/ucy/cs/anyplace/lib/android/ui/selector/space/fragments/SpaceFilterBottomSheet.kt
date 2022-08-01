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
 * This crashes. dont use.
 */
class SpaceFilterBottomSheet :  BottomSheetDialogFragment() {

  private var _binding: BottomSheetSpaceFilterBinding? = null
  private val binding get() = _binding!!
  private lateinit var VM: AnyplaceViewModel

  private lateinit var C : CONST
  private lateinit var queryOwnershipStr : String
  private lateinit var querySpaceTypeStr : String
  /** These are chip IDs, used to filter on user ownership of a space:
   * - public: viewed by all
   * - owner: user has created this building
   * - accessible: user is co-owner
   * */
  private var queryOwnershipId = 0
  /** These are chip IDs, used to filter on the type space (building, vessel) */
  private var querySpaceTypeId= 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    LOG.D2()

  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    LOG.D2()

    // Inflate the layout for this fragment
    _binding = BottomSheetSpaceFilterBinding.inflate(inflater, container, false)

    app.setMainView(binding.root)

    val activity = requireActivity()
    VM = ViewModelProvider(activity)[AnyplaceViewModel::class.java]
    C=CONST(activity)
    queryOwnershipStr = C.DEFAULT_QUERY_SPACE_OWNERSHIP
    querySpaceTypeStr = C.DEFAULT_QUERY_SPACE_TYPE

    lifecycleScope.launch {
      if (VM.dbqSpaces.runnedInitialQuery && VM.dbqSpaces.readSpacesQuery.first().isEmpty()) {
        app.snackbarWarning(VM.viewModelScope, "Previous query had no results!")
      }
    }

    VM.dbqSpaces.storedQuery.asLiveData().observe(viewLifecycleOwner) { value ->
      queryOwnershipStr = value.ownership.toString()
      querySpaceTypeStr = value.spaceType.toString()
      val queryOwnership = SpaceOwnership.valueOf(queryOwnershipStr)
      val querySpaceType = SpaceType.valueOf(querySpaceTypeStr)

      // initialize the query
      VM.dbqSpaces.saveQueryTypeTemp(
              queryOwnership, queryOwnershipId,
              querySpaceType, querySpaceTypeId)

      updateChip(value.ownershipId, binding.chipGroupOwnership)
      updateChip(value.spaceTypeId, binding.chipGroupSpaceType)

      LOG.D2(TAG, "Spaces Query: Ownership: $queryOwnershipStr Type: $querySpaceTypeStr")
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
      val queryOwnership = SpaceOwnership.valueOf(queryOwnershipStr)
      val querySpaceType = SpaceType.valueOf(querySpaceTypeStr)

      // CHECK storing temp, and below permanent (datastore).
      // must do: if query is null, then don't store it.
      VM.dbqSpaces.saveQueryTypeTemp(
        queryOwnership, queryOwnershipId,
        querySpaceType, querySpaceTypeId)

      // TODO: if query does not return empty results, then store it.. (after it's performed)
      // or: keep the previous query and swap it
      VM.dbqSpaces.saveQueryTypeDataStore()

      // LOG.E(TAG, "BUG: SpaceFilterBottomSheetDirections might be using obsolete code")
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
        LOG.D2(TAG, "$METHOD: ${e.message}")
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
