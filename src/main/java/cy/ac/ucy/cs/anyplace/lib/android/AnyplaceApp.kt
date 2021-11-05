package cy.ac.ucy.cs.anyplace.lib.android

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.asLiveData
import cy.ac.ucy.cs.anyplace.lib.Anyplace
import cy.ac.ucy.cs.anyplace.lib.android.cache.FileCache
import cy.ac.ucy.cs.anyplace.lib.android.data.datastore.DataStoreCvLogger
import cy.ac.ucy.cs.anyplace.lib.android.data.datastore.DataStoreMisc
import cy.ac.ucy.cs.anyplace.lib.android.data.datastore.DataStoreServer
import cy.ac.ucy.cs.anyplace.lib.android.data.datastore.DataStoreUser
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.Preferences
import cy.ac.ucy.cs.anyplace.lib.android.utils.network.RetrofitHolder
// import cy.ac.ucy.cs.anyplace.lib.android.utils.network.RetrofitHolder
// import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 *  Anyplace applications should inherit from this class, which initializes:
 *  - the Anyplace API (anyplace-core)
 *  - Preferences
 *  - the FileCache
 *
 *  TODO:
 *  - control toast messages from here
 */
abstract class AnyplaceApp : Application() {

  lateinit var dataStoreServer: DataStoreServer
  lateinit var dataStoreCvLogger: DataStoreCvLogger
  lateinit var dataStoreMisc: DataStoreMisc
  lateinit var dataStoreUser: DataStoreUser

  @Inject
  lateinit var retrofitHolder: RetrofitHolder

  abstract val navigator: Boolean
  abstract val logger: Boolean

  // CLR:PM are these not needed
  @Deprecated("")  lateinit var prefs: Preferences
  @Deprecated("")  lateinit var fileCache: FileCache
  @Deprecated("")  lateinit var apiOld: Anyplace

  override fun onCreate() {
    super.onCreate()
    LOG.D2(TAG, "onCreate")
    dataStoreServer = DataStoreServer(applicationContext)
    dataStoreCvLogger = DataStoreCvLogger(applicationContext)
    dataStoreMisc = DataStoreMisc(applicationContext)
    dataStoreUser= DataStoreUser(applicationContext)

    dynamicallyInjectRetrofit()

    // CLR:PM
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
  private fun dynamicallyInjectRetrofit() {
    val serverPreferences = dataStoreServer.readServerPrefs
    serverPreferences.asLiveData().observeForever { prefs ->
      retrofitHolder.set(prefs)
      LOG.V5("dynamicallyProvideRetrofit: Updated backend url: ${retrofitHolder.baseURL}")
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