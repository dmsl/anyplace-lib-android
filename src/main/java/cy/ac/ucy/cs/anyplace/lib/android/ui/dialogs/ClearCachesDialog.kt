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
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.cache.anyplace.Cache
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.CvDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.FloorWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.FloorsWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.SpaceWrapper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.ui.settings.base.IntentExtras
import cy.ac.ucy.cs.anyplace.lib.databinding.DialogClearCachesBinding
import java.lang.IllegalStateException

@Deprecated("Not needed - Used for Cv-Map Json fingerprints")
class ClearCachesDialog(
  private val repo: RepoAP,
  private val cvDataStore: CvDataStore
):
        DialogFragment() {

  companion object {
    private const val KEY_SPACE = "space"
    private const val KEY_FLOORS = "floors"
    private const val KEY_FLOOR = "floor"

    /**
     * Creating the dialog.
     * It gets a [SpaceWrapper], [FloorsWrapper], and a [FloorWrapper] and:
     * - if each object is not null:
     *   - it is serialized (into a [String]) and put into the bundle
     * - Once the dialog is created they are deserialized to provide additional clear cache options
     */
    fun SHOW(fragmentManager: FragmentManager,
             repo: RepoAP,
             cvDataStore: CvDataStore,
             SH: SpaceWrapper?,
             FSH: FloorsWrapper?,
             FH: FloorWrapper?) {
      val args = Bundle()

      SH?.let { sh ->
        args.putString(KEY_SPACE, sh.toString())
        FSH?.let { args.putString(KEY_FLOORS, it.toString()) }
        FH?.let { args.putString(KEY_FLOOR, it.toString()) }
      }

      val dialog = ClearCachesDialog(repo, cvDataStore)
      dialog.arguments = args
      // val test = dialog.requireArguments().getString(KEY_FROM)
      dialog.show(fragmentManager, "")
    }
  }

  var spaceH: SpaceWrapper?= null
  var floorsH: FloorsWrapper?= null
  var floorH: FloorWrapper?= null

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
        LOG.D(TAG_METHOD, "Space is ${SH.obj.name}")
        floorsH= IntentExtras.getFloors(spaceH, bundle, KEY_FLOORS)
        binding.radioButtonSpace.text=getString(R.string.for_var_var, SH.prettyType, SH.obj.name)
        binding.radioButtonSpace.visibility=View.VISIBLE
        binding.radioButtonFloor.isChecked = true

        floorH = IntentExtras.getFloor(spaceH, bundle, KEY_FLOOR)
        if (floorH!=null) {
          LOG.D(TAG_METHOD, "Floor ${floorH?.prettyFloorNumber()}")
          binding.radioButtonFloor.text = getString(R.string.for_var_var,
                  floorH?.prettyFloorNumber(), " of ${SH.obj.name}")
          binding.radioButtonFloor.visibility=View.VISIBLE
          binding.radioButtonFloor.isChecked = true
        }
      }
    }
  }

  // TODO: on CvLoggerActivity (or fragment) resume: redraw the heatmap
  // (or set a boolean if we must redraw)
  private fun setupConfirmButton() {
   val btn = binding.btnConfirm
    btn.setOnClickListener {
      when {
        // clear all
        binding.radioButtonAll.isChecked -> { Cache(requireActivity()).deleteCvMapsLocal() }
        // clear all floors
        binding.radioButtonSpace.isChecked -> { floorsH?.clearCacheCvMaps() }
        // clear specified floor
        binding.radioButtonFloor.isChecked -> { floorH?.clearCacheCvMaps() }
      }

      cvDataStore.setReloadCvMaps(true)
      dismiss()
    }
  }
}
