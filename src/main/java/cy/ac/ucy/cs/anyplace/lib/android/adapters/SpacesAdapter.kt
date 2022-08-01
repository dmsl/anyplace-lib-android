package cy.ac.ucy.cs.anyplace.lib.android.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.CvMapPrefs
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.ui.StartActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.selector.space.SelectSpaceActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilSpacesDiff
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
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
    private const val TG = "adapter-spaces"
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
        val activeTag = "downloading-button"
        val btn = binding.btnSelectSpace

        if (app.spaceSelectionInProgress)  {
          val tagStr = if (btn.tag == null) "" else btn.tag.toString()
          if (tagStr != activeTag) {
            act.utlUi.attentionInvalidOption(btn)
          }
          return@setOnClickListener
        }
        app.spaceSelectionInProgress=true

        LOG.W(TG, "selecting Space: ${space.name} ${space.buid}")

        scope.launch(Dispatchers.IO) {
          app.dsCvMap.setSelectedSpace(space.buid) // store the sapce
          val prefsCv = app.dsCvMap.read.first()

          // if these json resources exists, then most likely the remaining components would exist also
          val showNotif = app.cache.hasSpaceAndFloor(prefsCv.selectedSpace)
          if (!showNotif) {
            val msg = "Downloading resources of ${space.name.take(20)}.."
            app.snackbarLong(scope, msg)
          }

          act.utlUi.flashingLoop(btn)
          act.utlUi.changeMaterialIcon(btn, R.drawable.ic_downloading)
          act.utlUi.changeBackgroundMaterial(btn, R.color.green)
          btn.tag=activeTag

          // Reset some VM state
          act.VM.dbqSpaces.loaded=false
          act.VM.dbqSpaces.runnedInitialQuery=false
          app.backToSpaceSelectorFromOtherActivities=true

          downloadSpaceResources(prefsCv)

          scope.launch(Dispatchers.Main) {
            act.utlUi.clearAnimation(btn)
            act.utlUi.enable(btn)
            act.utlUi.changeMaterialIcon(btn, R.drawable.ic_arrow_forward)
            act.utlUi.changeBackgroundMaterial(btn, R.color.colorPrimary)
            btn.tag=null

            LOG.E(TG, "ADAPTER SELECTED SPACE: '${prefsCv.selectedSpace}' ")
            LOG.E(TG, "ADAPTER SELECTED SPACE: '${prefsCv.selectedSpace}' ")
            LOG.E(TG, "ADAPTER SELECTED SPACE: '${prefsCv.selectedSpace}' ")

            val userAP = app.dsUserAP.read.first()
            StartActivity.openActivity(prefsCv, userAP, act)
            app.spaceSelectionInProgress=false
          }
          act.finish()
        }
      }
    }

    /**
     * Caches all resources of a space, so the next activity will have them already in cache
     * These are:
     * - Json of Space and Levels (needed anyway to open a space)
     * - Level plan (base64 bitmap of floorplans/deckplans)
     * - POIs, and Connections
     */
    suspend fun downloadSpaceResources(prefsCv: CvMapPrefs) {
      // must be in this order
      downloadSpaceAndLevels(prefsCv) // needed first
      loadSpaceAndLevels(prefsCv)
      downloadFloorplans()
      downloadConnectionsAndPois()
    }

    /**
     *  Download JSON objects for the Space and all the Floors
     */
    private suspend fun downloadSpaceAndLevels(prefsCv: CvMapPrefs) {
      val gotSpace = act.VM.nwSpaceGet.blockingCall(prefsCv.selectedSpace)
      val gotFloors = act.VM.nwFloorsGet.blockingCall(prefsCv.selectedSpace)

      if (!gotSpace || !gotFloors) {
        app.snackbarWarningInf(scope, "Failed to download spaces. Restart app.")
        app.spaceSelectionInProgress=false
        act.finishAndRemoveTask()
      }
    }

    /**
     * 2. Load the downloaded objects
     */
    private fun loadSpaceAndLevels(prefsCv: CvMapPrefs) {
      app.initializeSpace(scope,
              app.cache.readJsonSpace(prefsCv.selectedSpace),
              app.cache.readJsonFloors(prefsCv.selectedSpace))
    }

    private suspend fun downloadFloorplans() {
      app.wLevels.showedMsgDownloading=true
      app.wLevels.fetchAllFloorplans(act.VMcv)
    }

    private suspend fun downloadConnectionsAndPois() {
      if (app.space!=null && !act.VMcv.cache.hasSpaceConnectionsAndPois(app.space!!)) {
        LOG.D2(TAG, "Fetching POIs and Connections..")
        act.VMcv.nwPOIs.callBlocking(app.space!!.buid)
        act.VMcv.nwConnections.callBlocking(app.space!!.buid)
      }
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
    return from(parent, app, act, scope)
  }

  override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
    val MT = "onBindViewHolder"
    LOG.V3(TG, "$MT: position: $position (sz: ${spaces.size})")
    if (spaces.isNotEmpty()) {
      val currentSpace = spaces[position]
      holder.bind(currentSpace, holder.act)
    }
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
    LOG.V5(TG, "setData: ${newSpaces.spaces.size}")

    try {
      val utlDiff = UtilSpacesDiff(spaces, newSpaces.spaces)
      val diffUtilResult = DiffUtil.calculateDiff(utlDiff)
      spaces = newSpaces.spaces.toList()
      scope.launch (Dispatchers.Main){
        diffUtilResult.dispatchUpdatesTo(this@SpacesAdapter)
      }
    } catch (e: Exception) {
      LOG.E(TG, "setData: EXCEPTION: ${e.message}")
    }
  }

  fun clearData() {
    val size = spaces.size
    scope.launch(Dispatchers.IO) {
      spaces = emptyList()
      notifyItemRangeRemoved(0, size)
    }
  }
}