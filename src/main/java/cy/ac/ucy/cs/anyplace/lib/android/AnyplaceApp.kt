package cy.ac.ucy.cs.anyplace.lib.android

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.View
import android.widget.Toast
import androidx.lifecycle.asLiveData
import com.google.android.material.snackbar.Snackbar
import cy.ac.ucy.cs.anyplace.lib.android.cache.anyplace.Cache
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.START_ACT_LOGGER
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.DetectionModel
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.di.DaggerAppComponent
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.wrappers.LevelWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.wrappers.LevelsWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.wrappers.SpaceWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.*
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.store.SmasDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.store.SmasUserDataStore
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilColor
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilSnackBarNotifier
import cy.ac.ucy.cs.anyplace.lib.android.utils.cv.CvUtils
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.di.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.navigator.CvNavigatorActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.CvBackendLoginActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.SmasLoginActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.SmasMainActivity
import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationResult
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Level
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Levels
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Space
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class NavigationAppSelection {
  Navigator,  // BUILDING THE APP AS ANYPLACE NAVIGATOR
  SMAS,       // BUILDING THE APP AS SMAS (for LASHFIRE)
}

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
 *
 *  It is specialized by different apps:
 *  - NavigatorApp: this one has CvLogger and also CvNavigator activities
 *    - no need for a separate logger app
 *    - its on the [navigator] app module (outside of the lib-android)
 *  - SmasApp: has CvLogger and SMAS functionality (like CvNavigator + Chat + Alerts)
 *    - its on the [smas] app module (outside of the lib-android)
 *  - LoggerApp: this one is outdated....
 */
abstract class AnyplaceApp : Application() {
  private val TG = "app"

  protected open val navigatorBaseApp = NavigationAppSelection.Navigator

  fun isNavigator() = navigatorBaseApp == NavigationAppSelection.Navigator
  fun isSMAS() = navigatorBaseApp == NavigationAppSelection.SMAS
  fun getNavigatorActivityName() = if (isNavigator()) CONST.ACT_NAME_NAV else CONST.ACT_NAME_SMAS
  fun getDefaultDetectionModel() =
          if (isNavigator()) DetectionModel.COCO.modelName else DetectionModel.LASHCO.modelName
  fun getDefaultAutoRestoreOwnLocation() = if (isNavigator()) false else true

  fun getNavigatorClass() = if (isNavigator())
      CvNavigatorActivity::class.java as Class<Activity>
  else  SmasMainActivity::class.java as Class<Activity>

  fun getSmasBackendLoginActivity() = if (isNavigator())
    CvBackendLoginActivity::class.java else SmasLoginActivity::class.java

  fun defaultNavigationAppCode(): String {
    val MT = ::defaultNavigationAppCode.name
    LOG.E(TG, "$MT: app: $navigatorBaseApp")

    return if (isNavigator()) CONST.START_ACT_NAV else CONST.START_ACT_SMAS
  }

  var spaceSelectionInProgress: Boolean = false
  @Inject lateinit var RH: RetrofitHolderAP

  /** Anyplace Server preferences */
  @Inject lateinit var dsAnyplace: AnyplaceDataStore
  /** Logged-in Anyplace User */
  @Inject lateinit var dsUserAP: ApUserDataStore
  /** Logged-in SMAS user */
  @Inject lateinit var dsUserSmas: SmasUserDataStore

  /** Miscellaneous settings */
  @Inject lateinit var dsMisc: MiscDS
  // TODO:PMX merge dsCv and dsCvMap
  @Inject lateinit var dsCv: CvDataStore
  @Inject lateinit var dsCvMap: CvMapDataStore

  /** SMAS Server preferences */
  @Inject lateinit var dsSmas: SmasDataStore
  @Inject lateinit var repoAP: RepoAP
  @Inject lateinit var repoSmas: RepoSmas

  // TODO: might be nice to use THIS once everywhere in app
  // or even better, pass it to methods through extension functoins..
  val cache by lazy { Cache(applicationContext) }

  /** [Snackbar] for notification */
  val notify by lazy { UtilSnackBarNotifier(this) }

  /** Root [View] of an activity ([SmasMainActivity], or [CvLoggerActivity]). Used for [SnackBar] */
  lateinit var cvUtils: CvUtils

  /** Last remotely calculated location (SMAS) */
  val locationSmas: MutableStateFlow<LocalizationResult> = MutableStateFlow(LocalizationResult.Unset())

  /** Selected [Space] (model)*/
  var space: Space? = null
  /** All floors of the selected [space] (model) */
  var levels: Levels? = null
  /** Selected floor/deck ([Level]) of [space] (model) */
  var level: MutableStateFlow<Level?> = MutableStateFlow(null)

