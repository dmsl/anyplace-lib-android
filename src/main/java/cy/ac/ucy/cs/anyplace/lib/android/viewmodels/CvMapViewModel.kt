package cy.ac.ucy.cs.anyplace.lib.android.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Localization is generally an one-time call. It gets a list of objects from the camera,
 * and calculates the user location.
 *
 * However, YOLO (and it's camera-related components) operate asynchronously in the background,
 * and store detection lists in 'scanning windows'.
 * Therefore we need the below states.
 */
enum class Localization {
  running,
  stopped,
  stoppedNoDetections,
}
/**  TODO put in CvMapViewModel
 *    - floorplan fetching
 *    - gmap markers
 */
@HiltViewModel
open class CvMapViewModel @Inject constructor(application: Application) :
        DetectorViewModel(application) {


  // TODO MOVE
  /** Controlling navigation mode */
  // val localization = MutableStateFlow(Localization.stopped)
  // val localizationFlow = localization.asStateFlow()
  // // CV WINDOW: on Localization/Logging the detections are grouped per scanning window,
  // // e.g., each window might be 5seconds.
  // /** related to cv scan window */
  // var currentTime : Long = 0
  // var windowStart : Long = 0
  // /** Detections for the localization scan-window */
  // val detectionsLocalization: MutableStateFlow<List<Detector.Detection>> = MutableStateFlow(emptyList())
  // /** Last Anyplace location */
  // val location: MutableStateFlow<LocalizationResult> = MutableStateFlow(LocalizationResult.Unset())
  // /** Selected [Space] */
  // var space: Space? = null
  // /** All floors of the selected [space]*/
  // var floors: Floors? = null
  // /** Selected [Space] ([SpaceHelper]) */
  // lateinit var spaceH: SpaceHelper
  // /** floorsH of selected [spaceH] */
  // lateinit var floorsH: FloorsHelper
  // TODO rebuild FLoorH and floor
  // /** Selected floorH of [floorsH] */
  // var floorH: FloorHelper? = null
  // /** Selected floor/deck ([Floor]) of [space] */
  // var floor: MutableStateFlow<Floor?> = MutableStateFlow(null)
  /** LastVals: user last selections regarding a space.
   * Currently not much use (for a field var), but if we have multiple
   * lastVals for space then it would make sense. */
  // var lastValSpaces: LastValSpaces = LastValSpaces()
  // /** Initialized onMapReady */
  // var markers : Markers? = null
  // // val floorplanResp: MutableLiveData<NetworkResult<Bitmap>> = MutableLiveData()
  // val floorplanFlow : MutableStateFlow<NetworkResult<Bitmap>> = MutableStateFlow(Error(null))
  // // MutableStateFlow(false)
  // // MutableStateFlow(false)
  // /** Holds the functionality of a [CvMap] and can generate the [CvMapFast] */
  // var cvMapH: CvMapHelper? = nul

}