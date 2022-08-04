package cy.ac.ucy.cs.anyplace.lib.android.ui.settings.base

import android.os.Bundle
import android.view.MenuItem
import androidx.preference.PreferenceFragmentCompat
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.ui.BaseActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Just setting up the UI for ettings
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