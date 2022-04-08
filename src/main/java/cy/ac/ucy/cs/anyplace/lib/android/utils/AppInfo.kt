package cy.ac.ucy.cs.anyplace.lib.android.utils

import android.content.Context
import android.content.pm.PackageManager
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG

@Deprecated("Use BuildConfig")
class AppInfo(private val ctx: Context) {

  val version : String? get() {
    return try {
      val PI = ctx.packageManager.getPackageInfo(ctx.packageName, 0)

      PI.versionName
    } catch (e: PackageManager.NameNotFoundException) {
      LOG.E(TAG, "Cannot get version name.")

      null
    }
  }
}