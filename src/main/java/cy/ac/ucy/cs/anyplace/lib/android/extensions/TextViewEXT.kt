package cy.ac.ucy.cs.anyplace.lib.android.extensions

import android.annotation.SuppressLint
import android.widget.TextView
import androidx.core.view.isVisible

@SuppressLint("SetTextI18n")

fun TextView.setTextOrHide(value: String?, title: String) {
  if(value!=null) {
    text = if (title.isNotEmpty()) "$title: $value" else "$value"
    isVisible = true
  } else {
    isVisible = false
  }
}