package cy.ac.ucy.cs.anyplace.lib.android.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.cv.enums.YoloConstants
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.legacy.gnk.utils.Detector
import cy.ac.ucy.cs.anyplace.lib.android.data.Repository
import cy.ac.ucy.cs.anyplace.lib.android.data.modelhelpers.FloorHelper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite.Classifier
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite.DetectorActivityBase
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite.YoloV4Classifier
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite.customview.OverlayView
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite.env.BorderedText
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite.env.ImageUtils
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite.tracking.MultiBoxTracker
import cy.ac.ucy.cs.anyplace.lib.android.utils.demo.AssetReader
import cy.ac.ucy.cs.anyplace.lib.android.utils.network.RetrofitHolder
import cy.ac.ucy.cs.anyplace.lib.models.*
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.processor.internal.definecomponent.codegen._dagger_hilt_android_components_ActivityRetainedComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*
import javax.inject.Inject


/**
 * [DetectorViewModel] contains only whatever is related to
 * - object detection (YOLOV4-TFLITE):
 *    - YOLO-related setup
 * - and Cvmap Activity
 *
 *  Initialized by [DetectorActivityBase.onPreviewSizeChosen]
 */
@HiltViewModel
open class DetectorViewModel @Inject constructor(application: Application) :
        AndroidViewModel(application) {

  // private val timeUtils by lazy { timeUtils }
  protected val assetReader by lazy { AssetReader(app.applicationContext) }

  @Deprecated("TODO SETTINGS")
  val model = YoloConstants.DETECTION_MODEL
  internal lateinit var detector: Classifier
}