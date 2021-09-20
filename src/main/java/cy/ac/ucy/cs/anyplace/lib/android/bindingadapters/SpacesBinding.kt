package cy.ac.ucy.cs.anyplace.lib.android.bindingadapters

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import cy.ac.ucy.cs.anyplace.lib.models.Spaces
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