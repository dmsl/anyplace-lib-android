package cy.ac.ucy.cs.anyplace.lib.android.ui.settings

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
import android.view.View
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.ACT_NAME_LOGGER
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.navigator.CvNavigatorActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.selector.space.SelectSpaceActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.settings.smas.SettingsSmasServerActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.SmasMainActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
import cy.ac.ucy.cs.anyplace.lib.databinding.DialogSettingsSmasBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class MainSettingsDialog(
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
      val dialog = MainSettingsDialog(parentActivity, version)
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
    setupVersion(version)
    setupButtonSwitch(parentActivity)
    setupButtonChangeSpace(parentActivity)
    setupLashfireLink()  // misc
  }

  private fun adaptUI () {
    when (parentActivity) {
      is CvLoggerActivity -> {
        binding.tvTitleAppName.text = "Anyplace Logger"
        binding.ivLogoApp.setImageResource(R.drawable.ic_anyplace)
      }
      is CvNavigatorActivity -> {
        binding.tvTitleAppName.text = "Anyplace Navigator"
        binding.ivLogoApp.setImageResource(R.drawable.ic_anyplace)
      }
      is SmasMainActivity -> {
        binding.tvTitleAppName.text = "Smart Alert System"
        binding.ivLogoApp.setImageResource(R.drawable.ic_lashfire_logo)
      }
    }
  }

  @SuppressLint("SetTextI18n")
  private fun setupButtonSwitch(parentActivity: Activity) {
    val btnSwitch = binding.btnSwitchMode

    var actName = "Activity"
    var  klass : Class<Activity>? = null

    val directive="Switch to"
    if (parentActivity is CvLoggerActivity) {
      actName= app.getNavigatorActivityName()
      klass = app.getNavigatorClass()
    } else if (parentActivity is SmasMainActivity || parentActivity is CvNavigatorActivity) {
      actName=ACT_NAME_LOGGER
      klass = CvLoggerActivity::class.java as Class<Activity>
    }

    btnSwitch.text = "$directive $actName"
    btnSwitch.setOnClickListener {
      app.showToast(lifecycleScope, "Opening $actName")
      startActivity(Intent(requireActivity(), klass))
      parentActivity.finish()
    }
  }

  /**
   * Clears the selected space, and closes the parent activity.
   * The user will be presented with [SelectSpaceActivity], to pick another space.
   */
  @SuppressLint("SetTextI18n")
  private fun setupButtonChangeSpace(parentActivity: Activity) {
    val btn = binding.btnChangeSpace
    if (!DBG.USE_SPACE_SELECTOR) { btn.visibility= View.GONE; return }

    btn.text = "Change space"
    if (app.wSpace.isVessel()) {
      val btnDrawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_vessel, null)
      btn.setCompoundDrawablesWithIntrinsicBounds(btnDrawable, null, null, null)
    }

    val name=app.wSpace.obj.name
    val shortName= if (name.length<15) name else name.take(15)+".."
    btn.text="Change space ($shortName)"

    btn.setOnClickListener {
      lifecycleScope.launch(Dispatchers.IO) {
        app.dsCvMap.clearSelectedSpace()
        app.showToast(lifecycleScope, "Please select another space.")
        // app.mustSelectSpaceForCvMap=true
        val intent = Intent(app.applicationContext, SelectSpaceActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        parentActivity.finishAndRemoveTask()

        dialog?.dismiss()
      }
    }
  }

  private fun setupChatUser() {
    CoroutineScope(Dispatchers.Main).launch {
      val chatUser = requireActivity().app.dsUserSmas.read.first()
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
    val btn = binding.btnSettingsSmas
    if (app.isNavigator()) {
     btn.text=getString(R.string.cv_backend_settings)
      val btnDrawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_anyplace, null)
      btn.setCompoundDrawablesWithIntrinsicBounds(btnDrawable, null, null, null)
    }
    btn.setOnClickListener {
      startActivity(Intent(requireActivity(), SettingsSmasServerActivity::class.java))
    }
  }

  private fun setupLashfireLink() {
    val btn = binding.btnAboutApp
    val url = if (app.isNavigator()) {
      btn.text = "About Anyplace"
      "https://anyplace.cs.ucy.ac.cy"
    } else {
      "https://lashfire.eu/"
    }
    btn.setOnClickListener {
      startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
  }

  private fun setupVersion(version: String) {
    CoroutineScope(Dispatchers.Main).launch {

      val prefsChat = requireActivity().app.dsSmas.read.first()
      LOG.V2(TAG, "Ver: $prefsChat")
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
        val chatUserDS = requireActivity().app.dsUserSmas
        val user = chatUserDS.read.first()
        if (user.sessionkey.isNotBlank()) {
          msg = "Logging out ${app.dsUserAP.read.first().name}.."
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


internal enum class SettingsUi {
  Main,
  Chat,
}
