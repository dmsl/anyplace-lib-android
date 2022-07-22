package cy.ac.ucy.cs.anyplace.lib.android.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.compose.material.ExperimentalMaterialApi
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import cy.ac.ucy.cs.anyplace.lib.BuildConfig
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.appSmas
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.SmasLoginActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.SmasMainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class StartActivity : Activity() {
  private val SPLASH_TIME_OUT = 500L
  lateinit var tvVersion : TextView

  companion object {
   fun openActivity(actCode: String, act: Activity) {
     val cls = when (actCode) {
       CONST.START_ACT_LOGGER -> CvLoggerActivity::class.java
       CONST.START_ACT_SMAS -> SmasMainActivity::class.java
       // start smas by default
       else -> SmasMainActivity::class.java
     }
     act.startActivity(Intent(act, cls))
     act.finish()  // terminate this activity
   }
  }

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_start)
    tvVersion = findViewById<View>(R.id.tvVersion) as TextView
    setupVersion()
  }

  private fun setupVersion() {
    val versionStr = "ver: ${BuildConfig.VERSION_CODE}"
    tvVersion.text = versionStr
  }

  override fun onResume() {
    super.onResume()
    CoroutineScope(Main).launch {
      delay(SPLASH_TIME_OUT)
      openInitialActivity()
    }
  }

  @OptIn(ExperimentalMaterialApi::class, ExperimentalPermissionsApi::class)  // compose
  private fun openInitialActivity() {
    LOG.D2(TAG_METHOD)
    CoroutineScope(Main).launch {
      // authenticated users go straight to the Main Smas activity
      val chatUser = appSmas.dsSmasUser.read.first()
      // appSmas.dsCvMap.putString()
      val prefsCv = appSmas.dsCvMap.read.first()
      if (chatUser.sessionkey.isNotBlank()) {
        LOG.D2(TAG, "$METHOD: user: session: $chatUser")

        openActivity(prefsCv.startActivity, this@StartActivity)
      } else {
        LOG.D2(TAG, "Opening activity: Login")
        val intent = Intent(this@StartActivity, SmasLoginActivity::class.java)
        startActivity(intent)
      }
      finish()
    }
  }
}