package cy.ac.ucy.cs.anyplace.lib.android.ui.selector.space.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceType
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceOwnership
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.FilterSpaces
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.AnyplaceViewModel
import cy.ac.ucy.cs.anyplace.lib.databinding.BottomSheetSpaceFilterBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Filtering the [Spaces] of the [SpaceListFragment].
 * This crashes. dont use.
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
    LOG.W(TG, MT)

    // Inflate the layout for this fragment
    _binding = BottomSheetSpaceFilterBinding.inflate(inflater, container, false)

    app.setMainView(binding.root)

    val activity = requireActivity()
    VM = ViewModelProvider(activity)[AnyplaceViewModel::class.java]
    C=CONST(activity)
    ownershipStr = C.DEFAULT_QUERY_SPACE_OWNERSHIP
    spaceTypeStr = C.DEFAULT_QUERY_SPACE_TYPE

    setInitialChipStates()
    setupChipClicks()
    setupButtonApply()

    return binding.root
  }

  /**
   * Update filters based on the stored query
   */
  private fun setInitialChipStates() {
    val MT = ::setInitialChipStates.name

    lifecycleScope.launch(Dispatchers.IO) {
      // val value = app.dbqSpaces.storedFilter.first()

      app.dsSpaceSelector.readSpaceFilter.collectLatest { value ->
        LOG.E(TG, "$MT: new value: $value")
        LOG.E(TG, "$MT: onwerIt: ${value.ownership}: ${value.ownershipId}")
        LOG.E(TG, "$MT: type: ${value.spaceType}: ${value.spaceTypeId}")

        // UPDATING LOCAL VARIABLES..
        ownershipStr = value.ownership.toString()
        spaceTypeStr = value.spaceType.toString()


        // UPDATE FILTER. WHY?!1
        // val ownership = SpaceOwnership.valueOf(ownershipStr)
        // val spaceType = SpaceType.valueOf(spaceTypeStr)
        // val filter = FilterSpaces(ownership, ownershipId, spaceType, spaceTypeId)
        // LOG.E(TG, "$MT: filter: $filter")
        // LOG.E(TG, "$MT: onwerIt: ${ownership}: ${ownershipStr}")
        // LOG.E(TG, "$MT: type: ${spaceType}: ${spaceTypeStr}")
        // app.dbqSpaces.updateTempFilter(filter)

        updateChip(value.ownershipId, binding.chipGroupOwnership)
        updateChip(value.spaceTypeId, binding.chipGroupSpaceType)
      }

      // ownershipStr = value.ownership.toString()
      // spaceTypeStr = value.spaceType.toString()
      // // val ownership = SpaceOwnership.valueOf(ownershipStr)
      // // val spaceType = SpaceType.valueOf(spaceTypeStr)
      //
      // // val filter = FilterSpaces(ownership, ownershipId, spaceType, spaceTypeId)
      // // app.dbqSpaces.updateTempFilter(value)
      // updateChip(value.ownershipId, binding.chipGroupOwnership)
      // updateChip(value.spaceTypeId, binding.chipGroupSpaceType)
    }

    // app.dbqSpaces.storedFilter.asLiveData().observe(viewLifecycleOwner) { value ->
    //   ownershipStr = value.ownership.toString()
    //   spaceTypeStr = value.spaceType.toString()
    //   val ownership = SpaceOwnership.valueOf(ownershipStr)
    //   val spaceType = SpaceType.valueOf(spaceTypeStr)
    //   val filter = FilterSpaces(ownership, ownershipId, spaceType, spaceTypeId)
    //   app.dbqSpaces.updateTempFilter(filter)
    //   updateChip(value.ownershipId, binding.chipGroupOwnership)
    //   updateChip(value.spaceTypeId, binding.chipGroupSpaceType)
    // }
  }

  private fun setupButtonApply() {
    val MT = ::setupButtonApply.name
    binding.applyButton.setOnClickListener {
      val ownership = SpaceOwnership.valueOf(ownershipStr)
      val spaceType = SpaceType.valueOf(spaceTypeStr)

      val filter = FilterSpaces(ownership, ownershipId, spaceType, spaceTypeId)

      if (app.dbqSpaces.filterChanged(filter) || app.dbqSpaces.wasReset) {
        if (app.dbqSpaces.filterChanged(filter)) {
          LOG.E(TG, "FITLER HAS CHANGED: $filter")
          app.dbqSpaces.printTempFilter()
        } else {
          LOG.E(TG, "FILTER has not changed")
        }

        if (app.dbqSpaces.wasReset) {
          LOG.E(TG, "FITLER WAS RESET")
        } else {
          LOG.E(TG, "FILTER was NOT reset")
        }

        app.dbqSpaces.wasReset=false
        app.dbqSpaces.updateTempFilter(filter)

        // send action, so a query will run
        val action = SpaceFilterBottomSheetDirections
                .actionSpaceFilterBottomSheetToSpacesListFragment(true)
        findNavController().navigate(action)
      } else {
        LOG.E(TG,"$MT: no changes: dismissing bsheet")
        this@SpaceFilterBottomSheet.dismiss()
      }
    }
  }

  private fun setupChipClicks() {
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
  }

  private fun updateChip(chipId: Int, chipGroup: ChipGroup) {
    if(chipId != 0) {
      try {
        lifecycleScope.launch(Dispatchers.Main) {
          val targetView = chipGroup.findViewById<Chip>(chipId)
          targetView.isChecked = true
          chipGroup.requestChildFocus(targetView, targetView)
        }
      } catch(e: Exception) {
        LOG.D2(TG, "$METHOD: ${e.message}")
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
