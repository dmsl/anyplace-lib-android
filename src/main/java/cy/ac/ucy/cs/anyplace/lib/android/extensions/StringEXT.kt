package cy.ac.ucy.cs.anyplace.lib.android.extensions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

fun String.copyToClipboard(context: Context) {
  val clipBoard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  val clipData = ClipData.newPlainText("label",this)
  clipBoard.setPrimaryClip(clipData)
}