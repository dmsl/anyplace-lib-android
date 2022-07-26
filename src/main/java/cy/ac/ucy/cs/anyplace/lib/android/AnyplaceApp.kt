package cy.ac.ucy.cs.anyplace.lib.android

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.lifecycle.asLiveData
import cy.ac.ucy.cs.anyplace.lib.android.utils.cv.CvUtils
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.di.DaggerAppComponent
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.FloorWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.FloorsWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.SpaceWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.*
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.store.SmasDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.store.SmasUserDataStore
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.net.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationResult
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Floor
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Floors
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Space
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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
  /** Selected floorH of [wFloors] */
  var wFloor: FloorWrapper? = null


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

  fun showToastDEV(scope: CoroutineScope, msg: String, len: Int = Toast.LENGTH_SHORT) {
    val devMode = true
    if (devMode) { showToast(scope, msg, len) }
  }

  private var toast: Toast ?= null
  fun showToast(scope: CoroutineScope, msg: String, len: Int = Toast.LENGTH_SHORT) {
    scope.launch(Dispatchers.Main) {
      if (toast != null) toast!!.cancel()
      toast = Toast.makeText(this@AnyplaceApp, msg, len)
      toast!!.show()
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