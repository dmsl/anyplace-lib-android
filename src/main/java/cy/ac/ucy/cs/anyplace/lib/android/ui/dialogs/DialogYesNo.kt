package cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface

object Dialogs {
  fun YesOrNo(
          activity: Activity,
          title: String,
          listener: DialogInterface.OnClickListener) =
          YesOrNo(activity, title, null, listener)

  fun YesOrNo(
          activity: Activity,
          title: String,
          message: String?,
          listener: DialogInterface.OnClickListener) {
    val builder = AlertDialog.Builder(activity)
    builder.setPositiveButton("Yes") { dialog, id ->
      dialog.dismiss()
      listener.onClick(dialog, id)
    }
    builder.setNegativeButton("No", null)
    val alert = builder.create()
    alert.setTitle(title)
    message?.let { alert.setMessage(it) }
    alert.show()
  }
}