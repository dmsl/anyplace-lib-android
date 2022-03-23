package cy.ac.ucy.cs.anyplace.lib.android

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.View
import android.widget.Toast
import androidx.lifecycle.asLiveData
import cy.ac.ucy.cs.anyplace.lib.legacy.Anyplace
import cy.ac.ucy.cs.anyplace.lib.android.cache.FileCache
import cy.ac.ucy.cs.anyplace.lib.android.data.store.*
import cy.ac.ucy.cs.anyplace.lib.android.di.DaggerAppComponent
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.Preferences
import cy.ac.ucy.cs.anyplace.lib.android.utils.network.RetrofitHolderAP
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
// import cy.ac.ucy.cs.anyplace.lib.android.utils.network.RetrofitHolder
// import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 *  Anyplace applications inherit from this class, to initialize:
 *  - the Anyplace API (anyplace-core)
 *  - Preferences
 *  - the FileCache
 */
abstract class AnyplaceApp : Application() {

  @Inject lateinit var retrofitHolderAP: RetrofitHolderAP

  // DATASTORES (Preferences/Settings)
  @Inject lateinit var serverDS: ServerDataStore
  @Inject lateinit var cvNavDS: CvNavDataStore
  @Inject lateinit var miscDS: MiscDataStore
  @Inject lateinit var userDS: UserDataStore
  @Inject lateinit var cvLogDSDataStore: CvLoggerDataStore
  @Inject lateinit var cvDataStoreDS: CvDataStore

  // TODO:PM: inject all those. otherwise we might have constructor issues.
  // they must be singleton, but after app ctx is created
  // example:
  // @Inject lateinit var serverDS: ServerDataStore

  /** Preferences for Cv Activities */
  /** Preferences for the CvLogger Activity */

  // CLR:PM are these not needed
  @Deprecated("")  lateinit var prefs: Preferences
  @Deprecated("")  lateinit var fileCache: FileCache
  @Deprecated("")  lateinit var apiOld: Anyplace

  override fun onCreate() {
    super.onCreate()
    LOG.D2(TAG, "onCreate")
    DaggerAppComponent.builder().application(this).build()

    observeServerPrefs()

    // CLR:PM OBSOLETE CODE
    prefs = Preferences(applicationContext)
    fileCache = FileCache(prefs)
    fileCache.initDirs()
  }

  /**
   * Dynamically provide a new Retrofit instance each time the
   * server preferences are updated.
   *
   * This is not a provide as in DI (Dependency Injection).
   * Instead we are manually setting/injecting again a new instance through the RetrofitHolder.
   */
  private fun observeServerPrefs() {
    val serverPref = serverDS.read
    serverPref.asLiveData().observeForever { prefs ->
      retrofitHolderAP.set(prefs)
      LOG.V5(TAG, "Updated backend url: ${retrofitHolderAP.baseURL}")
    }
  }

  private var toast: Toast ?= null
  fun showToast(msg: String, len: Int) {
    if (toast != null) toast!!.cancel()
    toast = Toast.makeText(this, msg, len)
    toast!!.show()
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