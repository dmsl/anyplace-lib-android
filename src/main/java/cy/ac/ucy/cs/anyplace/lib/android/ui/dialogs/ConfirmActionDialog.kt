package cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.LevelWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.LevelsWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.SpaceWrapper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.databinding.DialogConfirmActionBinding
import java.lang.IllegalStateException

/**
 * Handy class that shows a dialog and accepts a [callback] to perform any necessary actions
 * See it's usages for examples.
 *
 * The [callback] can be passed in as a lambda.
 */
open class ConfirmActionDialog(
        val title: String,
        val callback: () -> Unit,
        val subtitle: String?,
        val isImportant: Boolean,
        val cancellable: Boolean) :
        DialogFragment() {

  companion object {

    /**
     * Creating the dialog.
     * It gets a [SpaceWrapper], [LevelsWrapper], and a [LevelWrapper] and:
     * - if each object is not null:
     *   - it is serialized (into a [String]) and put into the bundle
     * - Once the dialog is created they are deserialized to provide additional clear cache options
     *
     * - TODO pass method over here
     */
    fun SHOW(fragmentManager: FragmentManager,
             title: String,
             /** optional subtitle */
             subtitle: String?=null,
             /** cancel the dialog when clicking outside of it */
             cancellable: Boolean = true,
             /** show red colors if important  */
             isImportant: Boolean = false,
             /** method to run when confirmed */
             callback: () -> Unit) {

      val dialog = ConfirmActionDialog(title, callback, subtitle, isImportant, cancellable)

      val args = Bundle()
      dialog.arguments = args
      dialog.show(fragmentManager, "")
    }
  }

  var _binding : DialogConfirmActionBinding?= null
  private val binding get() = _binding!!

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return activity?.let {
      _binding = DialogConfirmActionBinding.inflate(LayoutInflater.from(context))

      val builder= AlertDialog.Builder(it)
      isCancelable = cancellable
      builder.setView(binding.root)
      val dialog = builder.create()
      dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))


      // android:background="@drawable/button_delete"
      val drawableId = if (isImportant) {
         R.drawable.button_delete
      } else {
         R.drawable.button_confirm
      }

      binding.btnConfirm.background= AppCompatResources.getDrawable(requireContext(), drawableId)

      binding.tvTitle.text = title
      subtitle?.let { binding.tvSubtitle.text = subtitle }

      setupConfirmButton()

      return dialog
    }?: throw IllegalStateException("$TAG Activity is null.")
  }

  private fun setupConfirmButton() {
    val btn = binding.btnConfirm
    btn.setOnClickListener {
      callback()
      dismiss()
    }
  }
}
