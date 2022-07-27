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
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.appSmas
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.chat.components.Conversation
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.chat.components.TopMessagesBar
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.theme.*
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasChatViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasMainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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

  //Called when the back btn on the top bar is clicked
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




