package cy.ac.ucy.cs.anyplace.lib.android.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilSpacesDiff
import cy.ac.ucy.cs.anyplace.lib.databinding.SpaceRowLayoutBinding
import cy.ac.ucy.cs.anyplace.lib.models.Space
import cy.ac.ucy.cs.anyplace.lib.models.Spaces

class SpacesAdapter: RecyclerView.Adapter<SpacesAdapter.MyViewHolder>() {
  private var spaces = emptyList<Space>()

  class MyViewHolder(private val binding: SpaceRowLayoutBinding):
    RecyclerView.ViewHolder(binding.root) {

    fun bind(space: Space) {
      binding.space = space
      binding.executePendingBindings()  // update layout on data changes
    }

    companion object {
      fun from(parent: ViewGroup): MyViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = SpaceRowLayoutBinding.inflate(layoutInflater, parent, false)
        return MyViewHolder(binding)
      }
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
    return MyViewHolder.from(parent)
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