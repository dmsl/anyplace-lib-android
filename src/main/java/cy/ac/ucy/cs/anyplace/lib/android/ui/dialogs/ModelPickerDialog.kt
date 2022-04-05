package cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.RadioButton
import androidx.core.view.forEach
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.cv.enums.DetectionModel
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
             originalModel: String) {
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
  @SuppressLint("SetTextI18n")
  private fun setupRadioButtons() {
    val rbGroup = binding.radioGroupOptions
    LOG.E(TAG, "ORIGINAL MODEL: $originalModel")
    DetectionModel.list.forEach {
      val rb = RadioButton(context)
      val modelName = it.uppercase()
      rb.tag = modelName
      rb.text = "${it.uppercase()}: ${DetectionModel.getDescription(modelName)}"
      rbGroup.addView(rb)
    }


    // LEFTHERE:
    // 1. build and publish AGP upgrade
    // 2. fix change MODEL (broken this..)
    // 3. IMPLEMENT: DELAY artificial...

    rbGroup.forEach {
      val rb = it as RadioButton
      val modelName = originalModel.lowercase()
      if  (rb.text.toString().lowercase() == modelName) {
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
