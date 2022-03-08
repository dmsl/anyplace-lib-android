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
 * Just setting up the UI
 */
@AndroidEntryPoint
open class BaseSettingsActivity: BaseActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.settings_base_activity)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
  }

  fun setupFragment(fragment: PreferenceFragmentCompat, bundle: Bundle?) {
    if (bundle == null) {
      supportFragmentManager
          .beginTransaction()
          .replace(R.id.settings, fragment)
          .commit()
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val id = item.itemId
    if (id==android.R.id.home) {
      finish()
      return true
    }
    return false
  }
}