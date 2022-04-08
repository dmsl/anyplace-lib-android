package cy.ac.ucy.cs.anyplace.lib.android.bindingadapters

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.models.User

class UserBinding {
  companion object {
    @BindingAdapter("readUsername", requireAll = true)
    @JvmStatic
    fun readUserName(
      view:TextView,
      user: User?) {
      setText(view, user?.name, "")
    }
    @BindingAdapter("readEmail", requireAll = true)
    @JvmStatic
    fun readEmail(
      view:TextView,
      user: User?) {
      setText(view, user?.email, "")
      view.visibility = View.GONE
    }

    @BindingAdapter("readAccountSource", requireAll = true)
    @JvmStatic
    fun readAccountSource(
      view:TextView,
      user: User?) {
      setText(view, user?.account, "Account")
    }

    @BindingAdapter("readUserType", requireAll = true)
    @JvmStatic
    fun readUserType(
      view:TextView,
      user: User?) {
      setText(view, user?.type, "Type")
    }

    @BindingAdapter("readUserImage", requireAll = true)
    @JvmStatic
    fun readUserImage(
      view: ImageView,
      user: User?) {
      if (user==null) {
        view.isVisible = false
      } else {
        view.isVisible = true
        if (user.photoUri.isNotEmpty()) {   // if user : photoUri
          // TODO load image with image load lib
          LOG.V5("TODO show user photo")
        }
      }
    }

    @Deprecated("use EXT: setTextOrHide")
    private fun setText(tv: TextView, value: String?, title:String) {
      if(value!=null) {
        if (title.isNotEmpty()) {
          tv.text="$title: $value"
        } else {
          tv.text="$value"
        }
        tv.isVisible = true
      } else {
        tv.isVisible = false
      }
    }
  }
}