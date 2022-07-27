package cy.ac.ucy.cs.anyplace.lib.android

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.asLiveData
import com.google.android.material.snackbar.Snackbar
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.di.DaggerAppComponent
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.FloorWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.FloorsWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.SpaceWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.*
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.store.SmasDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.store.SmasUserDataStore
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilColor
import cy.ac.ucy.cs.anyplace.lib.android.utils.cv.CvUtils
import cy.ac.ucy.cs.anyplace.lib.android.utils.net.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationResult
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Floor
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Floors
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Space
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject


enum class MapBounds {
  outOfBounds,        // user is out of bounds (of gmap projection)
  inBounds,           // user is out of bounds (of gmap projection)
  notLocalizedYet     // user has not localized yet
}

/**
 *  Anyplace applications inherit from this class, to initialize:
 *  - the Anyplace API (anyplace-core)
 *  - Preferences
 *  - the FileCache
 */
abstract class AnyplaceApp : Application() {

  @Inject lateinit var RH: RetrofitHolderAP

  // DATASTORES (Preferences/Settings)
  @Inject lateinit var dsServer: ServerDataStore
  @Inject lateinit var dsApUser: ApUserDataStore
  @Inject lateinit var dsMisc: MiscDataStore
  @Inject lateinit var dsCv: CvDataStore
  @Inject lateinit var dsCvMap: CvMapDataStore

  /** SMAS Server preferences */
  @Inject lateinit var dsSmas: SmasDataStore
  /** Logged-in SMAS user */
  @Inject lateinit var dsSmasUser: SmasUserDataStore

  @Inject lateinit var repoAP: RepoAP
  @Inject lateinit var repoSmas: RepoSmas

  /** Root [View] of an activity ([SmasMainActivity], or [CvLoggerActivity]). Used for [SnackBar] */
  lateinit var rootView: View
  var snackbarOnTop = false
  lateinit var cvUtils: CvUtils

  /** Last remotely calculated location (SMAS) */
  val locationSmas: MutableStateFlow<LocalizationResult> = MutableStateFlow(LocalizationResult.Unset())

  /** Selected [Space] (model)*/
  var space: Space? = null
  /** All floors of the selected [space] (model) */
  var floors: Floors? = null
  /** Selected floor/deck ([Floor]) of [space] (model) */
  var floor: MutableStateFlow<Floor?> = MutableStateFlow(null)

  /** Selected [Space] ([SpaceWrapper]) */
  lateinit var wSpace: SpaceWrapper
  /** floorsH of selected [wSpace] */
  lateinit var wFloors: FloorsWrapper
  /** Selected floorH of [wFloors], using the UI ([FloorSelector]
   * NOTE: the [locationSmas] of a user can be on a different level (floor or deck)
   * from this selection
   */
  var wFloor: FloorWrapper? = null

  private val utlColor by lazy { UtilColor(applicationContext) }

  /** true when a user is issuing an alert */
  var alerting = false
  /** Whether the user location is out of bounds (of the current map projection)
   * NOTE: when there is no user location ([locationSmas]), then the user is NOT
   * considered out of bounds
   */
  var userOutOfBounds: MutableStateFlow<MapBounds> = MutableStateFlow(MapBounds.notLocalizedYet)

  // TODO:PM: inject all those. otherwise we might have constructor issues.
  // they must be singleton, but after app ctx is created
  // example:
  // @Inject lateinit var serverDS: ServerDataStore

  /**
   * The user has localized at least once
   */
  fun hasLastLocation() : Boolean {
    return locationSmas.value is LocalizationResult.Success
  }

  /**
   * Set the main view (root view) of the current [Activity], so we can use more easily [Snackbar]
   */
  fun setMainView(root_view: View, snackbarOnTop: Boolean) {
    this.rootView=root_view
    this.snackbarOnTop = snackbarOnTop
  }

