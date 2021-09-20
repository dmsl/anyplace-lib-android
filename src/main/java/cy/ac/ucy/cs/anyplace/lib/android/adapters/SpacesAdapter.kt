package cy.ac.ucy.cs.anyplace.lib.android.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cy.ac.ucy.cs.anyplace.lib.android.utils.SpacesDiffUtil
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

  fun setData(newSpaces: Spaces) {
    // DiffUtil: to update only the changes:
    val spacesDiffUtil = SpacesDiffUtil(spaces, newSpaces.spaces)
    val diffUtilResult = DiffUtil.calculateDiff(spacesDiffUtil)

    spaces = newSpaces.spaces.toList()
    // notifyDataSetChanged() // is overkill (updates the whole RV)
    diffUtilResult.dispatchUpdatesTo(this)
  }

  fun clearData() {
    val size = spaces.size
    spaces = emptyList()
    notifyItemRangeRemoved(0, size)
  }
}