package cy.ac.ucy.cs.anyplace.lib.android.ui.selector.space

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.ui.BaseActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.settings.SettingsCvActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.user.AnyplaceLoginActivity
import cy.ac.ucy.cs.anyplace.lib.databinding.ActivitySelectSpaceBinding
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.AnyplaceViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.SpacesViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectSpaceActivity : BaseActivity(), SearchView.OnQueryTextListener {
  private lateinit var binding: ActivitySelectSpaceBinding
  private lateinit var navController: NavController
  private lateinit var VMap: AnyplaceViewModel
  private lateinit var VMspaces: SpacesViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // setTheme(R.style.AppTheme) // INFO alternative way to present a splash screen

    // this may be opened with parameters:
    // 1: get spaces of user
    // 2: get spaces that the user can access
    // 3: get all spaces
    // 4: get spaces by type
    // 5: get spaces nearby

    binding = ActivitySelectSpaceBinding.inflate(layoutInflater)
    setContentView(binding.root)
    VMap = ViewModelProvider(this)[AnyplaceViewModel::class.java]
    VMspaces = ViewModelProvider(this)[SpacesViewModel::class.java]

    binding.lifecycleOwner = this

    VMap.readBackOnline.observe(this) { VMap.backOnline = it }
    VMap.readBackFromSettings.observe(this) { VMap.backFromSettings = it }

    requireAnyplaceLogin()

    // lifecycleScope.launch { spaceViewModel.runFirstQuery() }
    // The idea was to have a list, or a map view for a Space Selector. but it's incomplete.
    // navController = findNavController(R.id.navHostFragment)
    // val appBarConfiguration=AppBarConfiguration(setOf( R.id.spacesListFragment, R.id.spacesMapFragment))
    // binding.bottomNavigationView.setupWithNavController(navController)
    // setupActionBarWithNavController(navController, appBarConfiguration)
  }

  /**
   * This makes sures the user does not proceed unless there is Anyplace authentication
   */
  private fun requireAnyplaceLogin() {
    VMap.readUserLoggedIn.observe(this) {
      // INFO: this will force an anyplace login
      if (it.accessToken.isBlank()) {
        // terminate activity if user not logged in
        // there is probably a better way to handle this
        // (somehow to tie this activity lifecycle with the user login)
        startActivity(Intent(this@SelectSpaceActivity, AnyplaceLoginActivity::class.java))
        finish()
      }
    }
  }

  override fun onSupportNavigateUp(): Boolean {
    // enables navigation between the fragments
    // INFO it was enabled without this
    return navController.navigateUp() || super.onSupportNavigateUp()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.select_space_top_menu, menu)
    val search = menu.findItem(R.id.item_search_spaces)
    val searchView = search.actionView as? SearchView
    searchView?.queryHint = getString(R.string.search_space)
    // searchView?.isSubmitButtonEnabled = true
    searchView?.setOnQueryTextListener(this)

    return super.onCreateOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val id = item.itemId
    if(id == R.id.item_settings) {
      startActivity(Intent(this, SettingsCvActivity::class.java))
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onQueryTextSubmit(query: String?): Boolean {
    return true
  }

  override fun onQueryTextChange(newText: String?): Boolean {
    newText?.let { VMspaces.searchViewData.value = it }
    return true
  }
}