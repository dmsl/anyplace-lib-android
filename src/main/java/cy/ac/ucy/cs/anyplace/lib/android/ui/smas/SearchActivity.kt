package cy.ac.ucy.cs.anyplace.lib.android.ui.smas

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
 * TODO:ATHX this file is yours. put your all of your stuff here (for poi/user search)
 * This will be merged later with another class (by me).
 * But for now keep everything separate here..
 *
 */
@AndroidEntryPoint
class SearchActivity : CvMapActivity(), OnMapReadyCallback {
  // PROVIDE TO BASE CLASS [CameraActivity]:
  // PROVIDE TO BASE CLASS [CameraActivity]:
  override val layout_activity: Int get() = R.layout.activity_search
  override val id_bottomsheet: Int get() = R.id.bottom_sheet_layout
  override val id_gesture_layout: Int get() = R.id.gesture_layout
  override val id_gmap: Int get() = R.id.mapView


  @Suppress("UNCHECKED_CAST")
  override val view_model_class: Class<DetectorViewModel> =
          SmasMainViewModel::class.java as Class<DetectorViewModel>

  // VIEW MODELS
  /** extends [CvViewModel] */
  private lateinit var VM: SmasMainViewModel

  override fun postCreate() {
    super.postCreate()
    VM = _vm as SmasMainViewModel

    setupCollectors()
  }

  override fun onResume() {
    super.onResume()
    LOG.D(TAG, "onResume")
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
    observeFloors()
  }


  override fun onMapReadyCallback() {
    /*
    TODO:ATH here your Google Map is ready.

    - [wMap]  it's a is a wrapper class ([GmapWrapper]) for the map (more info below)
    The [GoogleMap] object for that wrapper is: [wMap] (it is in the [CvMapActivity])

    - [wMap.obj] it's the actual [GoogleMap] object
    e.g.: you can use wMap.obj.addMarker()

    you can set a bool Flow, and when both: a floor is selected + the map is ready,
    you can put some markers on the map..

    GmapWrapper (cy/ac/ucy/cs/anyplace/lib/android/ui/cv/map/GmapWrapper.kt):
    - it has for example: CvMapUi, that manages:
      - FloorSelector,
      - and Overlays (adding heatmap or images on map)
      - Markers of user locations, etc. (cy.ac.ucy.cs.anyplace.lib.android.maps.Markers)

     Eventually, your functionality will be broken down and put into the [GmapWrapper]
     in a similar fashion..

     TODO:ATH Other notes:
     - you can make a tmpmodels folder for any models you might extra need.
     - there are some models already here: cy/ac/ucy/cs/anyplace/lib/models

     Example:
     - cy/ac/ucy/cs/anyplace/lib/models/POI.kt (that also has POIS.kt)

     - BTW for the POIx (the one that you used for Routes), you can name it: RoutePOI

     */

  }

  override fun onFirstFloorLoaded() {
    TODO("Not yet implemented")
  }

}