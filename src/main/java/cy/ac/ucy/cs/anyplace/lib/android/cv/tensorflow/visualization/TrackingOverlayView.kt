package cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.visualization

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.visualization.MultiBoxTracker

/**
 * A simple [View] providing a render callback for [MultiBoxTracker].
 */
class TrackingOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var tracker: MultiBoxTracker? = null

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        tracker?.draw(canvas)
    }

    fun setTracker(tracker: MultiBoxTracker){
        this.tracker = tracker
    }
}