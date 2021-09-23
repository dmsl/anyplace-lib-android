package cy.ac.ucy.cs.anyplace.lib.android.ui.selector.space

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.extensions.getViewModelFactory
import cy.ac.ucy.cs.anyplace.lib.android.ui.BaseActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.login.LoginActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.settings.SettingsDialog
import cy.ac.ucy.cs.anyplace.lib.databinding.ActivitySelectSpaceBinding
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectSpaceActivity : BaseActivity() {
  // private val args by navArgs<RecipesFragmentArgs>() // TODO navigation arguments..

  private lateinit var binding: ActivitySelectSpaceBinding
  private lateinit var navController: NavController
  // private val mainViewModel by viewModels<MainViewModel> { getViewModelFactory() }
  private lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // setTheme(R.style.AppTheme) // TODO:PM splash screen

    // TODO:PM SelectSpaceActivity: Open with parameters:
    // 1: get spaces of user
    // 2: get spaces that the user can access
    // 3: get all spaces
    // 4: get spaces by type
    // 5: get spaces nearby

    binding = ActivitySelectSpaceBinding.inflate(layoutInflater)
    setContentView(binding.root)
    mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java) // CLR ?

    binding.lifecycleOwner = this
    binding.mainViewModel = this.mainViewModel

    mainViewModel.readBackOnline.observe(this, { mainViewModel.backOnline = it })
    mainViewModel.readBackFromSettings.observe(this, { mainViewModel.backFromSettings = it })

    mainViewModel.readUserLoggedIn.observe(this, {
      if(it.accessToken.isBlank()) {
        // terminate activity if user not logged in
        // there is probably a better way to handle this
        // (somehow to tie this activity lifecycle with the user login)
        startActivity(Intent(this@SelectSpaceActivity, LoginActivity::class.java))
        finish()
      }
    })

    navController = findNavController(R.id.navHostFragment)
    val appBarConfiguration = AppBarConfiguration(setOf(
      R.id.spacesListFragment,  R.id.spacesMapFragment))

    binding.bottomNavigationView.setupWithNavController(navController)
    setupActionBarWithNavController(navController, appBarConfiguration)
  }

  override fun onSupportNavigateUp(): Boolean {
    // enables navigation between the fragments
    // INFO it was enabled without this
    return navController.navigateUp() || super.onSupportNavigateUp()
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.main_menu_settings, menu)
    return super.onCreateOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val id = item.itemId
    if(id == R.id.item_settings) {
      SettingsDialog().show(supportFragmentManager, null)
    }
    return super.onOptionsItemSelected(item)
  }
}