package cy.ac.ucy.cs.anyplace.lib.android.ui.smas.chat

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.*
import androidx.lifecycle.ViewModelProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.extensions.appSmas
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.chat.components.Conversation
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.chat.components.TopMessagesBar
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.chat.theme.*
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasChatViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasMainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * This activity is using [Jetpack Compose](https://developer.android.com/jetpack/compose)
 *
 * IMPORTANT REGARDING Data Access ([ViewModel]):
 *
 * ALL MESSAGES ARE PREPARED BY THE PARENT ACTIVITY ([SmasMainActivity])
 *
 * + Receiving messages:
 *   - this activity is launched by [SmasMainActivity] (parent).
 *   - parent stays open (in background), and is pulling/handling the msgs
 *     - this activity just renders whatever [SmasApp.msgList] has
 *   - e.g. [SmasMainActivity.collectMessages] does exactly that
 *
 * + this activity never instantiates [SmasMainViewModel] (it is done only from parent)
 *
 * + SmasChatViewModel:
 *   - it is instanciated and used in here ([VMchat]) for sending messages
 *   - however, when a msg is sent we want to immediately pull msgs back, to include it
 *     - like a verification that it was sent
 *     - alternatively we would have to wait for the parent loop to run
 *   - how we fetch it earlier?
 *     - this app never pulls "directly" any new msgs. it is parent's responsibility
 *     - to accomplish that, we:
 *     - use [AppSmas.pullMessagesONCE], which uses parent's VM ([SmasChatViewModel])
 *       - that VM was registered by parent using [setMainActivityVMs]
 *       - so AppSmas uses that (parent) VM to call [nwPullMessagesONCE]
 *       - so this way, we are making our parent (indirectly) to pull new messages a bit earlier
 *         instead of waiting of the parent's loop to run
 */
@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalMaterialApi
@ExperimentalPermissionsApi
@AndroidEntryPoint
class SmasChatActivity : AppCompatActivity() {

  private lateinit var VMchat: SmasChatViewModel
  @Inject lateinit var repo: RepoSmas

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    VMchat = ViewModelProvider(this)[SmasChatViewModel::class.java]

    setContent {
      Scaffold(
              topBar = { TopMessagesBar(::onBackClick) },
              content = { Conversation(appSmas, VMchat, supportFragmentManager, repo, ::returnCoords) },
              backgroundColor = WhiteGray
      )
    }

    val rootView= (findViewById<View>(android.R.id.content) as ViewGroup).getChildAt(0) as ViewGroup
    app.setMainView(rootView, true)
  }

  // Called when the back btn on the top bar is clicked
  private fun onBackClick() {
    intent.data = null
    finish()
  }

  //Called when the location button in a message is clicked
  private fun returnCoords(lat: Double, lon: Double, level: Int) {
    setResult(Activity.RESULT_OK, Intent()
            .putExtra("lat", lat)
            .putExtra("lon", lon)
            .putExtra("level", level)
    )
    finish()
  }
}




