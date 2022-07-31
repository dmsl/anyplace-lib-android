package cy.ac.ucy.cs.anyplace.lib.android.adapters

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.appSmas
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.ui.StartActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilSpacesDiff
import cy.ac.ucy.cs.anyplace.lib.databinding.SpaceRowLayoutBinding
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Space
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Spaces
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Recycler View for rendering the spaces
 * - the dynamic list
 */
class SpacesAdapter(private val app: AnyplaceApp,
                    private val act: Activity,
                    private val scope: CoroutineScope,
                    ):
        RecyclerView.Adapter<SpacesAdapter.MyViewHolder>() {
  private var spaces = emptyList<Space>()

  class MyViewHolder(
          private val binding: SpaceRowLayoutBinding,
          private val app: AnyplaceApp,
          private val act: Activity,
          private val scope: CoroutineScope,
  ):
    RecyclerView.ViewHolder(binding.root) {

    fun bind(space: Space) {
      binding.space = space
      binding.executePendingBindings()  // update layout on data changes
      setupBtnSelectSpace(space)
    }

    private fun setupBtnSelectSpace(space: Space) {
      binding.btnSelectSpace.setOnClickListener {
        // TODO: store spaceId and spaceName..
        LOG.W(TAG, "Selecting Space: ${space.name} ${space.id}")

        scope.launch(Dispatchers.IO) {
          app.dsCvMap.setSelectedSpace(space.id)
          val prefsCv = app.dsCvMap.read.first()

          scope.launch(Dispatchers.Main) {
            StartActivity.openActivity(prefsCv, act)
          }
          act.finish()
        }
      }
    }

    companion object {
      fun from(parent: ViewGroup, app: AnyplaceApp, act: Activity,
      scope: CoroutineScope): MyViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = SpaceRowLayoutBinding.inflate(layoutInflater, parent, false)
        return MyViewHolder(binding, app, act, scope)
      }
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
    return MyViewHolder.from(parent, app, act, scope)
  }

  override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
    val currentSpace = spaces[position]
    holder.bind(currentSpace)
  }

  override fun getItemCount(): Int {
    return spaces.size
  }

  /**
   * [DiffUtil]: optimization: update only the changess
   *
   *  - notifyDataSetChanged():
   *    - naive approach. overkill.
   *    - updates the whole RV.
   */
  fun setData(newSpaces: Spaces) {
    val utlDiff = UtilSpacesDiff(spaces, newSpaces.spaces)
    val diffUtilResult = DiffUtil.calculateDiff(utlDiff)

    spaces = newSpaces.spaces.toList()
    diffUtilResult.dispatchUpdatesTo(this)

  }

  fun clearData() {
    val size = spaces.size
    spaces = emptyList()
    notifyItemRangeRemoved(0, size)
  }
}