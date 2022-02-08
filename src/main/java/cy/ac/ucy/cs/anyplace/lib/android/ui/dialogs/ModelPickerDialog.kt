package cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioButton
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
import cy.ac.ucy.cs.anyplace.lib.databinding.DialogPickModelBinding
import java.lang.IllegalStateException

class ModelPickerDialog(private val dataStoreCv: DataStoreCv):
        DialogFragment() {

  companion object {

    /** Creating the dialog. */
    fun SHOW(fragmentManager: FragmentManager, dataStoreCv: DataStoreCv) {
      val args = Bundle()

      val dialog = ModelPickerDialog(dataStoreCv)
      dialog.arguments = args
      dialog.show(fragmentManager, "")
    }
  }

  var _binding : DialogPickModelBinding?= null
  private val binding get() = _binding!!

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return activity?.let {
      _binding = DialogPickModelBinding.inflate(LayoutInflater.from(context))
      val builder= AlertDialog.Builder(it)
      // isCancelable = false
      builder.setView(binding.root)
      val dialog = builder.create()
      dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

      return dialog
    }?: throw IllegalStateException("$TAG Activity is null.")
  }

  private fun setupRadioButtons() { // TODO
    val rbGroup = binding.radioGroupOptions
    // TODO 1. fetch from assets. and loop automatically
    // TODO 2. on update prompt to restart activity.. (or restart it automatically?)

    val rb = RadioButton(context)
    rb.text = "COCO"
    rbGroup.addView(rb)

    val rb2 = RadioButton(context)
    rb2.text = "ucytCo"
    rbGroup.addView(rb2)
  }
}
