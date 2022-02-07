package cy.ac.ucy.cs.anyplace.lib.android.ui.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.Repository
import cy.ac.ucy.cs.anyplace.lib.android.data.datastore.DataStoreServer
import cy.ac.ucy.cs.anyplace.lib.android.extensions.dataStoreServer
import cy.ac.ucy.cs.anyplace.lib.android.ui.BaseActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.network.RetrofitHolder
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
open class BaseSettingsActivity: BaseActivity() {
  protected lateinit var mainViewModel: MainViewModel

  @Inject protected lateinit var repo: Repository
  @Inject protected lateinit var retrofitHolder: RetrofitHolder

  override fun onResume() {
    super.onResume()
    mainViewModel.setBackFromSettings()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)
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