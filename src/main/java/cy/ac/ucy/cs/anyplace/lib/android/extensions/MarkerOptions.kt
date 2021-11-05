package cy.ac.ucy.cs.anyplace.lib.android.extensions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.ui.IconGenerator
import cy.ac.ucy.cs.anyplace.lib.R

fun MarkerOptions.iconFromVector(context: Context, @DrawableRes vectorDrawable: Int): MarkerOptions {
  this.icon(ContextCompat.getDrawable(context, vectorDrawable)?.run {
    setBounds(0, 0, intrinsicWidth, intrinsicHeight)
    val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
    draw(Canvas(bitmap))
    BitmapDescriptorFactory.fromBitmap(bitmap)
  })
  return this
}

fun MarkerOptions.iconFromShape(ctx: Context, @DrawableRes id: Int): MarkerOptions {
  this.icon(ContextCompat.getDrawable(ctx, id)?.run {
    val iconGen = IconGenerator(ctx)
    val shapeSize = ctx.resources.getDimensionPixelSize(R.dimen.map_dot_marker_size)
    val shapeDrawable = ResourcesCompat.getDrawable(ctx.resources, id, null)
    iconGen.setBackground(shapeDrawable)

    // Create a view container to set the size
    val view = View(ctx)
    view.layoutParams = ViewGroup.LayoutParams(shapeSize, shapeSize)
    iconGen.setContentView(view)

    val bitmap = iconGen.makeIcon()
    BitmapDescriptorFactory.fromBitmap(bitmap)
  })
  return this
}