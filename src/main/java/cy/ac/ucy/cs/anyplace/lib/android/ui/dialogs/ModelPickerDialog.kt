package cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.RadioButton
import androidx.core.view.forEach
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.store.CvDataStore
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.databinding.DialogPickModelBinding
import java.lang.IllegalStateException

class ModelPickerDialog(private val dsCV: CvDataStore,
private val originalModel: String):
        DialogFragment() {

  companion object {
    /** Creating the dialog. */
    fun SHOW(fragmentManager: FragmentManager,
             cvDataStore: CvDataStore,
             originalModel: String
    ) {
      val args = Bundle()

      val dialog = ModelPickerDialog(cvDataStore, originalModel)
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

      // isCancelable = true
      builder.setView(binding.root)
      val dialog = builder.create()
      dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

      setupRadioButtons()
      setupButtonConfirm(dialog)

      return dialog
    }?: throw IllegalStateException("$TAG activity is null.")
  }

  /**
   */
  private fun setupRadioButtons() {
    val rbGroup = binding.radioGroupOptions
    val modelList = listOf("lashco", "ucyco", "coco")
      LOG.E(TAG, "ORIGINAL MODEL: $originalModel")
      modelList.forEach {
        val rb = RadioButton(context)
        rb.text = it.uppercase()
        rbGroup.addView(rb)
      }

      rbGroup.forEach {
        val rb = it as RadioButton
        if  (rb.text.toString().lowercase() == originalModel.lowercase()) {
          rb.isChecked = true
          return@forEach
        }
      }
  }

  private fun setupButtonConfirm(dialog: AlertDialog) {
    val rbGroup = binding.radioGroupOptions

    binding.btnConfirm.setOnClickListener {
      val rbSelectedId = rbGroup.checkedRadioButtonId
      val rb = binding.radioGroupOptions.findViewById<RadioButton>(rbSelectedId)
      val selectedModel = rb.text.toString().lowercase()
      LOG.W(TAG, "Selected new DNN Model: $selectedModel")

      dsCV.setModelName(selectedModel)
      dialog.dismiss()
    }
  }
}
