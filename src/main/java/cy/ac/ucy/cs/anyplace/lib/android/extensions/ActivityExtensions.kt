package cy.ac.ucy.cs.anyplace.lib.android.extensions

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.AndroidViewModel
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.cv.misc.ViewModelFactory
import cy.ac.ucy.cs.anyplace.lib.android.data.datastore.DataStoreMisc
import cy.ac.ucy.cs.anyplace.lib.android.data.datastore.DataStoreServer
import cy.ac.ucy.cs.anyplace.lib.android.data.datastore.DataStoreUser

fun AppCompatActivity.getViewModelFactory(): ViewModelFactory {
    return ViewModelFactory(this)
}

fun ComponentActivity.registerForActivityResult(
    callback: ActivityResultCallback<ActivityResult>
): ActivityResultLauncher<Intent> {
    return this.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        callback
    )
}

// EXTENSION FUNCTIONS
val Activity.app: AnyplaceApp get() = this.application as AnyplaceApp
val Activity.dataStoreServer: DataStoreServer get() = this.app.dataStoreServer
val Activity.dataStoreMisc: DataStoreMisc get() = this.app.dataStoreMisc
val Activity.dataStoreUser: DataStoreUser get() = this.app.dataStoreUser

val DialogFragment.app: AnyplaceApp get() = this.activity?.application as AnyplaceApp
val AndroidViewModel.app: AnyplaceApp get() = getApplication<AnyplaceApp>()

// Extending Any (Java Object): name convention for loggin: ap_<className>
val Any.TAG : String
    get() = "ap_${this::class.java.simpleName}"
