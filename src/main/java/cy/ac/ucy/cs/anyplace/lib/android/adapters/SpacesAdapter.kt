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
    LOG.E(TAG, "setData: ${newSpaces.spaces.size}")
    // LEFTHERE
    // LEFTHERE
    // LEFTHERE
    // LEFTHERE
    // crash because of this one:?

    // if (!yourList.isEmpty())
    //   yourList.clear(); //The list for update recycle view

    LOG.E(TAG, "setData: 1")

    try {
      
    val utlDiff = UtilSpacesDiff(spaces, newSpaces.spaces)
    LOG.E(TAG, "setData: 2")
    val diffUtilResult = DiffUtil.calculateDiff(utlDiff)
    LOG.E(TAG, "setData: 3")

    spaces = newSpaces.spaces.toList()
    scope.launch (Dispatchers.Main){
      diffUtilResult.dispatchUpdatesTo(this@SpacesAdapter)
    }
    } catch (e: Exception) {
      LOG.E(TAG, "setData: EXCEPTION: ${e.message}")
    }
  }

  /*


  LEFTHERE:


eadSpaceDrawable: space type: Stena Flavia vessel
2022-07-31 13:44:45.565 7575-7575/cy.ac.ucy.cs.anyplace.smas E/AndroidRuntime: FATAL EXCEPTION: main
    Process: cy.ac.ucy.cs.anyplace.smas, PID: 7575
    java.lang.IndexOutOfBoundsException: Inconsistency detected.
    Invalid view holder adapter positionMyViewHolder
    {a1ffc49 position=4 id=-1, oldPos=0, pLpos:0 scrap [attachedScrap] tmpDetached no parent}
    androidx.recyclerview.widget.RecyclerView{9eacc66 VFED..... ......I. 23,0-1057,1876 #7f0a01e0 app:id/recyclerView},
    adapter:cy.ac.ucy.cs.anyplace.lib.android.adapters.SpacesAdapter@244ba7,
    layout:androidx.recyclerview.widget.LinearLayoutManager@8ab4854,
    context:dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper@f1f5abc
        at androidx.recyclerview.widget.RecyclerView$Recycler.validateViewHolderForOffsetPosition(RecyclerView.java:6156)

        validateViewHolderForOffsetPosition is this one?!

   */
  // TODO: if filter NO elements: run query to clear it..
  fun clearData() {
    LOG.E(TAG, "Clearing data")
    val size = spaces.size
    LOG.E(TAG, "Clearing data: $size")
    spaces = emptyList()
    notifyItemRangeRemoved(0, size)
  }
}