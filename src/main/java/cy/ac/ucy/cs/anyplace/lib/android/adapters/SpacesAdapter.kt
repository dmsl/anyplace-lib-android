package cy.ac.ucy.cs.anyplace.lib.android.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.CvMapPrefs
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.wrappers.SpaceWrapper
import cy.ac.ucy.cs.anyplace.lib.android.ui.StartActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.selector.space.SelectSpaceActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Space
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Spaces
import cy.ac.ucy.cs.anyplace.lib.databinding.RowSpaceBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.lang.IndexOutOfBoundsException

/*
 * Recycler View for rendering a dynamic list of [Spaces]  (part of SpaceSelector):
 * - the list of many [Space] objects
 * - the adapter is responsible for one entry (one [Space]) in that list
 *
 * Before opening a [Space] we download several resources using [downloadSpaceResources].
 * Then it opens the Space either in SMAS or Navigator app
 * - depending which app si built
 */
class SpacesAdapter(private val app: AnyplaceApp,
                    private val act: SelectSpaceActivity,
                    private val scope: CoroutineScope):
        RecyclerView.Adapter<SpacesAdapter.MyViewHolder>() {
  private val notify = app.notify

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

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
    return from(parent, app, act, scope)
  }

  override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
    val MT = "onBindViewHolder" // overload resolution ambiguity
    LOG.V2(TG, "$MT: position: $position (sz: ${spaces.size})")
    try {
    if (spaces.isNotEmpty()) {
      val currentSpace = spaces[position]
      holder.bind(currentSpace, holder.act)
    }
    } catch(e: Exception) {
      LOG.E(TG, "$MT: ${e.message}")
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
    val MT = ::setData.name
    LOG.V(TG, "$MT: ${newSpaces.spaces.size}")

    try {
      val utlDiff = UtilSpacesDiff(spaces, newSpaces.spaces)
      val diffUtilResult = DiffUtil.calculateDiff(utlDiff)
      spaces = newSpaces.spaces.toList()
      scope.launch(Dispatchers.Main) {
        diffUtilResult.dispatchUpdatesTo(this@SpacesAdapter)
      }
    } catch (e: IndexOutOfBoundsException) {
      LOG.E(TG, "$MT: error: ${e.message}")
    }
  }

  fun clearData() {
    val MT = ::clearData.name
    val size = spaces.size
    scope.launch(Dispatchers.Main) {
      spaces = emptyList()
      notifyItemRangeRemoved(0, size)
    }
  }

  class MyViewHolder(
          private val binding: RowSpaceBinding,
          private val app: AnyplaceApp,
          val act: SelectSpaceActivity,
          private val scope: CoroutineScope):
          RecyclerView.ViewHolder(binding.root) {
    private val TG = SpacesAdapter.TG+"-"+MyViewHolder::class.java.simpleName
    private val notify = app.notify

    fun bind(space: Space, act: SelectSpaceActivity) {
      binding.space = space
      binding.executePendingBindings()  // update layout on data changes
      setupBtnSelectSpace(space, act)
    }

    private fun setupBtnSelectSpace(space: Space, act: SelectSpaceActivity) {
      val MT = ::setupBtnSelectSpace.name

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

        LOG.E(TG, "$MT: selecting Space: ${space.name} ${space.buid}")

        scope.launch(Dispatchers.IO) {
          app.dsCvMap.setSelectedSpace(space.buid) // store the sapce
          val prefsCv = app.dsCvMap.read.first()

          // if these json resources exists, then most likely the remaining components would exist also
          val showNotification = !app.cache.hasSpaceAndFloor(prefsCv.selectedSpace)
          if (showNotification) {
            val msg = "Downloading resources.."
            notify.INFO(scope, msg)
          }

          act.utlUi.changeMaterialIcon(btn, R.drawable.ic_downloading)
          act.utlUi.changeBackgroundMaterial(btn, R.color.green)
          act.utlUi.flashingLoop(btn)
          btn.tag=activeTag

          // Reset some VM state
          act.VM.dbqSpaces.loaded=false
          act.VM.dbqSpaces.runnedInitialQuery=false
          app.backToSpaceSelectorFromOtherActivities=true

          var tries=0
          while (tries<3) {
            if (downloadSpaceResources(space, prefsCv, showNotification)) break

            LOG.E(TG, "$MT: something went wrong. trying again..")
            tries++
          }

          act.utlUi.clearAnimation(btn)
          act.utlUi.enable(btn)
          act.utlUi.changeMaterialIcon(btn, R.drawable.ic_arrow_forward)
          act.utlUi.changeBackgroundMaterial(btn, R.color.colorPrimary)
          btn.tag=null

          LOG.D2(TG, "$MT: selected space: '${prefsCv.selectedSpace}' ")

          // if (downloadOK) {
          //     app.showToast(scope, "Failed to download some resources")
          // }

          val userAP = app.dsUserAP.read.first()
          StartActivity.openActivity(prefsCv, userAP, act)
          app.spaceSelectionInProgress=false
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
    suspend fun downloadSpaceResources(space: Space, prefsCv: CvMapPrefs, showNotif: Boolean) : Boolean {
      val MT = ::downloadSpaceResources.name
      if(!downloadCvModelFilesAndCvClasses(prefsCv)) return false
      downloadCvFingerprints(prefsCv) // not critical, so not returning a bool

      val shortName = if (space.name.length > 20) space.name.take(15) else space.name
      if (showNotif) {
        notify.INFO(scope, "Downloading ${SpaceWrapper.prettyType(space)} $shortName..")
      }
      if (!downloadSpaceAndLevels(prefsCv)) return false // needed before loading space and levels
      if(!loadSpaceAndLevels(prefsCv))  {
       LOG.E(TG, "$MT: failed to download space and levels")
      }
      downloadFloorplans() // not critical (smas/nav or logger can download those
      if (!downloadConnectionsAndPois()) return false

      return true
    }

    /**
     * Download Cv Model files (classes/obj.names and weights/tflite)
     */
    private suspend fun downloadCvModelFilesAndCvClasses(prefsCv: CvMapPrefs) : Boolean {
      val MT = ::downloadCvModelFilesAndCvClasses.name
      LOG.D(TG, MT)
      if  (act.VMcv.nwCvModelFilesGet.mustDownloadCvModels()) {
        val msg = "Downloading CvModels. Please wait.."
        LOG.W(TG, "$MT: $msg")
        notify.INFO(scope, msg)
        if(!act.VMcv.nwCvModelFilesGet.downloadMissingModels()) { // tflite/weights, and labels
          return false
        }
      }
      if (!act.VMcv.nwCvModelsGet.conditionalBlockingCall()) { // labels, but in the SMAS sqlite format (containing OIDs, etc)
        return false
      }

      return true
    }

    private suspend fun downloadCvFingerprints(prefs: CvMapPrefs) {
      val MT = ::downloadCvFingerprints.name
      if (app.hasInternet() && prefs.autoUpdateCvFingerprints) {
        LOG.E(TG, "$MT: ${prefs.selectedSpace}")
        act.VMcv.nwCvFingerprintsGet.blockingCall(prefs.selectedSpace, false)
      }
    }

    /**
     *  Download JSON objects for the Space and all the Floors
     */
    private suspend fun downloadSpaceAndLevels(prefsCv: CvMapPrefs) : Boolean {
      val MT = ::downloadSpaceAndLevels.name
      val gotSpace = act.VM.nwSpaceGet.blockingCall(prefsCv.selectedSpace)
      val gotFloors = act.VM.nwFloorsGet.blockingCall(prefsCv.selectedSpace)

      if (!gotSpace || !gotFloors) {
        val msg ="Failed to download spaces. Restart app."
        LOG.E(TG, "$MT: $msg")
        notify.WARN(scope, msg)
        app.spaceSelectionInProgress=false
        return false
      }

      return true
    }

    /**
     * 2. Load the downloaded objects
     */
    private fun loadSpaceAndLevels(prefsCv: CvMapPrefs) : Boolean {
      return !app.loadSpace(scope,
              app.cache.readJsonSpace(prefsCv.selectedSpace),
              app.cache.readJsonFloors(prefsCv.selectedSpace))
    }

    private suspend fun downloadFloorplans() {
      act.VMcv.nwLevelPlan.showedMsgDownloading=true
      act.VMcv.nwLevelPlan.downloadAll()
    }

    private suspend fun downloadConnectionsAndPois() : Boolean {
      val MT = ::downloadConnectionsAndPois.name
      if (app.space!=null && !act.VMcv.cache.hasSpaceConnectionsAndPois(app.space!!)) {
        LOG.D2(TG, "$MT: Fetching POIs and Connections..")
        if (!act.VMcv.nwPOIs.callBlocking(app.space!!.buid)) return false
        if (!act.VMcv.nwConnections.callBlocking(app.space!!.buid)) return false
      }
      return true
    }
  } // END OF MyViewHolder
}



class UtilSpacesDiff<T>(private val oldList: List<T>,
                        private  val newList: List<T>): DiffUtil.Callback() {

  override fun getOldListSize(): Int { return oldList.size }
  override fun getNewListSize(): Int { return newList.size }

  override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
    return oldList[oldItemPosition] === newList[newItemPosition]
  }

  override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
    return oldList[oldItemPosition] == newList[newItemPosition]
  }
}
