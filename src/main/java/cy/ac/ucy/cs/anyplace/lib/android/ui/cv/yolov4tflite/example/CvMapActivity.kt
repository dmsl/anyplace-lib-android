package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite.example

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.PersistableBundle
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite.DetectorActivityBase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * EXAMPLE ACTIVITY:
 * It uses:
 * - uses Yolov4 TFLite
 * - Settings
 * - Google Maps TODO
 *   - floor selector?
 *   - LEFTHERE:
 *     - 1. put google maps
 *     - 2. anything else?
 *     - 3. make this open?!
 *        - to pass google maps?
 *     - or no?
 *       - reuse functionality using Helper UI classes/objects?
 *  - TODO ViewModel for this??
 */
@AndroidEntryPoint
class CvMapActivity : DetectorActivityBase(), OnMapReadyCallback {
  // PROVIDE TO BASE CLASS [CameraActivity]:
  override val layout_activity: Int get() = R.layout.example_cvmap
  override val id_bottomsheet: Int get() = R.id.bottom_sheet_cvmap
  override val id_gesture_layout: Int get() = R.id.gesture_layout

  // TODO override?
  protected lateinit var gmap: GoogleMap

  // BottomSheet specific details (default ones)
  lateinit var frameValueTextView: TextView
  lateinit var cropValueTextView: TextView
  lateinit var inferenceTimeTextView: TextView
  lateinit var bottomSheetArrowImageView: ImageView

  override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
    LOG.E("LEGACY CV MAP ACTIVITY")
    super.onCreate(savedInstanceState, persistentState)
  }

  override fun setupSpecializedUi() {
    setupBottomSheet()
    setupMap()
  }

    /**
     * Attaches a map dynamically
     */
    private fun setupMap() {
      val mapFragment = SupportMapFragment.newInstance()
      supportFragmentManager
              .beginTransaction()
              .add(R.id.mapView, mapFragment)
              .commit()
      mapFragment.getMapAsync(this)
    }

  private fun setupBottomSheet() {
    bottomSheetArrowImageView = findViewById(R.id.bottom_sheet_arrow)
    sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout)
    val vto = gestureLayout.viewTreeObserver

    vto.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
              override fun onGlobalLayout() {
                gestureLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val height = gestureLayout.measuredHeight
                sheetBehavior.peekHeight = height/3
              }
            })

    sheetBehavior.isHideable = false
    setupBottomStageChange(bottomSheetArrowImageView,
            R.drawable.ic_icon_down, R.drawable.ic_icon_up)

    frameValueTextView = findViewById(R.id.frame_info)
    cropValueTextView = findViewById(R.id.crop_info)
    inferenceTimeTextView = findViewById(R.id.time_info)
  }

  override fun onMapReady(googleMap: GoogleMap) {
    LOG.D(TAG, "onMapReady")
    gmap = googleMap

    // VMB.markers = Markers(applicationContext, gmap)
    // val maxZoomLevel = gmap.maxZoomLevel // may be different from device to device
    // map.addMarker(MarkerOptions().position(latLng).title("Ucy Building"))
    // map.moveCamera(CameraUpdateFactory.newLatLng(latLng))

    // TODO Space has to be sent to this activity (SafeArgs?) using a previous "Select Space" activity.
    // (maybe using Bundle is easier/better)
    // loadSpaceAndFloor()

    // TODO:PM this must be moved to earlier activity
    // along with Space/Floors loading (that also needs implementation).
    // lifecycleScope.launch(Dispatchers.IO) { VMB.floorsH.fetchAllFloorplans() }

    // place some restrictions on the map
    // gmap.moveCamera(CameraUpdateFactory.newCameraPosition(
    //         CameraAndViewport.loggerCamera(VMB.spaceH.latLng(), maxZoomLevel)))
    // gmap.setMinZoomPreference(maxZoomLevel-3)

    gmap.uiSettings.apply {
      isZoomControlsEnabled = false
      isMapToolbarEnabled = false
      isTiltGesturesEnabled = false
      isCompassEnabled = false
      isIndoorLevelPickerEnabled = true
    }
    // onMapReadySpecialize() // TODO
  }

  override fun onProcessImageFinished() {
    LOG.D()
    lifecycleScope.launch(Dispatchers.Main) {
      // updateUiBottomSheet()
    }
  }

  @SuppressLint("SetTextI18n")
  private fun updateUiBottomSheet() {
    frameValueTextView.text = "${previewWidth}x${previewHeight}"
    val w = cropCopyBitmap.width
    val h = cropCopyBitmap.height
    cropValueTextView.text = "${w}x${h}"
    inferenceTimeTextView.text =  "${lastProcessingTimeMs}ms"
  }
}