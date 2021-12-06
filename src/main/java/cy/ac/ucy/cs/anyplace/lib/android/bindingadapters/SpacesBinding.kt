package cy.ac.ucy.cs.anyplace.lib.android.bindingadapters

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.models.Space
import cy.ac.ucy.cs.anyplace.lib.models.Spaces
import cy.ac.ucy.cs.anyplace.lib.models.User
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult

class SpacesBinding {
  companion object {

    /**
     * TODO add database here..
     */
    @BindingAdapter("readRemoteResponse", requireAll = true)
    @JvmStatic
    fun errorVisibility(
      view:View,
      apiResponse: NetworkResult<Spaces>?) {
      view.isVisible = apiResponse is NetworkResult.Error

      if (view is TextView) view.text = apiResponse?.message.toString()
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
    fun readSpaceName(
      view:TextView,
      space: Space?) {
      setText(view, space?.name, "No name")
    }

    @BindingAdapter("readSpaceDescription", requireAll = true)
    @JvmStatic
    fun readSpaceDescription(
      view:TextView,
      space: Space?) {
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
    fun readSpaceDrawable(
      iv:ImageView,
      space: Space?) {
      space?.let {
        if (space.type.lowercase() == "building") {
          val drawableCompat = ContextCompat.getDrawable(iv.context, R.drawable.ic_building)
          iv.setImageDrawable(drawableCompat)
        } else if (space.type.lowercase() == "vessel-wrong") {
          val drawableCompat = ContextCompat.getDrawable(iv.context, R.drawable.ic_vessel)
          iv.setImageDrawable(drawableCompat)
        }
        // iv.drawable =
      }
      // view.text = space?.type ?: "unknown"
    }


    // TODO:PM
    // // two attributes because we have 2 vars (apiRes, db)
    // @BindingAdapter("readApiResponse", "readDatabase", requireAll = true)
    // @JvmStatic
    // fun errorVisibility(
    //   view: View,
    //   apiResponse: NetworkResult<FoodRecipe>?,
    //   database: List<RecipesEntity>?) {
    //   view.isVisible = apiResponse is NetworkResult.Error && database.isNullOrEmpty()
    //   if (view is TextView) view.setText(apiResponse?.message.toString())
    // }
  }
}