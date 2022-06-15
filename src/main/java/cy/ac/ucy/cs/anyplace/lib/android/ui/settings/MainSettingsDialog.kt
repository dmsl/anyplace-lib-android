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
import androidx.fragment.app.FragmentManager
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.databinding.DialogMainSettingsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.lang.IllegalStateException

class MainSettingsDialog : DialogFragment() {

  companion object {
    val KEY_FROM = "key.from"
    val FROM_LOGIN = "login.settings"
    val FROM_CVLOGGER = "cvlogger.settings"
    val FROM_SELECT_SPACE = "select.space.settings"

    fun SHOW(fragmentManager: FragmentManager, from: String) {
      val args = Bundle()
      args.putString(KEY_FROM, from)
      val dialog = MainSettingsDialog()
      dialog.arguments = args
      // val test = dialog.requireArguments().getString(KEY_FROM)
      dialog.show(fragmentManager, from)
    }
  }

  var _binding : DialogMainSettingsBinding ?= null
  private val binding get() = _binding!!
  var fromCvLogger = true

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return activity?.let {
      _binding = DialogMainSettingsBinding.inflate(LayoutInflater.from(context))

      handleArguments()

      val builder= AlertDialog.Builder(it)
      // isCancelable = false
      builder.setView(binding.root)
      val dialog = builder.create()
      dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
      setup()

      return dialog
    }?: throw IllegalStateException("$TAG Activity is null.")
  }

  private fun handleArguments() {
    val bundle = requireArguments()
    if (bundle.containsKey(KEY_FROM)) {
      val fromActivity = bundle.getString(KEY_FROM)
      when (fromActivity) {
        FROM_CVLOGGER -> {
          LOG.D(TAG, "From: cv logger")
        }
        else -> {
          LOG.W(TAG, "From: $fromActivity")
          fromCvLogger = false
        }
      }
    }
  }

  private fun setupUser() {
    CoroutineScope(Dispatchers.Main).launch {
      val user = app.dsUser.readUser.first()
      if (user.accessToken.isNotBlank()) {
        binding.user = user
      }
    }

    setupUserLogout()
  }

  private fun setupUserLogout() {
    binding.btnLogout.setOnClickListener {
      CoroutineScope(Dispatchers.Main).launch {
        val msg: String
        val user = app.dsUser.readUser.first()
        if (user.accessToken.isNotBlank()) {
          msg = "Logging out ${app.dsUser.readUser.first().name}.."
          app.dsUser.deleteUser()
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

  private fun setupCvLoggerSetings() {
    binding.buttonComputerVision.setOnClickListener {
      startActivity(Intent(requireActivity(), SettingsCvLoggerActivityDEPR::class.java))
    }
  }

  private fun setup() {
    setupCvLoggerSetings()
    fromCvLogger=false // TODO:PM
    if (!fromCvLogger) {
      setupServerSettings()
      setupUser()
    } else {
      // TODO handle this centrally
      binding.btnLogout.isEnabled = false
      binding.buttonHelpAndFeedback.isEnabled = false
      binding.buttonSettingsServer.isEnabled = false
      binding.btnLogout.alpha = .5f
      binding.buttonHelpAndFeedback.alpha= .5f
      binding.buttonSettingsServer.alpha= .5f
    }
  }
}