  /** Selected [Space] ([SpaceWrapper]) */
  lateinit var wSpace: SpaceWrapper
  /** floorsH of selected [wSpace] */
  lateinit var wLevels: LevelsWrapper
  /** Selected floorH of [wLevels], using the UI ([FloorSelector]
   * NOTE: the [locationSmas] of a user can be on a different level (floor or deck)
   * from this selection
   */
  var wLevel: LevelWrapper? = null

  val utlColor by lazy { UtilColor(applicationContext) }

  /** true when a user is issuing an alert */
  var alerting = false
  /** Whether the user location is out of bounds (of the current map projection)
   * NOTE: when there is no user location ([locationSmas]), then the user is NOT
   * considered out of bounds
   */
  var userOutOfBounds: MutableStateFlow<MapBounds> = MutableStateFlow(MapBounds.notLocalizedYet)

  /** Terrible workaround for the terrible SpaceSelector code */
  var backToSpaceSelectorFromOtherActivities= false

  /** The user has localized at least once */
  fun hasLastLocation() : Boolean {
    return locationSmas.value is LocalizationResult.Success
  }

  suspend fun isLogger() = dsCvMap.read.first().startActivity == START_ACT_LOGGER

  /**
   * Set the main view (root view) of the current [Activity],
   * so we can use app [Snackbar] accross different activities
   */
  fun setMainView(root_view: View, placeOnActionbar: Boolean=false) {
    notify.rootView=root_view
    notify.snackbarForChat=placeOnActionbar
  }

  override fun onCreate() {
    super.onCreate()
    LOG.D2()
    DaggerAppComponent.builder().application(this).build()

    LOG.E(TG, "NAVIGATION APP: $navigatorBaseApp")

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
    val serverPref = dsAnyplace.read
    serverPref.asLiveData().observeForever { prefs ->
      RH.set(prefs)
      LOG.D2(TG, "URL: ANYPLACE: ${RH.baseURL}")
    }
  }

  /**
   * The user has selected a different level (floor or deck) that his last position was
   */
  fun userOnOtherFloor() : Boolean {
    if (userHasLocation()) {  // there was a previous user location
      val coord = locationSmas.value.coord!!
      val lastLevel = coord.level
      val selectedLevel = wLevel?.levelNumber() // this is a UI selection
      return lastLevel != selectedLevel
    }
    return false
  }

  fun userHasLocation() = locationSmas.value is LocalizationResult.Success

  private var toast: Toast ?= null

  fun showToastLong(scope: CoroutineScope, msg: String) = showToast(scope, msg, Toast.LENGTH_LONG)
  @Deprecated("use app.notify (or at least a toast in that util class)")
  fun showToast(scope: CoroutineScope, msg: String, duration: Int = Toast.LENGTH_SHORT) {
    scope.launch(Dispatchers.Main) {
      if (toast != null) toast!!.cancel()
      toast = Toast.makeText(this@AnyplaceApp, msg, duration)
      toast?.show()
    }
  }

  @Deprecated("use app.notify (or at least a toast in that util class)")
  fun showToastDEV(scope: CoroutineScope, msg: String, len: Int = Toast.LENGTH_SHORT) {
    val devMode = true
    if (devMode) { showToast(scope, msg, len) }
  }

  /** Whether the user has Developer Mode (devMode) enabled */
  suspend fun hasDevMode() = dsCvMap.read.first().devMode

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

  var initedSpace=false
  /**
   * Initialize the wrappers for the space (and all the levels)
   *
   * It also CLEARS any previous level selection
   * - that will be loaded later on to either:
   *   - the first available level of that space
   *   - or the last selected level of that space
   */
  fun loadSpace(scope: CoroutineScope, newSpace: Space?, newLevels: Levels?): Boolean {
    val MT = ::loadSpace.name
    LOG.E(TG, MT)

    // Initialize space
    try {
      this.space = newSpace
      wSpace = SpaceWrapper(applicationContext, repoAP, this.space!!)
      // Initialize levels:
      // - the available floors or decks of that space
      this.levels = newLevels
      wLevels = LevelsWrapper(this.levels!!, this.wSpace)

      // CLEAR any previous level selection
      this.level.update { null }
      this.wLevel = null

      if (newSpace == null || newLevels == null) {
        notify.WARN(scope, "Cannot load space:\nSpace or Level were empty.")
        return false
      }

      val prettySpace = wSpace.prettyTypeCapitalize
      val prettyFloors= wSpace.prettyFloors

      LOG.I(TG, "$MT: loaded: $prettySpace: ${space!!.name} " +
              "(has ${levels!!.levels.size} $prettyFloors)")

      initedSpace=true
      return true

    } catch(e: Exception) {
      return false
    }
  }

  @Deprecated("Is there a better way for this?")
  suspend fun waitForSpace() {
    val MT = ::waitForSpace.name
    while (!initedSpace) {
      LOG.V(TG, "$MT..")
      delay(200)
    }
  }


}