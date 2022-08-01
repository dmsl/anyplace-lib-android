package cy.ac.ucy.cs.anyplace.lib.android.utils.demo

import android.content.Context
import com.google.gson.Gson
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Levels
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Space
import java.io.IOException
import java.io.InputStream
import java.lang.Exception

open class AssetReader(val ctx: Context) {
  private val gson = Gson()

  private val SPACE_UCY_CS = "demo/spaces/building/ucy"
  private val SPACE_STENA_FLAVIA = "demo/spaces/vessel/flavia"

  private val SELECTED_SPACE = SPACE_STENA_FLAVIA

  fun getFloors(): Levels? {
    val str = getFloorsStr()
    str.let {
      try {
        return Gson().fromJson(str, Levels::class.java)
      } catch (e: Exception) {
        LOG.E("Failed to parse: $str")
      }
    }
    return null
  }


  fun getSpace(): Space? {
    // TODO in endpoint result: /api/mapping/space/get: unwrap it:
    // e.g.: { "space": {..space data..} -> {..space data..}
    val str = getSpaceStr()
    str.let {
      try {
        return Gson().fromJson(str, Space::class.java)
      } catch (e: Exception) {
        LOG.E("Failed to parse: $str")
      }
    }
    return null
  }

  private fun getSpaceStr(): String? {
    return getJsonDataFromAsset(ctx, "$SELECTED_SPACE/space.json")
  }

  private fun getFloorsStr(): String? {
    return getJsonDataFromAsset(ctx, "$SELECTED_SPACE/floors.json")
  }
  fun getFloorplan64Str(): String? {
    return getJsonDataFromAsset(ctx, "$SELECTED_SPACE/floorplan3.base64")
  }

  protected fun getJsonDataFromAsset(context: Context, filename: String): String? {
    val jsonString: String
    try {
      jsonString = context.assets.open(filename).bufferedReader().use { it.readText() }
    } catch (ioException: IOException) {
      ioException.printStackTrace()
      LOG.E(TAG, "Error reading: $filename: ${ioException.message}")
      return null
    }
    return jsonString
  }

  /**
   * Was used for testing floorplan overlays on [GoogleMap]
   */
  fun readImageAsset(): InputStream? {
    val floorplanFilename = "t1.png"
    try {
      with(ctx.assets.open(floorplanFilename)){  return this  }
    } catch (e: IOException) {
      // log error
    }
    return null
  }
}