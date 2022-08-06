package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map

import android.content.Context
import androidx.fragment.app.FragmentManager
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.maps.GmapWrapper
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.LevelSelector
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.UiLocalization
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.CvMapActivity
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import kotlinx.coroutines.CoroutineScope

/**
 * Enapsulating UI for common CV/Map operations
 * - floors
 * - floorplans
 * - floorSelector
 *
 * NOTE: this method seems just a wrapper of ui common elements.
 * - by itself it doesn't have much/any functionality..
 * - maybe some more restructuring/cleanup is needed, to bring more ui objects in here..
 */
open class CvUI(
        protected val app: AnyplaceApp,
        protected val activity: CvMapActivity,
        protected val VM: CvViewModel,
        protected val scope: CoroutineScope,
        protected val fragmentManager: FragmentManager,
        val levelSelector: LevelSelector,
        private val btn_id_localization: Int,
        private val btn_id_whereami: Int) {
  val TG = "ui-cv"

  protected val ctx: Context = activity.applicationContext
  /** Google Maps Wrapper */
  val map by lazy { GmapWrapper(app, scope) }

  /** Localization Button Wrapper */
  val localization by lazy { UiLocalization(activity, app, VM, scope,
          map,
          btn_id_localization,
          btn_id_whereami) }

  /**
   * Used to be inside analyzeImage I think
   */
  open fun onInferenceRan() {
    val MT = ::onInferenceRan.name
    LOG.V2(TG, MT)
    // scope.launch(Dispatchers.Main) {
    // TODO: binding bottom sheet stats..
    // bottom.tvTimeInfo.text =  "<TODO>ms" // "${detectionTime}ms"
    // bottom.bindCvStats()
    // bindCvStatsImgDimensions(image) // and do this once. not on each analyze
    // }
  }

  fun setupLevelSelectorClicks(){
    levelSelector.onLevelDown { app.wLevels.tryGoDown(VM) }
    levelSelector.onLevelUp { app.wLevels.tryGoUp(VM) }
  }
}