  override fun onCreate() {
    super.onCreate()
    LOG.D2(TAG, "onCreate")
    DaggerAppComponent.builder().application(this).build()

    cvUtils = CvUtils(this, repoSmas )

    observeServerPrefs()
  }

  /**
   * Dynamically provide a new Retrofit instance each time the
   * server preferences are updated.
   *
   * This is not a provide as in DI (Dependency Injection).
   * Instead we are manually setting/injecting again a new instance through the RetrofitHolder.
   */
  private fun observeServerPrefs() {
    val serverPref = dsServer.read
    serverPref.asLiveData().observeForever { prefs ->
      RH.set(prefs)
      LOG.D2(TAG, "URL: ANYPLACE: ${RH.baseURL}")
    }
  }

  /**
   * The user has selected a different level (floor or deck) that his last position was
   */
  fun userOnOtherFloor() : Boolean {
    if (userHasLocation()) {  // there was a previous user location
      val coord = locationSmas.value.coord!!
      val lastLevel = coord.level
      val selectedLevel = wFloor?.floorNumber() // this is a UI selection
      return lastLevel != selectedLevel
    }
    return false
  }

  fun userHasLocation() = locationSmas.value is LocalizationResult.Success

  private var toast: Toast ?= null
  fun showToast(scope: CoroutineScope, msg: String, duration: Int = Toast.LENGTH_SHORT) {
    scope.launch(Dispatchers.Main) {
      if (toast != null) toast!!.cancel()
      toast = Toast.makeText(this@AnyplaceApp, msg, duration)
      toast?.show()
    }
  }

  fun showToastDEV(scope: CoroutineScope, msg: String, len: Int = Toast.LENGTH_SHORT) {
    val devMode = true
    if (devMode) { showToast(scope, msg, len) }
  }

  fun showSnackbarLong(scope: CoroutineScope, msg: String) {
    showSnackbar(scope, msg, Snackbar.LENGTH_LONG)
  }

  fun showSnackbarIndefinite(scope: CoroutineScope, msg: String) {
    showSnackbar(scope, msg, Snackbar.LENGTH_INDEFINITE)
  }

  // TODO:PMX: NXT
  fun showSnackbarDEV(scope: CoroutineScope, msg: String,
                      duration: Int = Snackbar.LENGTH_SHORT) {
    if (!DBG.DVO) {
      showToast(scope, msg, Toast.LENGTH_SHORT)
      return
    }

    scope.launch(Dispatchers.IO) {
      if(dsCvMap.read.first().devMode) {
        showSnackbar(scope, msg, duration, true)
      }
    }
  }

  fun showSnackbar(scope: CoroutineScope, msg: String,
                   duration: Int = Snackbar.LENGTH_SHORT, devMode : Boolean = false) {

    if (!DBG.DVO) {
      showToast(scope, msg, Toast.LENGTH_SHORT)
      return
    }

    scope.launch(Dispatchers.Main) {
      val sb = Snackbar.make(rootView, msg, duration)
      sb.setActionTextColor(utlColor.ColorWhite())

      if (duration != Snackbar.LENGTH_SHORT || devMode) {
        sb.setAction("OK") { } // dismissible
      }

      // center text
      val tv = sb.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
      tv.gravity = Gravity.CENTER
      tv.setTypeface(tv.typeface, Typeface.BOLD)
      if (snackbarOnTop) sb.gravityTop()
      tv.textAlignment = View.TEXT_ALIGNMENT_GRAVITY
      tv.maxLines=3

      if (devMode) {
        sb.setDrawableLeft(R.drawable.ic_dev_mode)
        sb.setIconTint(utlColor.ColorWhite())
        sb.setBackground(R.drawable.bg_snackbar_devmode)
      } else {
        sb.setBackground(R.drawable.bg_snackbar_normal)
      }

      sb.show()
    }
  }

  //// MISC
  fun hasInternet(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = connectivityManager.activeNetwork?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)?:return false
    return when {
      capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
      capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
      capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
      else -> false
    }
  }
}