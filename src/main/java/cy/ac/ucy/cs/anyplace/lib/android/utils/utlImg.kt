package cy.ac.ucy.cs.anyplace.lib.android.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

object utlImg {
    /**
     * Accepts a string in [Base64] format and converts it to a bitmap
     */
    fun stringToBitmap(base64: String): Bitmap? {
        val byteArray = Base64.decode(base64, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
}