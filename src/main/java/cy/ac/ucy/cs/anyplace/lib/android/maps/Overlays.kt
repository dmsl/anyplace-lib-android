package cy.ac.ucy.cs.anyplace.lib.android.maps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import cy.ac.ucy.cs.anyplace.lib.android.maps.camera.CameraAndViewport.Companion.LATLNG_UCY
import java.io.IOException
import java.io.InputStream

class Overlays(private val ctx: Context) {

  // TODO:PM download svg using coil: https://stackoverflow.com/a/63869597/776345 (wont do svg?)
  // TODO:PM coil: download and cache to storage..

  fun getImgTestAsset(): InputStream? {
    val floorplanFilename = "t1.png"
    try {
      with(ctx.assets.open(floorplanFilename)){  return this  }
    } catch (e: IOException) {
      // log error
    }
    return null
  }

  fun addSpaceOverlay(bitmap: Bitmap?, map: GoogleMap, bounds: LatLngBounds): GroundOverlay? {
    if (bitmap != null) {
      // val bmap = BitmapFactory.decodeStream(getImgTestAsset())
      val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
      return map.addGroundOverlay(
        GroundOverlayOptions().apply {
          // position(LATLNG_UCY, 100f, 500f)
          // positionFromBounds(BOUNDS_UCY)
          positionFromBounds(bounds)
          image (bitmapDescriptor)
        }
      )
    }
    return null
  }


  // CLR:PM
  private fun bitmapFromVectorKt(vectorResID: Int) : BitmapDescriptor {
    val vectorDrawable=ContextCompat.getDrawable(ctx ,vectorResID)
    vectorDrawable!!.setBounds(0,0,vectorDrawable.intrinsicWidth,vectorDrawable.intrinsicHeight)
    val bitmap=Bitmap.createBitmap(vectorDrawable.intrinsicWidth,vectorDrawable.intrinsicHeight,Bitmap.Config.ARGB_8888)
    val canvas=Canvas(bitmap)
    vectorDrawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
  }
  /**
   * Convert a vector asset (SVG) to a bitmap
   */
  // val floorplanFilename = "file:///android_asset/demo.spaces/vessel/stena_flavia_deck5.svg"
  // // val floorplanFilename = "demo.spaces/vessel/stena_flavia_deck5.svg"
  // // val svg = SVG.getFromAsset(ctx.assets, floorplanFilename)
  // LOG.D("Width: ${svg.documentWidth}")
  // LOG.D("Height: ${svg.documentHeight}")
  //
  // val bmp = Bitmap.createBitmap(
  //   ceil(svg.documentWidth.toDouble()).toInt(),
  //   ceil(svg.documentHeight.toDouble()).toInt(), Bitmap.Config.ARGB_8888
  // )
  //
  // // Bitmap  newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
  // // (int) Math.ceil(svg.getDocumentHeight()),
  // // Bitmap.Config.ARGB_8888);
  // val bmcanvas = Canvas(bmp)
  // bmcanvas.drawRGB(255, 255, 255)  // clear bg to white
  // svg.renderToCanvas(bmcanvas
  // private fun  bitmapDescriptorFromVector(vectorResId: Int): BitmapDescriptor {
  //   val vectorDrawable = ContextCompat.getDrawable(ctx, vectorResId)
  //   vectorDrawable!!.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
  //   val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
  //   val canvas =  Canvas(bitmap)
  //   vectorDrawable.draw(canvas)
  //   return BitmapDescriptorFactory.fromBitmap(bitmap)
  // }
}