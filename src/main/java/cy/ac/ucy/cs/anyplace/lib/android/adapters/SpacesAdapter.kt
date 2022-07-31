package cy.ac.ucy.cs.anyplace.lib.android.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.ui.StartActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.selector.space.SelectSpaceActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilSpacesDiff
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Space
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Spaces
import cy.ac.ucy.cs.anyplace.lib.databinding.RowSpaceBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Recycler View for rendering the [Spaces] the dynamic list
 * - list of many [Space] objects
 */
class SpacesAdapter(private val app: AnyplaceApp,
                    private val act: SelectSpaceActivity,
                    private val scope: CoroutineScope,
                    ):
        RecyclerView.Adapter<SpacesAdapter.MyViewHolder>() {

  companion object {
    fun from(parent: ViewGroup, app: AnyplaceApp, act: SelectSpaceActivity,
             scope: CoroutineScope): MyViewHolder {
      val layoutInflater = LayoutInflater.from(parent.context)
      val binding = RowSpaceBinding.inflate(layoutInflater, parent, false)

      return MyViewHolder(binding, app, act, scope)
    }
  }

  private var spaces = emptyList<Space>()


  class MyViewHolder(
          private val binding: RowSpaceBinding,
          private val app: AnyplaceApp,
          val act: SelectSpaceActivity,
          private val scope: CoroutineScope,
  ):
    RecyclerView.ViewHolder(binding.root) {

    fun bind(space: Space, act: SelectSpaceActivity) {
      binding.space = space
      binding.executePendingBindings()  // update layout on data changes
      setupBtnSelectSpace(space, act)
    }

    private fun setupBtnSelectSpace(space: Space, act: SelectSpaceActivity) {
      binding.btnSelectSpace.setOnClickListener {
        // TODO: store spaceId and spaceName..
        LOG.W(TAG, "Selecting Space: ${space.name} ${space.id}")

        // TODO: PMX DOWNLOAD ALL RESOURCES.. THEN OPEN NEXT ACTIVITY...

        scope.launch(Dispatchers.IO) {
          app.dsCvMap.setSelectedSpace(space.id)
          val prefsCv = app.dsCvMap.read.first()

          val gotSpace = act.VM.nwSpaceGet.blockingCall(prefsCv.selectedSpace)
          val gotFloors = act.VM.nwFloorsGet.blockingCall(prefsCv.selectedSpace)

          if (!gotSpace || !gotFloors) {
           app.showToast(scope, "Failed to download space! (restart app)")
          } else {
            scope.launch(Dispatchers.Main) {
              StartActivity.openActivity(prefsCv, act)
            }
            act.finish()
          }
        }
      }
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
    LOG.W(TAG, "$METHOD: onCreateViewHolder")
    return from(parent, app, act, scope)
  }

  override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
    LOG.W(TAG, "$METHOD: position: $position (sz: ${spaces.size})")
    val currentSpace = spaces[position]
    holder.bind(currentSpace, holder.act)
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
    LOG.W(TAG, "setData: ${newSpaces.spaces.size}")

    try {
      val utlDiff = UtilSpacesDiff(spaces, newSpaces.spaces)
      val diffUtilResult = DiffUtil.calculateDiff(utlDiff)
      spaces = newSpaces.spaces.toList()
      scope.launch (Dispatchers.Main){
        diffUtilResult.dispatchUpdatesTo(this@SpacesAdapter)
      }
    } catch (e: Exception) {
      LOG.E(TAG, "setData: EXCEPTION: ${e.message}")
    }
  }

  fun clearData() {
    LOG.D2()
    val size = spaces.size
    scope.launch(Dispatchers.IO) {
      spaces = emptyList()
      notifyItemRangeRemoved(0, size)
    }
  }
}