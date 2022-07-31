package cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.*
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.cache.anyplace.Cache
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.query.SpacesQueryDB
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.MiscDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.ServerDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.ApUserDataStore
import cy.ac.ucy.cs.anyplace.lib.android.utils.net.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw.FloorsGetNW
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw.SpaceGetNW
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw.SpacesGetNW
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw.VersionApNW
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
TODO: PM SEPARATE CORE APP (MainViewModel) with something specific
(e.g. SpaceViewModel, Login)
 This is for Anyplace?
*/
@HiltViewModel
class AnyplaceViewModel @Inject constructor(
        app: Application,
        private val repo: RepoAP,
        private val RH: RetrofitHolderAP,
        dsServer: ServerDataStore,
        dsUser: ApUserDataStore,
        private val dsMisc: MiscDataStore,
  ): AndroidViewModel(app) {

  private val C by lazy { CONST(app.applicationContext) }
  val cache by lazy { Cache(app.applicationContext) }

  val nwVersion by lazy { VersionApNW(app as AnyplaceApp, this, RH, repo) }
  val nwSpaceGet by lazy { SpaceGetNW(app as AnyplaceApp, this, RH, repo) }
  val nwFloorsGet by lazy { FloorsGetNW(app as AnyplaceApp, this, RH, repo) }
  val nwSpacesGet by lazy { SpacesGetNW(app as AnyplaceApp, this, RH, dsUser, repo) }
  val dbqSpaces by lazy { SpacesQueryDB(app as AnyplaceApp, this, repo, dsMisc) }

  // PREFERENCES
  val prefsServer = dsServer.read

  //// RETROFIT

  var networkStatus = false
  /** normal var, filled by the observer (SelectSpaceActivity) */
  var backOnline = false
  // TODO:PM: bind this when connectivity status changes
  var readBackOnline = dsMisc.readBackOnline.asLiveData()
  var readUserLoggedIn = dsUser.readUser.asLiveData()

  var backFromSettings= false // INFO filled by the observer (collected from the fragment)
  var readBackFromSettings= dsMisc.readBackFromSettings.asLiveData()

  fun showNetworkStatus() {
    if (!networkStatus) {
      Toast.makeText(getApplication(), C.ERR_MSG_NO_INTERNET, Toast.LENGTH_SHORT).show()
      saveBackOnline(true)
    } else if(networkStatus && backOnline)  {
      Toast.makeText(getApplication(), "Back online!", Toast.LENGTH_SHORT).show()
      saveBackOnline(false)
    }
  }

  private fun saveBackOnline(value: Boolean) =
    viewModelScope.launch(Dispatchers.IO) {
      dsMisc.saveBackOnline(value)
    }

  fun setBackFromSettings() = saveBackFromSettings(true)
  fun unsetBackFromSettings() = saveBackFromSettings(false)

  private fun saveBackFromSettings(value: Boolean) =
    viewModelScope.launch(Dispatchers.IO) {  dsMisc.saveBackFromSettings(value) }
}
