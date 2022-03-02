package cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.cache.Cache
import cy.ac.ucy.cs.anyplace.lib.android.data.Repository
import cy.ac.ucy.cs.anyplace.lib.android.data.datastore.DataStoreCv
import cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers.FloorHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers.FloorsHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers.SpaceHelper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.ui.settings.IntentExtras
import cy.ac.ucy.cs.anyplace.lib.databinding.DialogClearCachesBinding
import java.lang.IllegalStateException

class ClearCachesDialog(
        private val repo: Repository,
        private val dataStoreCv: DataStoreCv):
        DialogFragment() {

  companion object {
    private const val KEY_SPACE = "space"
    private const val KEY_FLOORS = "floors"
    private const val KEY_FLOOR = "floor"

    /**
     * Creating the dialog.
     * It gets a [SpaceHelper], [FloorsHelper], and a [FloorHelper] and:
     * - if each object is not null:
     *   - it is serialized (into a [String]) and put into the bundle
     * - Once the dialog is created they are deserialized to provide additional clear cache options
     */
    fun SHOW(fragmentManager: FragmentManager,
             repo: Repository,
             dataStoreCv: DataStoreCv,
             SH: SpaceHelper?,
             FSH: FloorsHelper?,
             FH: FloorHelper?) {
      val args = Bundle()

      SH?.let { sh ->
        args.putString(KEY_SPACE, sh.toString())
        FSH?.let { args.putString(KEY_FLOORS, it.toString()) }
        FH?.let { args.putString(KEY_FLOOR, it.toString()) }
      }

      val dialog = ClearCachesDialog(repo, dataStoreCv)
      dialog.arguments = args
      // val test = dialog.requireArguments().getString(KEY_FROM)
      dialog.show(fragmentManager, "")
    }
  }

  var spaceH: SpaceHelper?= null
  var floorsH: FloorsHelper?= null
  var floorH: FloorHelper?= null

  var _binding : DialogClearCachesBinding?= null
  private val binding get() = _binding!!

  var fromCvLogger = true

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return activity?.let {
      _binding = DialogClearCachesBinding.inflate(LayoutInflater.from(context))

      val builder= AlertDialog.Builder(it)
      // isCancelable = false
      builder.setView(binding.root)
      val dialog = builder.create()
      dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

      setupRadioButtons()
      setupConfirmButton()

      return dialog
    }?: throw IllegalStateException("$TAG Activity is null.")
  }

  /**
   * Shows additional radio buttons when:
   * - space is not null
   * - floor is not null
   */
  private fun setupRadioButtons() {
    val bundle = requireArguments()
    if (bundle.containsKey(KEY_SPACE)) {
      spaceH = IntentExtras.getSpace(requireActivity(), repo, bundle, KEY_SPACE)
      if (spaceH != null) {
        val SH = spaceH!!
        LOG.D(TAG_METHOD, "Space is ${SH.space.name}")
        floorsH=IntentExtras.getFloors(spaceH, bundle, KEY_FLOORS)
        binding.radioButtonSpace.text=getString(R.string.for_var_var, SH.prettyType, SH.space.name)
        binding.radioButtonSpace.visibility=View.VISIBLE
        binding.radioButtonFloor.isChecked = true

        floorH = IntentExtras.getFloor(spaceH, bundle, KEY_FLOOR)
        if (floorH!=null) {
          LOG.D(TAG_METHOD, "Floor ${floorH?.prettyFloorNumber()}")
          binding.radioButtonFloor.text = getString(R.string.for_var_var,
                  floorH?.prettyFloorNumber(), " of ${SH.space.name}")
          binding.radioButtonFloor.visibility=View.VISIBLE
          binding.radioButtonFloor.isChecked = true
        }
      }
    }
  }

  // TODO: on CvLoggerActivity (or fragment) resume: redraw the heatmap
  // (or set a boolean if we must redraw)
  private fun setupConfirmButton() {
   val btn = binding.buttonClearCaches
    btn.setOnClickListener {
      when {
        // clear all
        binding.radioButtonAll.isChecked -> { Cache(requireActivity()).deleteCvMapsLocal() }
        // clear all floors
        binding.radioButtonSpace.isChecked -> { floorsH?.clearCacheCvMaps() }
        // clear specified floor
        binding.radioButtonFloor.isChecked -> { floorH?.clearCacheCvMaps() }
      }

      dataStoreCv.setReloadCvMaps(true)
      dismiss()
    }
  }
}
