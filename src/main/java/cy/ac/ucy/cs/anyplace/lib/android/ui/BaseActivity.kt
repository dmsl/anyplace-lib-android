package cy.ac.ucy.cs.anyplace.lib.android.ui.selector.space

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity

/**
 * This is extended by any activity to provide some common settings.
 */
open class BaseActivity: AppCompatActivity() {
  @SuppressLint("SourceLockedOrientationActivity")
  override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
    super.onCreate(savedInstanceState, persistentState)

    // forcing portrait orientation for all activities
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
  }
}