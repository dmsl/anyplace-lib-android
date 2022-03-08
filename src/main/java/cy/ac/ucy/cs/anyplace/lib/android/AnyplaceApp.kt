package cy.ac.ucy.cs.anyplace.lib.android

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.asLiveData
import cy.ac.ucy.cs.anyplace.lib.legacy.Anyplace
import cy.ac.ucy.cs.anyplace.lib.android.cache.FileCache
import cy.ac.ucy.cs.anyplace.lib.android.data.store.*
import cy.ac.ucy.cs.anyplace.lib.android.di.DaggerAppComponent
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.Preferences
import cy.ac.ucy.cs.anyplace.lib.android.utils.network.RetrofitHolderAP
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

  // TODO:PM: inject all those. otherwise we might have constructor issues.
  // they must be singleton, but after app ctx is created
  // example:
  // @Inject lateinit var serverDS: ServerDataStore
  lateinit var serverDS: ServerDataStore
  /** Preferences for Cv Activities */
  lateinit var cvDataStoreDS: CvDataStore
  /** Preferences for the CvLogger Activity */
  lateinit var cvLogDSDataStore: CvLoggerDataStore
  lateinit var cvNavDS: CvNavDataStore
  lateinit var miscDS: MiscDataStore
  lateinit var userDS: UserDataStore

  @Inject
  lateinit var retrofitHolderAP: RetrofitHolderAP

  // CLR:PM are these not needed
  @Deprecated("")  lateinit var prefs: Preferences
  @Deprecated("")  lateinit var fileCache: FileCache
  @Deprecated("")  lateinit var apiOld: Anyplace

  override fun onCreate() {
    super.onCreate()
    LOG.D2(TAG, "onCreate")
    DaggerAppComponent.builder().application(this).build()

    val ctx = applicationContext
    serverDS = ServerDataStore(ctx)

    cvDataStoreDS = CvDataStore(ctx)
    cvLogDSDataStore = CvLoggerDataStore(ctx)
    cvNavDS= CvNavDataStore(ctx)
    miscDS = MiscDataStore(ctx)
    userDS= UserDataStore(ctx)

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

  //// MISC
  fun hasInternetConnection(): Boolean {
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