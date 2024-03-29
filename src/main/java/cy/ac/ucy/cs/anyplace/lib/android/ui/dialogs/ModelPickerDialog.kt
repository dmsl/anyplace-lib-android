package cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.RadioButton
import androidx.core.view.forEach
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.DetectionModel
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.CvDataStore
import cy.ac.ucy.cs.anyplace.lib.databinding.DialogPickModelBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.lang.IllegalStateException

/**
 * Dialog for the model picker
 * - allows changing the CV Model
 */
class ModelPickerDialog(private val dsCv: CvDataStore):
        DialogFragment() {
  val TG = "dialog-model-picker"

  companion object {
    /** Creating the dialog. */
    fun SHOW(fragmentManager: FragmentManager,
             dsCv: CvDataStore) {
      val args = Bundle()

      val dialog = ModelPickerDialog(dsCv)
      dialog.arguments = args
      dialog.show(fragmentManager, "")
    }
  }

  var _binding : DialogPickModelBinding?= null
  private val binding get() = _binding!!

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return activity?.let {
      _binding = DialogPickModelBinding.inflate(layoutInflater)
      val builder= AlertDialog.Builder(it)

      // isCancelable = true
      builder.setView(binding.root)
      val dialog = builder.create()
      dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

      setupRadioButtons()
      setupButtonConfirm(dialog)

      return dialog
    }?: throw IllegalStateException("$TG activity is null.")
  }

  /**
   */
  @SuppressLint("SetTextI18n")
  private fun setupRadioButtons() {
    val rbGroup = binding.radioGroupOptions
    lifecycleScope.launch(Dispatchers.Main) {
      val selectedModel = dsCv.read.first().modelName
      LOG.D2(TG, "setupRadioButtons: selected model: $selectedModel")

      DetectionModel.list.forEach {
        val rb = RadioButton(context)
        rb.tag = it.modelName.lowercase()
        rb.text = DetectionModel.getModelAndDescription(rb.tag.toString())
        rbGroup.addView(rb)
      }

      rbGroup.forEach {
        val rb = it as RadioButton
        rb.isChecked = rb.tag.toString() == selectedModel
      }
    }
  }

  private fun setupButtonConfirm(dialog: AlertDialog) {
    val rbGroup = binding.radioGroupOptions

    binding.btnConfirm.setOnClickListener {
      val rbSelectedId = rbGroup.checkedRadioButtonId
      val rb = binding.radioGroupOptions.findViewById<RadioButton>(rbSelectedId)
      val selectedModel = rb.tag.toString().lowercase()
      LOG.W(TG, "Selected new DNN Model: $selectedModel")
      dsCv.setModelName(selectedModel)
      dialog.dismiss()
    }
  }
}
