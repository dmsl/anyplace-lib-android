package cy.ac.ucy.cs.anyplace.lib.android.legacy_cv_gnk.tensorflow.legacy.gnk.utils

import android.graphics.Bitmap
import android.media.Image
import android.renderscript.ScriptIntrinsicYuvToRGB

@Deprecated("")
/**
 * Utility class for converting [Image] to [Bitmap].
 */
interface ImageToBitmapConverter {
    /**
     * Converts [Image] to [Bitmap] using [ScriptIntrinsicYuvToRGB].
     */
    fun imageToBitmap(image: Image): Bitmap
}