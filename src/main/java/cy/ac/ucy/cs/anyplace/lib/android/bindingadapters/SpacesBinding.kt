package cy.ac.ucy.cs.anyplace.lib.android.bindingadapters

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.SpaceWrapper.Companion.TP_BUILDING
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.SpaceWrapper.Companion.TP_VESSEL
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Space
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Spaces
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult

/**
 * Provides a binding between the XML of [row_space] and it's values.
 * These methods are essentially called in teh XML.
 *
 * What it matters is the name of the adapter (provided by [@BindingAdapter] annotation)
 * and the method's prototype (parameter list)
 */
object SpacesBinding {

  /**
   * TODO add database here..
   * readRemoteResponse
   */
  @BindingAdapter("readSpacesResponse", requireAll = true)
  @JvmStatic
  fun readSpacesResponse(view:View, resp: NetworkResult<Spaces>?) {
    view.isVisible = resp is NetworkResult.Error

    if (view is TextView) view.text = resp?.message.toString()
  }

  @BindingAdapter("readSpaceType", requireAll = true)
  @JvmStatic
  fun readSpaceType(
          view:TextView,
          space: Space?) {
    setText(view, space?.type, "unknown")
  }

  @BindingAdapter("readSpaceName", requireAll = true)
  @JvmStatic
  fun readSpaceName(view:TextView, space: Space?) {
    setText(view, space?.name, "No name")
  }

  @BindingAdapter("readSpaceDescription", requireAll = true)
  @JvmStatic
  fun readSpaceDescription(view:TextView, space: Space?) {
    setText(view, space?.description, "")
  }

  private fun setText(tv: TextView, value: String?, default: String) {
    if (value != null && value.isNotEmpty()) {
      tv.text="$value"
    } else {
      tv.text=default
    }
  }

  @BindingAdapter("readSpaceDrawable", requireAll = true)
  @JvmStatic
  fun readSpaceDrawable(iv:ImageView, space: Space?) {
    space?.let {
      LOG.E(TAG, "$METHOD: space type: ${space.name} ${space.type}")
      if (space.type.lowercase() == TP_BUILDING) {
        val drawableCompat = ContextCompat.getDrawable(iv.context, R.drawable.ic_building)
        iv.setImageDrawable(drawableCompat)
      } else if (space.type.lowercase() == TP_VESSEL) {
        val drawableCompat = ContextCompat.getDrawable(iv.context, R.drawable.ic_vessel)
        iv.setImageDrawable(drawableCompat)
      }
    }
  }
}