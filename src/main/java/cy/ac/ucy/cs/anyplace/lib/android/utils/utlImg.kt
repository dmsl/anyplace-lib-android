package cy.ac.ucy.cs.anyplace.lib.android.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.webkit.MimeTypeMap
import androidx.exifinterface.media.ExifInterface
import cy.ac.ucy.cs.anyplace.lib.android.extensions.rotate
import java.io.ByteArrayOutputStream
import java.io.File

object utlImg {
  val TG = "utl-img"

  private fun returnRotatedBm(imageUri: Uri?, img: Bitmap, context: Context) : Bitmap {
    if (imageUri != null) {
      val inputStream = context.contentResolver.openInputStream(imageUri)
      if (inputStream != null) {
        val exif = ExifInterface(inputStream)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL)

        when (orientation) {
          ExifInterface.ORIENTATION_ROTATE_270 -> return img.rotate(70)
          ExifInterface.ORIENTATION_ROTATE_180 -> return img.rotate(180)
          ExifInterface.ORIENTATION_ROTATE_90 -> return img.rotate(90)
        }
      }
    }
    return img
  }

  /**
   * Encoding an image to base64
   */
  fun encodeBase64(imageUri: Uri?, context: Context): String {
    var encodedBase64 = ""
    if (imageUri != null) {
      val imageStream = context.contentResolver.openInputStream(imageUri!!)
      val selectedImage = BitmapFactory.decodeStream(imageStream)
      val rotatedBm = returnRotatedBm(imageUri, selectedImage, context)
      // val compressedBm = resize(rotatedBm, 1000, 1000)
      val baos = ByteArrayOutputStream()
      rotatedBm.compress(Bitmap.CompressFormat.JPEG, 50, baos)
      val b = baos.toByteArray()
      encodedBase64 = Base64.encodeToString(b, Base64.DEFAULT)
    }

    return encodedBase64
  }

  /**
   * Decoding a base64 to a string
   */
  fun base64toBytes(encodedBase64: String): ByteArray? {
    val MT = ::base64toBytes.name
    return try {
      return Base64.decode(encodedBase64, Base64.DEFAULT)
      // example: encodes then decodes
      // val encodedString = Base64.getEncoder().withoutPadding().encodeToString(oriString.toByteArray())
      // println(encodedString)
      // val decodedBytes = Base64.getDecoder().decode(encodedString)
      // String(bytes)
    } catch (e: IllegalArgumentException) {
      LOG.E(TG, MT, e)
      null
    }
  }

  fun base64toBitmap(base64: String): Bitmap? {
    val byteArray = Base64.decode(base64, Base64.DEFAULT)
    return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
  }

  fun getMimeType(imageUri: Uri, context: Context): String {
    val extension: String? = if (imageUri.scheme.equals(ContentResolver.SCHEME_CONTENT)) {
      val mime = MimeTypeMap.getSingleton()
      mime.getExtensionFromMimeType(context.contentResolver.getType(imageUri))
    } else {
      val path = imageUri.toString()
      MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(File(path)).toString())
    }

    return extension ?: ""
  }
}