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
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.AnyplaceDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.ApUserDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.di.RetrofitHolderAP
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
        application: Application,
        private val repo: RepoAP,
        private val RH: RetrofitHolderAP,
        dsAnyplace: AnyplaceDataStore,
        dsUserAP: ApUserDataStore,
        private val dsMisc: MiscDataStore,
  ): AndroidViewModel(application) {

  private val app = application as AnyplaceApp
  private val C by lazy { CONST(app.applicationContext) }
  val cache by lazy { Cache(app.applicationContext) }

  val nwVersion by lazy { VersionApNW(app, this, RH, repo) }
  val nwSpaceGet by lazy { SpaceGetNW(app, this, RH, repo) }
  val nwFloorsGet by lazy { FloorsGetNW(app, this, RH, repo) }
  val nwSpacesGet by lazy { SpacesGetNW(app, this, RH, dsUserAP, repo) }
  val dbqSpaces by lazy { SpacesQueryDB(app, this, repo, dsMisc) }

  // PREFERENCES
  val prefsServer = dsAnyplace.read

  //// RETROFIT

  var networkStatus = false
  /** normal var, filled by the observer (SelectSpaceActivity) */
  var backOnline = false
  // TODO:PM: bind this when connectivity status changes
  var readBackOnline = dsMisc.readBackOnline.asLiveData()
  var readUserLoggedIn = dsUserAP.read.asLiveData()

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
