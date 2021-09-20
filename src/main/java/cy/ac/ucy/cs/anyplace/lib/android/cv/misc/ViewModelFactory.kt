package cy.ac.ucy.cs.anyplace.lib.android.cv.misc

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerViewModel

// TODO
// import org.tensorflow.lite.examples.detector.ui.detector.DetectorViewModel
// import org.tensorflow.lite.examples.detector.ui.main.MainViewModel

class ViewModelFactory(
    owner: SavedStateRegistryOwner,
    defaultArgs: Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ) = with(modelClass) {
        when {
            // isAssignableFrom(MainViewModel::class.java) -> MainViewModel()
            isAssignableFrom(CvLoggerViewModel::class.java) -> CvLoggerViewModel()
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    } as T

}