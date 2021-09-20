package cy.ac.ucy.cs.anyplace.lib.android.cv.tensorflow.utils

import android.graphics.Bitmap
import android.media.Image
import android.renderscript.ScriptIntrinsicYuvToRGB

/**
 * Utility class for converting [Image] to [Bitmap].
 */
interface ImageToBitmapConverter {
    /**
     * Converts [Image] to [Bitmap] using [ScriptIntrinsicYuvToRGB].
     */
    fun imageToBitmap(image: Image): Bitmap
}