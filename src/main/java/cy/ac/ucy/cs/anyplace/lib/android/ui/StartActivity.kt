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
import cy.ac.ucy.cs.anyplace.lib.android.cache.anyplace.Cache
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.CvMapPrefs
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.navigator.CvNavigatorActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.selector.space.SelectSpaceActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.SmasMainActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.user.AnyplaceLoginActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.AnyplaceViewModel
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.UserAP
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
    private val TG = "act-start"

    /**
     * Open an activity depending on many conditions:
     * - whether we need to select a space
     *   - in that case: whether the user is logged into anyplace
     * - given that there was a space selection:
     *   - consider the [prefsCv.actCode] (to decide between SMAS/Logger)
     */
    fun openActivity(prefsCv: CvMapPrefs, userAP: UserAP, act: Activity) {
      val MT = ::openActivity.name
      LOG.V2(TG, MT)

      // No space is selected
      val mustSelectSpace = prefsCv.selectedSpace.isEmpty()
      val mustLoginToAnyplace =  userAP.accessToken.isBlank()
      val actCode = prefsCv.startActivity

      LOG.E(TG, "$MT: $actCode")

      val activityClass = when {
        DBG.SLR && mustSelectSpace && mustLoginToAnyplace -> AnyplaceLoginActivity::class.java
        DBG.SLR && mustSelectSpace -> SelectSpaceActivity::class.java
        actCode == CONST.START_ACT_LOGGER -> CvLoggerActivity::class.java
        actCode == CONST.START_ACT_SMAS -> SmasMainActivity::class.java
        actCode == CONST.START_ACT_NAV -> CvNavigatorActivity::class.java
        else -> act.app.getNavigatorClass()
      }

      LOG.W(TG, "$MT: open activity: ${activityClass.simpleName}")

      act.startActivity(Intent(act, activityClass))
      act.finish()  // terminate this activity
    }
  }

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (app.isNavigator()) {
      setContentView(R.layout.activity_start_anyplace)
    } else {
      setContentView(R.layout.activity_start_smas)
    }

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
    val MT = ::onResume.name
    LOG.W(TG, MT)

   lifecycleScope.launch(Dispatchers.IO) {
      delay(SPLASH_TIME_OUT)
      openInitialActivity()
    }
  }

  @OptIn(ExperimentalMaterialApi::class, ExperimentalPermissionsApi::class)  // compose
  private fun openInitialActivity() {
    val MT = ::openInitialActivity.name
    LOG.W(TG, MT)

    lifecycleScope.launch(Dispatchers.IO) {
      val chatUser = app.dsUserSmas.read.first()
      val prefsCv = app.dsCvMap.read.first()

      // authenticated SMAS Users try to open the Smas/Logger activity
      // If no space is selected, the user might have to authenticate to Anyplace also
      if (chatUser.sessionkey.isNotBlank()) {
        LOG.W(TG, "$MT: user: session: $chatUser")

        val selectedSpace = prefsCv.selectedSpace
        // there is a space selection, but for some reason they are not cached locally:
        // fetch them now
        // if there isnt: it will open the FloorSelector (by [openActivity],
        // and when the user selects a floor, it will download them
        if (selectedSpace.isNotEmpty() && !cache.hasSpaceAndFloor(selectedSpace)) {
          VMap.nwSpaceGet.blockingCall(selectedSpace)
          VMap.nwFloorsGet.blockingCall(selectedSpace)
        }

        openActivity(prefsCv, app.dsUserAP.read.first(),this@StartActivity)
      } else { // user must login to SMAS first
        LOG.W(TG, "$MT opening activity: SMAS login")

        val intent = Intent(this@StartActivity, app.getSmasBackendLoginActivity())
        startActivity(intent)
      }
      finish()
    }
  }
}