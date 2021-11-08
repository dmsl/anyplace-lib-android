package cy.ac.ucy.cs.anyplace.lib.android.ui.cv

import android.content.Context
import androidx.camera.core.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.StatusUpdater
import kotlinx.coroutines.CoroutineScope

/**
 * CLR:PM either CLR or use it...
 *
 * Encapsulating UI operations for the CV Activities.
 * This allwos sharing code between:
 * - [CvLoggerActivity]
 * - [CvNavigatorActivity] TODO:PM
 */
open class UiActivityCvCommon(
    protected val ctx: Context,
    protected val scope: CoroutineScope,
    // private val viewModel: CvLoggerViewModel,
    // private val binding: ActivityCvLoggerBinding,
    protected val statusUpdater: StatusUpdater,
    ) {
  // TODO common stuff here?

}