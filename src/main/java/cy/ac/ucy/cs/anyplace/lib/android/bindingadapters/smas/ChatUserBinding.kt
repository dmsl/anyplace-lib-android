package cy.ac.ucy.cs.anyplace.lib.android.bindingadapters.smas

import android.widget.TextView
import androidx.databinding.BindingAdapter
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.setTextOrHide
import cy.ac.ucy.cs.anyplace.lib.smas.models.ChatUser

class ChatUserBinding {
  companion object {
    @BindingAdapter("readUserid", requireAll = true)
    @JvmStatic
    fun readUserId(
      view:TextView,
      user: ChatUser?) {
      // BUG: it's null
      LOG.D(TAG, "UserBinding: $user")
      view.setTextOrHide(user?.uid, "")
    }
  }
}