package cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs.smas

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.appSmas
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.ui.settings.SettingsCvActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.settings.smas.SettingsChatActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.SmasMainActivity
import cy.ac.ucy.cs.anyplace.lib.databinding.DialogSettingsSmasBinding
// import cy.ac.ucy.cs.anyplace.smas.ui.SmasMainActivity
// import cy.ac.ucy.cs.anyplace.smas.ui.settings.SettingsChatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal enum class SettingsUi {
  Main,
  Chat,
}

class MainSmasSettingsDialog(
        private val parentActivity: Activity,
        private val version: String): DialogFragment() {

  companion object {
    const val KEY_FROM = "key.from"
    const val FROM_MAIN = "smas.main"
    const val FROM_CHAT = "smas.chat"

    fun SHOW(fragmentManager: FragmentManager, from: String,
             parentActivity: Activity,
             version: String) {
      val args = Bundle()
      args.putString(KEY_FROM, from)
      val dialog = MainSmasSettingsDialog(parentActivity, version)
      dialog.arguments = args
      dialog.show(fragmentManager, from)
    }
  }

  var _binding : DialogSettingsSmasBinding ?= null
  private val binding get() = _binding!!

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return activity?.let {
      _binding = DialogSettingsSmasBinding.inflate(LayoutInflater.from(context))

      handleArguments()

      val builder= AlertDialog.Builder(it)
      isCancelable = true
      builder.setView(binding.root)
      val dialog = builder.create()
      dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
      binding.btnLogout.isEnabled = false
      setup(parentActivity, version)

      return dialog
    }?: throw IllegalStateException("$TAG Activity is null.")
  }

  private var settingsUi = SettingsUi.Main
  // TODO: when back from specific settings: close dialog automatically
  // must pass a message for that (in a bundle probably)
  private fun handleArguments() {
    // val bundle = requireArguments()
    // if (bundle.containsKey(KEY_FROM)) {
    //   val fromActivity = bundle.getString(KEY_FROM)
    //   when (fromActivity) {
    //     FROM_MAIN -> {  settingsUi = SettingsUi.Main  }
    //     FROM_CHAT -> {  settingsUi = SettingsUi.Chat }
    //   }
    // }
  }

  private fun setup(parentActivity: Activity, version: String) {
    adaptUI()
    setupMapSettings()
    setupChatUser()
    setupChatSettings()

    // misc:
    setupLashfireLink()
    // setupAnyplaceLink()

    // var versionStr = BuildConfig.VERSION_NAME
    setupVersion(version)
    setupButtonSwitch(parentActivity)
  }

  private fun adaptUI () {
    when (parentActivity) {
      is CvLoggerActivity -> {
        binding.tvTitleAppName.text = "Anyplace Logger"
        // binding.tvTitleAppName.setTextColor(StatusUpdater.ColorYellowDark(parentActivity))
        binding.ivLogoApp.setImageResource(R.drawable.ic_anyplace)
      }
      is SmasMainActivity -> {
        binding.tvTitleAppName.text = "Smart Alert System"
        // binding.tvTitleAppName.setTextColor(StatusUpdater.ColorBlueDark(parentActivity))
        binding.ivLogoApp.setImageResource(R.drawable.ic_lashfire_logo)
      }
    }
  }

  @SuppressLint("SetTextI18n")
  private fun setupButtonSwitch(parentActivity: Activity) {
    val btnSwitch = binding.btnSwitchMode

    var directive ="Switching to"
    var actName = "Activity"
    var  klass : Class<Activity>? = null

    if (parentActivity is CvLoggerActivity) {
      directive="Back to"
      actName="SMAS"
      klass = SmasMainActivity::class.java as Class<Activity>
      // btnSwitch.icon
      // btnSwitch.setCompoundDrawablesRelativeWithIntrinsicBounds
      // (R.drawable.ic_lashfire_logo,0,0,0)
    } else if (parentActivity is SmasMainActivity) {
      directive="Switch to"
      actName="Logger"
      klass = CvLoggerActivity::class.java as Class<Activity>
      // btnSwitch.setCompoundDrawablesRelativeWithIntrinsicBounds(
      // R.drawable.ic_anyplace,0,0,0)
    }

    btnSwitch.text = "$directive $actName"
    btnSwitch.setOnClickListener {
      app.showToast(lifecycleScope, "Opening $actName")
      startActivity(Intent(requireActivity(), klass))
      parentActivity.finish()
    }

  }


  private fun setupChatUser() {
    CoroutineScope(Dispatchers.Main).launch {
      val chatUser = requireActivity().appSmas.dsChatUser.readUser.first()
      if (chatUser.sessionkey.isNotBlank()) {
        binding.user = chatUser
        binding.tvAccountType.isVisible = true
        binding.tvTitleAccountType.isVisible = true
        setupChatUserLogout()
      }
    }
  }

  private fun setupMapSettings() {
    binding.btnMapSettings.setOnClickListener {
      startActivity(Intent(requireActivity(), SettingsCvActivity::class.java))
    }
  }

  // private fun setupServerSettings() TODO

  private fun setupChatSettings() {
    binding.btnSettingsChat.setOnClickListener {
      startActivity(Intent(requireActivity(), SettingsChatActivity::class.java))
    }
  }

  // private fun setupAnyplaceLink() {
  //   binding.btnAboutAnyplace.setOnClickListener {
  //     startActivity(Intent(Intent.ACTION_VIEW,
  //             Uri.parse(getString(R.string.url_anyplace_about))))
  //   }
  // }

  private fun setupLashfireLink() {
    binding.btnAboutLashfire.setOnClickListener {
      startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://lashfire.eu/")))
    }
  }

  private fun setupVersion(version: String) {
    CoroutineScope(Dispatchers.Main).launch {

      val prefsChat = requireActivity().appSmas.dsChat.read.first()
      LOG.W(TAG, "Ver: $prefsChat")
      var versionStr = version
      if (prefsChat.version != null) versionStr += " (${prefsChat.version})"
      binding.btnVersionSmas.text = getString(R.string.smas_version, versionStr)
    }
  }

  private fun setupChatUserLogout() {
    binding.btnLogout.isEnabled = true
    binding.btnLogout.setOnClickListener {
      CoroutineScope(Dispatchers.Main).launch {
        val msg: String
        val chatUserDS = requireActivity().appSmas.dsChatUser
        val user = chatUserDS.readUser.first()
        if (user.sessionkey.isNotBlank()) {
          msg = "Logging out ${app.dsUser.readUser.first().name}.."
          chatUserDS.deleteUser()
          dialog?.dismiss()
        } else {
          msg = "No logged in user."
        }
        Toast.makeText(requireActivity(), msg, Toast.LENGTH_SHORT).show()
      }
    }
  }

}
