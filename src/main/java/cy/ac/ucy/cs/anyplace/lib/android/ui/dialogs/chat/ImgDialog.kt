package cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs.chat

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.databinding.DialogImgBinding
import java.lang.IllegalStateException

/**
 * Dialog that is shown when the user clicks on an image message
 * Image is displayed larger and it is zoomable
 *
 * Used in [SmasChatActivity]
 */
class ImgDialog(bmImg: Bitmap) : DialogFragment() {

  private val image = bmImg

  companion object {

    /** Creating the dialog. */
    fun SHOW(fragmentManager: FragmentManager, bmImg: Bitmap) {
      val args = Bundle()
      val dialog = ImgDialog(bmImg)
      dialog.arguments = args
      dialog.show(fragmentManager, "")
    }
  }

  var _binding : DialogImgBinding?= null
  private val binding get() = _binding!!

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return activity?.let {
      _binding = DialogImgBinding.inflate(LayoutInflater.from(context))
      val builder= AlertDialog.Builder(it)
      isCancelable = true
      builder.setView(binding.root)
      val dialog = builder.create()
      dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

      val imgView = binding.tvImg
      imgView.setImageBitmap(image)

      return dialog
    }?: throw IllegalStateException("$TAG Activity is null.")
  }
}