package cy.ac.ucy.cs.anyplace.lib.android.ui.selector.space

import android.content.Intent
import kotlinx.coroutines.flow.collect
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.dataStoreUser
import cy.ac.ucy.cs.anyplace.lib.android.ui.login.LoginActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.settings.SettingsDialog
import cy.ac.ucy.cs.anyplace.lib.databinding.ActivitySelectSpaceBinding
import cy.ac.ucy.cs.anyplace.lib.android.utils.NetworkListener
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SelectSpaceActivity : BaseActivity() {
  private val TAG = SelectSpaceActivity::class.java.simpleName

  private lateinit var binding: ActivitySelectSpaceBinding
  private lateinit var navController: NavController
  private lateinit var mainViewModel: MainViewModel
  private lateinit var networkListener: NetworkListener

  override fun onResume() {
    super.onResume()
    if(mainViewModel.backFromSettings) {
      lifecycleScope.launch {
        val user = dataStoreUser.readUser.first()
        if (user.accessToken.isBlank()) {
          finish()
          // startActivity(Intent(this@Select, LoginActivity::class.java))
        } else {
          reloadSpaces()
        }
      }
    }
  }


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

    mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)

    mainViewModel.readBackOnline.observe(this, { mainViewModel.backOnline = it })
    mainViewModel.readBackFromSettings.observe(this, { mainViewModel.backFromSettings = it })

    mainViewModel.readUserLoggedIn.observe(this, {
      if(it.accessToken.isBlank()) {
        // terminate activity if user not logged in
        // there is probably a better way to handle this
        // (somehow to tie this activity lifecycle with the user login)
        startActivity(Intent(this@SelectSpaceActivity,LoginActivity::class.java))
        finish()
      }
    })

    // Listen for network changes, and request data once back online
    // Runs on the very first time (when Activity is created), and then
    // on Network changes.
    lifecycleScope.launchWhenStarted {
      networkListener = NetworkListener()
      networkListener.checkNetworkAvailability(applicationContext)
          .collect { status ->
            LOG.D(TAG, "Network status: $status")
            mainViewModel.networkStatus = status
            mainViewModel.showNetworkStatus()
            readDatabase()
          }
    }

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

  private fun reloadSpaces() {
    mainViewModel.unsetBackFromSettings()
    mainViewModel.loadedApiData = false
    requestRemoteSpacesData()
  }

  private fun requestRemoteSpacesData() {
    mainViewModel.getSpaces()
  }

  private fun readDatabase() { // TODO:PM database caching
    lifecycleScope.launch {
      LOG.W("TODO:PM: cache database")
      if (!mainViewModel.loadedApiData) {
      // TODO DATABASE CACHE
      // mainViewModel.readRecipes.observeOnce(viewLifecycleOwner, { database ->
      // if (database.isNotEmpty() && !args.backFromBottomSheet) {
      // Log.d(TAG, "readDatabase: cached")
      // TODO in child...
      // mAdapter.setData(database[0].foodRecipe)
      // } else {
      LOG.D2(TAG, "readDatabase: requesting new data..")
      requestRemoteSpacesData()
      }
      // }
      // })
    }
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