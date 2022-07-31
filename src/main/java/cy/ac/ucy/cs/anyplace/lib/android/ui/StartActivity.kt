package cy.ac.ucy.cs.anyplace.lib.android.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.compose.material.ExperimentalMaterialApi
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import cy.ac.ucy.cs.anyplace.lib.BuildConfig
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.appSmas
import cy.ac.ucy.cs.anyplace.lib.android.cache.anyplace.Cache
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.CvMapPrefs
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.selector.space.SelectSpaceActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.SmasLoginActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.SmasMainActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.AnyplaceViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * Makes some checks and opens the relevant activity:
 * - if user is not logged into SMAS, it will open Smas Login
 * - if the user haven't selectedSpace yet, it will open Space Selector
 *    - which in turn will open Anyplace login (if the user is not logged into AP)
 * - if the user has selected a space, but for some reason the relevant JSONs are not cached
 *   it will cache them
 * - finally: if all good: it will open either SMAS or Logger
 */
@AndroidEntryPoint
class StartActivity : BaseActivity() {
  private val SPLASH_TIME_OUT = 500L
  lateinit var tvVersion : TextView

  val cache by lazy { Cache(applicationContext) }
  lateinit var VMap: AnyplaceViewModel

  companion object {
    fun openActivity(prefsCv: CvMapPrefs, act: Activity) {

      // val selectedSpace=
      // val startAct
      LOG.E(TAG," OPEN ACT: COBJ")
      if (!DBG.SLR || prefsCv.selectedSpace.isEmpty()) {
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


    VMap = ViewModelProvider(this)[AnyplaceViewModel::class.java]

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

   lifecycleScope.launch(Dispatchers.IO) {
      delay(SPLASH_TIME_OUT)
      LOG.E(TAG, "START ACTIVITY: OPENING INITIAL")

      openInitialActivity()
    }
  }

  @OptIn(ExperimentalMaterialApi::class, ExperimentalPermissionsApi::class)  // compose
  private fun openInitialActivity() {
    LOG.D2(TAG_METHOD)
    LOG.E(TAG, "OPEN INITIAL")

    lifecycleScope.launch(Dispatchers.IO) {
      val chatUser = appSmas.dsSmasUser.read.first()
      val prefsCv = appSmas.dsCvMap.read.first()

      // authenticated SMAS Users try to open the Smas/Logger activity
      // If no space is selected, the user might have to authenticate to Anyplace also
      if (chatUser.sessionkey.isNotBlank()) {
        LOG.D2(TAG, "$METHOD: user: session: $chatUser")
        LOG.E(TAG, "$METHOD: user: session: $chatUser")

        val selectedSpace = prefsCv.selectedSpace
        // there is a space selection, but for some reason they are not cached locally:
        // fetch them now
        // if there isnt: it will open the FloorSelector (by [openActivity],
        // and when the user selects a floor, it will download them
        if (selectedSpace.isNotEmpty() && !cache.hasSpaceAndFloor(selectedSpace)) {
          VMap.nwSpaceGet.blockingCall(selectedSpace)
          VMap.nwFloorsGet.blockingCall(selectedSpace)
        }

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