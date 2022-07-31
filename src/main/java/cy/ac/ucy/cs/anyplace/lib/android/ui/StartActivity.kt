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
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.appSmas
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.CvMapPrefs
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.selector.space.SelectSpaceActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.SmasLoginActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.SmasMainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class StartActivity : Activity() {
  private val SPLASH_TIME_OUT = 500L
  lateinit var tvVersion : TextView

  companion object {
    fun openActivity(prefsCv: CvMapPrefs, act: Activity) {

      // val selectedSpace=
      // val startAct
      LOG.E(TAG," OPEN ACT: COBJ")
      if (prefsCv.selectedSpace.isEmpty()) {
        LOG.E(TAG, "OPENING: ACT: IS EMPTY... starting space")
        LOG.W(TAG, "$METHOD: must select space first")
          act.startActivity(Intent(act, SelectSpaceActivity::class.java))
      } else {
        val actCode = prefsCv.startActivity
        LOG.E(TAG, "OPENING: ACT: starting ACTIVITY.. $actCode")
        val cls = when (actCode) {
          CONST.START_ACT_LOGGER -> CvLoggerActivity::class.java
          CONST.START_ACT_SMAS -> SmasMainActivity::class.java
          // start smas by default
          else -> SmasMainActivity::class.java
        }
        act.startActivity(Intent(act, cls))
      }

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
    LOG.E(TAG, "START ACTIVITY")
    LOG.E(TAG, "START ACTIVITY")
    LOG.E(TAG, "START ACTIVITY")
    CoroutineScope(Dispatchers.IO).launch {
      delay(SPLASH_TIME_OUT)
      LOG.E(TAG, "START ACTIVITY: OPENING INITIAL")
      openInitialActivity(this)
    }
  }

  @OptIn(ExperimentalMaterialApi::class, ExperimentalPermissionsApi::class)  // compose
  private fun openInitialActivity(scope: CoroutineScope) {
    LOG.D2(TAG_METHOD)
    LOG.E(TAG, "OPEN INITIAL")
    CoroutineScope(Dispatchers.IO).launch {
      val chatUser = appSmas.dsSmasUser.read.first()
      val prefsCv = appSmas.dsCvMap.read.first()

      // authenticated SMAS Users try to open the Smas/Logger activity
      // If no space is selected, the user might have to authenticate to Anyplace also
      if (chatUser.sessionkey.isNotBlank()) {
        LOG.D2(TAG, "$METHOD: user: session: $chatUser")
        LOG.E(TAG, "$METHOD: user: session: $chatUser")

        openActivity(prefsCv, this@StartActivity)
      } else { // user must login to SMAS first
        LOG.D2(TAG, "Opening activity: Login")
        val intent = Intent(this@StartActivity, SmasLoginActivity::class.java)
        startActivity(intent)
      }
      finish()
    }
  }
}