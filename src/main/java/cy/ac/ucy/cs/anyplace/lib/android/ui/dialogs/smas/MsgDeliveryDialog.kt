package cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs.smas

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.RadioButton
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.SmasApp
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.CHAT
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.store.ChatPrefsDataStore
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasChatViewModel
import cy.ac.ucy.cs.anyplace.lib.databinding.DialogDeliveryModelBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.lang.IllegalStateException

/**
 *  Dialog that is shown when the user clicks on the [DeliveryCard].
 *  Users can select who is going to receive their messages.
 */
class MsgDeliveryDialog(private val dsChat: ChatPrefsDataStore,
                        private val app: SmasApp,
                        vm: SmasChatViewModel) : DialogFragment() {

  private val C by lazy { CHAT(app.applicationContext) }
  private var VMchat = vm

  companion object {

    /** Creating the dialog. */
    fun SHOW(fragmentManager: FragmentManager, dsChat: ChatPrefsDataStore, app: SmasApp, vm : SmasChatViewModel) {
      val args = Bundle()
      val dialog = MsgDeliveryDialog(dsChat, app, vm)
      dialog.arguments = args
      dialog.show(fragmentManager, "")
    }
  }

  var _binding : DialogDeliveryModelBinding?= null
  private val binding get() = _binding!!

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return activity?.let {
      _binding = DialogDeliveryModelBinding.inflate(LayoutInflater.from(context))
      val builder= AlertDialog.Builder(it)
      isCancelable = true
      builder.setView(binding.root)
      val dialog = builder.create()
      dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.WHITE))

      setupRadioButtons()
      setupOkButton()

      return dialog
    }?: throw IllegalStateException("$TAG Activity is null.")
  }

  private fun setupRadioButtons() {
    val rbGroup = binding.radioGroupOptions
    // TODO 2. on update prompt to restart activity.. (or restart it automatically?) //why?
    val method_codes = resources.getStringArray(R.array.delivery_options_values)
    // human readable
    val method_values = resources.getStringArray(R.array.delivery_options_entries)

    method_codes.forEachIndexed { index, code ->
      val rb = RadioButton(context)
      rb.text=method_values[index]
      rb.tag=code
      rbGroup.addView(rb)
    }

    lifecycleScope.launch {
      val chatPrefs = dsChat.read.first()
      LOG.D(TAG, "chatPrefs: ${chatPrefs.mdelivery}")
      val mdelivery = chatPrefs.mdelivery
      val rb = rbGroup.findViewWithTag<RadioButton>(mdelivery)
      if (rb != null)
        rb.isChecked = true
    }
  }

  private fun setupOkButton() {
    val btn = binding.btnOK
    val rbGroup = binding.radioGroupOptions

    btn.setOnClickListener {
      val checkedBtn = rbGroup.checkedRadioButtonId
      val rb = binding.radioGroupOptions.findViewById<RadioButton>(checkedBtn)
      dsChat.putString(C.PREF_CHAT_MDELIVERY, rb.tag.toString())
      VMchat.mdelivery = rb.tag.toString()

      dismiss()
    }
  }
}
