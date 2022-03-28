package cy.ac.ucy.cs.anyplace.lib.android.ui.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceFragmentCompat
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.data.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.ui.BaseActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.network.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Setup the UI using [BaseSettingsActivity],
 * and adding [MainViewModel], and Anyplace [RepoAP]
 */
@AndroidEntryPoint
abstract class AnyplaceSettingsActivity: BaseSettingsActivity() {
  protected lateinit var mainViewModel: MainViewModel
  @Inject protected lateinit var repo: RepoAP
  @Inject protected lateinit var retrofitHolderAP: RetrofitHolderAP

  override fun onResume() {
    super.onResume()
    mainViewModel.setBackFromSettings()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
  }
}