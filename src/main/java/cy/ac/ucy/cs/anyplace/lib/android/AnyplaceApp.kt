package cy.ac.ucy.cs.anyplace.lib.android

import android.app.Activity
import android.app.Application
import cy.ac.ucy.cs.anyplace.lib.Anyplace
import cy.ac.ucy.cs.anyplace.lib.android.cache.FileCache
import cy.ac.ucy.cs.anyplace.lib.android.utils.Preferences

/**
 *  Anyplace applications inherit from this class which initializes:
 *  - the Anyplace API (anyplace-core)
 *  - Preferences
 *  - the FileCache
 *
 *  TODO:
 *  - control toast messages from her
 */
open class AnyplaceApp: Application() {
  private val TAG = AnyplaceApp::class.java.simpleName

  lateinit var api: Anyplace
  lateinit var prefs: Preferences
  lateinit var fileCache: FileCache

  override fun onCreate() {
    super.onCreate()

    LOG.D2(TAG, "AnyplaceApp:: onCreate")

    prefs = Preferences(applicationContext)
    api = Anyplace(prefs.ip, prefs.port, prefs.cacheDir)
    fileCache = FileCache(prefs)
    fileCache.initDirs()
  }
}

// EXTENSION FUNCTIONS
val Activity.app: AnyplaceApp get() = this.application as AnyplaceApp
