package cy.ac.ucy.cs.anyplace.lib.android.maps

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilColor


enum class UserInfoType { OwnUser, OtherUser }
data class UserInfoMetadata(
        val type: UserInfoType
)

/**
 * Custom Map Markers
 *
 * Can create buttons, etc here..
 */
class UserInfoWindowAdapter(private val ctx: Context) : GoogleMap.InfoWindowAdapter {

 private val view: View = LayoutInflater.from(ctx).inflate(R.layout.marker_user_window, null)
 private val utlColor by lazy { UtilColor(ctx) }

  fun renderWindowText(marker: Marker, view: View) {
    val tvTitle = view.findViewById<TextView>(R.id.tv_title)
    val tvSubtitle = view.findViewById<TextView>(R.id.tv_subtitle)

    tvTitle.text=marker.title
    tvSubtitle.text=marker.snippet

    specialize(marker, tvTitle)
  }

  // TODO:PM how to set custom info?
  // 1. pass custom info
  // 2. make button to change floor
  fun specialize(marker: Marker, tvTitle: TextView) {
    val metadata = marker.tag as UserInfoMetadata?
    if (metadata != null) {
      val drawable = view.background as GradientDrawable
      drawable.mutate() // only change this instance of the xml, not all components using this xml

      val col = when (metadata.type) {
        UserInfoType.OwnUser -> {
          drawable.setColor(utlColor.GrayLighter())

          utlColor.get(R.color.colorPrimary)
        }

        UserInfoType.OtherUser-> {
          utlColor.get(R.color.yellowDark2)
        }
      }

      drawable.setStroke(5, col)
      tvTitle.setTextColor(col)
    }
  }

  override fun getInfoContents(marker: Marker): View {
    renderWindowText(marker, view)
    return view
  }

  override fun getInfoWindow(marker: Marker): View {
    renderWindowText(marker, view)
    return view
  }
}