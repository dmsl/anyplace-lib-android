package cy.ac.ucy.cs.anyplace.lib.android.ui.settings.base

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.source.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.utils.net.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Setup the UI using [BaseSettingsActivity],
 * and adding [MainViewModel], and Anyplace [RepoAP]
 */
@AndroidEntryPoint
abstract class AnyplaceSettingsActivity: BaseSettingsActivity() {
  protected lateinit var VMmainAp: MainViewModel
  @Inject protected lateinit var repoAP: RepoAP
  @Inject protected lateinit var repoSmas: RepoSmas
  @Inject protected lateinit var rfhAP: RetrofitHolderAP
  @Inject protected lateinit var rfhSmas: RetrofitHolderSmas

  override fun onResume() {
    super.onResume()
    VMmainAp.setBackFromSettings()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    VMmainAp = ViewModelProvider(this)[MainViewModel::class.java]
  }
}