package cy.ac.ucy.cs.anyplace.lib.android.bindingadapters.smas

import android.widget.TextView
import androidx.databinding.BindingAdapter
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.setTextOrHide
import cy.ac.ucy.cs.anyplace.lib.smas.models.SmasUser

/**
 * Methods that bind code to XML entries.
 * Search for their usages.
 * The 'value' in the [@BindingAdapter]  annotation is what it matters.
 * The method name (that happens to have the same name) only has to be unique.
 */
class ChatUserBinding {
  companion object {
    @BindingAdapter("readUserid", requireAll = true)
    @JvmStatic
    fun readUserId(
      view:TextView,
      user: SmasUser?) {
      // BUG: it's null
      LOG.D(TAG, "UserBinding: $user")
      view.setTextOrHide(user?.uid, "")
    }
  }
}