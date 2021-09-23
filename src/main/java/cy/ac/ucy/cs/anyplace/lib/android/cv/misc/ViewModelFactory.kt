package cy.ac.ucy.cs.anyplace.lib.android.cv.misc

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import cy.ac.ucy.cs.anyplace.lib.android.data.Repository
import cy.ac.ucy.cs.anyplace.lib.android.data.datastore.DataStoreMisc
import cy.ac.ucy.cs.anyplace.lib.android.data.datastore.DataStoreServer
import cy.ac.ucy.cs.anyplace.lib.android.data.datastore.DataStoreUser
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerViewModel
import cy.ac.ucy.cs.anyplace.lib.android.utils.network.RetrofitHolder
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.MainViewModel
import javax.inject.Inject


class ViewModelFactory @Inject constructor(
    owner: SavedStateRegistryOwner,
    defaultArgs: Bundle? = null, ) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ) = with(modelClass) {
        when {
            isAssignableFrom(CvLoggerViewModel::class.java) -> CvLoggerViewModel()
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    } as T

}