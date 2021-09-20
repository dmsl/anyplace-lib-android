package cy.ac.ucy.cs.anyplace.lib.android.ui.settings

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.databinding.DialogMainSettingsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.lang.IllegalStateException

class SettingsDialog : DialogFragment() {
  val TAG = SettingsDialog::class.java.simpleName

  companion object {
    val FROM_LOGIN = "login.settings"
    val FROM_SELECT_SPACE = "select.space.settings"
  }

  var _binding : DialogMainSettingsBinding ?= null
  private val binding get() = _binding!!

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return activity?.let {
      _binding = DialogMainSettingsBinding.inflate(LayoutInflater.from(context))
      // savedInstanceState[] // TODO send here rfom where we sttart..

      val builder= AlertDialog.Builder(it)
      // isCancelable = false
      builder.setView(binding.root)
      val dialog = builder.create()
      dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
      setupUserLogout()
      setupServerSettings()

      CoroutineScope(Dispatchers.Main).launch {
        val user = app.dataStoreUser.readUser.first()
        if (user.accessToken.isNotBlank()) {
          binding.user=user
        }
      }

      return dialog
    }?: throw IllegalStateException("$TAG Activity is null.")
  }

  private fun setupUserLogout() {
    binding.buttonLogout.setOnClickListener {
      CoroutineScope(Dispatchers.Main).launch {
        val msg: String
        val user = app.dataStoreUser.readUser.first()
        if (user.accessToken.isNotBlank()) {
          msg = "Logging out ${app.dataStoreUser.readUser.first().name}.."
          app.dataStoreUser.deleteUser()
          dialog?.dismiss()
        } else {
          msg = "No logged in user."
        }
        Toast.makeText(requireActivity(), msg, Toast.LENGTH_SHORT).show();
      }
    }
  }

  private fun setupServerSettings() {
    binding.buttonSettingsServer.setOnClickListener {
      startActivity(Intent(requireActivity(), SettingsServerActivity::class.java))
    }
  }
}
