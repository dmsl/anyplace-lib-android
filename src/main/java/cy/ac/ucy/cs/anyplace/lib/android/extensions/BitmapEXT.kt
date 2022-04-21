package cy.ac.ucy.cs.anyplace.lib.android.extensions

import android.graphics.Bitmap
import android.graphics.Matrix

/**
 * EXT: rotate a bitmap
 */
fun Bitmap.rotate(angle: Int) : Bitmap {
  val matrix = Matrix()
  matrix.postRotate(angle.toFloat())
  return Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
}

fun Bitmap.resize(maxWidth: Int, maxHeight: Int): Bitmap {
  var img=this
  return if (maxHeight > 0 && maxWidth > 0) {
    val width = img.width
    val height = img.height
    val ratioBitmap = width.toFloat() / height.toFloat()
    val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()
    var finalWidth = maxWidth
    var finalHeight = maxHeight
    if (ratioMax > 1) {
      finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
    } else {
      finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
    }
    img = Bitmap.createScaledBitmap(img, finalWidth, finalHeight, true)
    img
  } else {
    this
  }
}