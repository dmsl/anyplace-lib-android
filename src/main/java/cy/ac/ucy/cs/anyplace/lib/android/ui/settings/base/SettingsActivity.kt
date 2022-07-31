package cy.ac.ucy.cs.anyplace.lib.android.ui.settings.base

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.source.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.android.utils.net.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.AnyplaceViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Setup the UI using [BaseSettingsActivity],
 * and adding [AnyplaceViewModel], and Anyplace [RepoAP]
 */
@AndroidEntryPoint
abstract class SettingsActivity: BaseSettingsActivity() {
  protected lateinit var VMap: AnyplaceViewModel
  @Inject protected lateinit var repoAP: RepoAP
  @Inject protected lateinit var repoSmas: RepoSmas
  @Inject protected lateinit var rfhAP: RetrofitHolderAP
  @Inject protected lateinit var rfhSmas: RetrofitHolderSmas

  override fun onResume() {
    super.onResume()
    VMap.setBackFromSettings()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    VMap = ViewModelProvider(this)[AnyplaceViewModel::class.java]
  }
}