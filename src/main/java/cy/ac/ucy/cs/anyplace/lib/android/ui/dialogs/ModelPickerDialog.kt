package cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.RadioButton
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import cy.ac.ucy.cs.anyplace.lib.android.data.store.CvDataStore
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.databinding.DialogPickModelBinding
import java.lang.IllegalStateException

class ModelPickerDialog(private val cvDataStore: CvDataStore):
        DialogFragment() {

  companion object {

    /** Creating the dialog. */
    fun SHOW(fragmentManager: FragmentManager, cvDataStore: CvDataStore) {
      val args = Bundle()

      val dialog = ModelPickerDialog(cvDataStore)
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
