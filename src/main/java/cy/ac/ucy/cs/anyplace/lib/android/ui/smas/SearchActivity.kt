package cy.ac.ucy.cs.anyplace.lib.android.ui.smas

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.CvMapActivity
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.DetectorViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasMainViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * DEMO CODE that was used by Athina (ATH)
 * - there is an open pull request with some of this functionality in demo mode
 * - many things are hardcoded by there is a UI component that allows some search
 *   (on those hardcoded data. by hardcoded: some pois are loaded from assets..)
 */
@AndroidEntryPoint
class SearchActivity : CvMapActivity(), OnMapReadyCallback {

  //// PROVIDE TO BASE CLASS ([CvMapActivity]), which will provide to its base class
  override val layout_activity: Int get() = R.layout.activity_search
  override val id_bottomsheet: Int get() = R.id.bottom_sheet_layout
  override val id_gesture_layout: Int get() = R.id.gesture_layout
  //// PROVIDE TO IMMEDIATE PARENT CLASS [CvMapActivity]:
  override val id_gmap: Int get() = R.id.mapView
  override val actName: String = "act-cv-search"
  override val id_btn_settings: Int get() = R.id.button_settings
  ////// FLOOR SELECTOR
  override val id_group_levelSelector : Int get() = R.id.group_levelSelector
  override val id_tvTitleFloor: Int get() = R.id.textView_titleLevel
  override val id_btnSelectedFloor: Int get() = R.id.button_selectedLevel
  override val id_btnFloorUp: Int get() = R.id.button_levelUp
  override val id_btnFloorDown: Int get() = R.id.button_levelDown
  ////// UI-LOCALIZATION
  override val id_btn_localization: Int get() = R.id.btn_localization
  override val id_btn_whereami: Int get() = R.id.btn_whereami


  @Suppress("UNCHECKED_CAST")
  override val view_model_class: Class<DetectorViewModel> =
          SmasMainViewModel::class.java as Class<DetectorViewModel>

  // VIEW MODELS
  /** extends [CvViewModel] */
  private lateinit var VM: SmasMainViewModel

  override fun postResume() {
    super.postResume()
    VM = _vm as SmasMainViewModel

    setupCollectors()
  }

  override fun onResume() {
    super.onResume()
    LOG.D(TAG, "onResume")
  }

  override fun setupUiAfterGmap() {
  }

  override fun setupUi() {
    super.setupUi()
    LOG.D2()

    // TODO:ATH setup your UI here!
    // For example see this method in [CvMapActivity]
    // (the one that is extended by this class)
    // For checking, invoke this method from the [StartActivity]
    // put any dependencies, resources etc, (like the button you showed me),  into smas
  }

  /**
   * NOTE:ATH If you need anything more look at [SmasMainActivity]
   */
  private fun setupCollectors() {
    LOG.D(TAG_METHOD)
    observeLevels()
  }

  override fun onMapReady(googleMap: GoogleMap) {
    super.onMapReady(googleMap)
  }

  override fun onFirstLevelLoaded() {
  }

}