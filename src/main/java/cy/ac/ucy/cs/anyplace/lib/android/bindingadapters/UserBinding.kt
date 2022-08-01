package cy.ac.ucy.cs.anyplace.lib.android.bindingadapters

import android.annotation.SuppressLint
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.UserAP

// class  {
object UserBindingUtl {
  @BindingAdapter("readUsername", requireAll = true)
  @JvmStatic
  fun readUserName(
    view:TextView,
    userAP: UserAP?) {
    setText(view, userAP?.name, "")
  }
  @BindingAdapter("readEmail", requireAll = true)
  @JvmStatic
  fun readEmail(
    view:TextView,
    userAP: UserAP?) {
    setText(view, userAP?.email, "")
    view.visibility = View.GONE
  }

  @BindingAdapter("readAccountSource", requireAll = true)
  @JvmStatic
  fun readAccountSource(
    view:TextView,
    userAP: UserAP?) {
    setText(view, userAP?.account, "Account")
  }

  @BindingAdapter("readUserType", requireAll = true)
  @JvmStatic
  fun readUserType(
    view:TextView,
    userAP: UserAP?) {
    setText(view, userAP?.type, "Type")
  }

  @BindingAdapter("readUserImage", requireAll = true)
  @JvmStatic
  fun readUserImage(
    view: ImageView,
    userAP: UserAP?) {
    if (userAP==null) {
      view.isVisible = false
    } else {
      view.isVisible = true
      if (userAP.photoUri.isNotEmpty()) {   // if user : photoUri
        // TODO load image with image load lib
        LOG.V5("TODO show user photo")
      }
    }
  }

  @SuppressLint("SetTextI18n")
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
// }