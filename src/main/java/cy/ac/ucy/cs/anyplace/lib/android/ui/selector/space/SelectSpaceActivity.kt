package cy.ac.ucy.cs.anyplace.lib.android.ui.selector.space

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.ui.BaseActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.settings.SettingsCvActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.user.AnyplaceLoginActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.UtilUI
import cy.ac.ucy.cs.anyplace.lib.databinding.ActivitySelectSpaceBinding
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.AnyplaceViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.update

/**
 * Sample activity for fetching [Spaces] and selecting one ([Space]) from the Anyplace backend.
 * The space is then stored to [CvMapActivity], and it is used in its child activities.
 */
@AndroidEntryPoint
class SelectSpaceActivity : BaseActivity(), SearchView.OnQueryTextListener {
  private lateinit var binding: ActivitySelectSpaceBinding
  private lateinit var navController: NavController
  lateinit var VM: AnyplaceViewModel
  lateinit var VMcv: CvViewModel

  val utlUi by lazy { UtilUI(applicationContext, lifecycleScope) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivitySelectSpaceBinding.inflate(layoutInflater)
    setContentView(binding.root)
    app.setMainView(binding.root, true)

    VM = ViewModelProvider(this)[AnyplaceViewModel::class.java]
    VMcv = ViewModelProvider(this)[CvViewModel::class.java]

    binding.lifecycleOwner = this

    VM.readBackOnline.observe(this) { VM.backOnline = it }
    VM.readBackFromSettings.observe(this) { VM.backFromSettings = it }

    requireAnyplaceLogin()
    setupNavController()
    setupFabQuery()
  }

  private fun setupNavController() {
    navController = findNavController(R.id.navHostFragment)
    // map not being used..
    // The idea was to have a list, or a map view for a Space Selector. but it's incomplete.
    val appBarConfiguration = AppBarConfiguration(setOf(
            R.id.spacesListFragment,  R.id.spacesMapFragment))

    binding.bottomNavigationView.setupWithNavController(navController)
    binding.bottomNavigationView.elevation=1f
    setupActionBarWithNavController(navController, appBarConfiguration)
  }

  /**
   * This makes sures the user does not proceed unless there is Anyplace authentication
   */
  private fun requireAnyplaceLogin() {
    VM.readUserLoggedIn.observe(this) {
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

  private fun setupFabQuery() {
    binding.fabFilterSpaces.setOnClickListener {
      if (app.spaceSelectionInProgress) {
        val utlUi = UtilUI(applicationContext, lifecycleScope)
        utlUi.attentionInvalidOption(binding.fabFilterSpaces)
        return@setOnClickListener
      }

      // if(VM.dbqSpaces.resultsLoaded) {
      navController.navigate(R.id.action_spacesListFragment_to_spaceFilterBottomSheet)
      // } else {
      //   app.snackbarWarning(lifecycleScope, "No spaces loaded")
      // }
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
    searchView?.setOnQueryTextListener(this)
    
    return super.onCreateOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val id = item.itemId
    if(id == R.id.item_settings) {
      if (app.spaceSelectionInProgress)  {
        val view = findViewById<View>(R.id.item_settings)
        utlUi.attentionInvalidOption(view)
      } else {
        startActivity(Intent(this, SettingsCvActivity::class.java))
      }
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onQueryTextSubmit(query: String?): Boolean {
    return true
  }

  override fun onQueryTextChange(newText: String?): Boolean {
    if (app.spaceSelectionInProgress) return true
    app.dbqSpaces.txtQuery.update { newText?:"" }

    return true
  }
}