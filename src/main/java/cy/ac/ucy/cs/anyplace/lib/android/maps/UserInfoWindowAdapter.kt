package cy.ac.ucy.cs.anyplace.lib.android.maps

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilColor
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Coord

enum class UserInfoType {
  OwnUser,
  OtherUser,
  OtherUserAlerting,
  SharedLocation,
}
data class UserInfoMetadata(
        val type: UserInfoType,
        /** SMAS user id */
        val uid: String,
        val coord: Coord,
        val secondsElapsed: Long,
)

/**
 * Custom Map Markers
 *
 * Can create buttons, etc here..
 */
class UserInfoWindowAdapter(private val ctx: Context) : GoogleMap.InfoWindowAdapter {

  /** This is the vertical [LinearLayout] */
  private val view: View = LayoutInflater.from(ctx).inflate(R.layout.marker_user_window, null)
  private val utlColor by lazy { UtilColor(ctx) }

  fun renderWindowText(marker: Marker, view: View) {
    val tvTitle = view.findViewById<TextView>(R.id.tv_title)
    val tvSubtitle = view.findViewById<TextView>(R.id.tv_subtitle)
    val clayout = view.findViewById<ConstraintLayout>(R.id.constraintLayout_infoWin)

    tvTitle.text=marker.title
    tvSubtitle.text=marker.snippet

    specialize(marker, clayout, tvTitle, tvSubtitle)
  }

  // TODO:PM how to set custom info?
  // 1. pass custom info
  // 2. make button to change floor
  fun specialize(marker: Marker, clayout: ConstraintLayout, tvTitle: TextView, tvSubtitle: TextView) {
    val metadata = marker.tag as UserInfoMetadata?
    if (metadata != null) {
      val drawable = clayout.background as GradientDrawable
      drawable.mutate() // only change this instance of the xml, not all components using this xml

      tvSubtitle.setTextColor(utlColor.get(R.color.darkGray))

      val col = when {
        // OWN USER
        metadata.type == UserInfoType.OwnUser -> {
          drawable.setColor(utlColor.GrayLighter())
          utlColor.get(R.color.colorPrimary)
        }

        // OTHER USER THAT IS INACTIVE
        metadata.type == UserInfoType.OtherUser && MapMarkers.isUserConsideredInactive(metadata.secondsElapsed) -> {
          utlColor.get(R.color.gray)
        }

        // OTHER USER THAT IS ALERTING
        metadata.type == UserInfoType.OtherUserAlerting -> {

          // also update the snippet
          tvSubtitle.text="(alerting)"
          tvSubtitle.setTextColor(utlColor.get(R.color.redDark))

          utlColor.get(R.color.redDark)
        }

        // OTHER USER THAT IS ACTIVE
        metadata.type == UserInfoType.OtherUser -> {
          utlColor.get(R.color.yellowDark2)
        }
        // OTHER USER THAT IS ACTIVE
        metadata.type == UserInfoType.SharedLocation-> {
          utlColor.get(R.color.black)
        }

        // this should never enter..
        else -> {
          utlColor.get(R.color.yellowDark2)
        }
      }

      drawable.setStroke(5, col)
      tvTitle.setTextColor(col)

      val btnLocation = view.findViewById<Button>(R.id.btn_coordinates)
      val latStr = "${metadata.coord.lat}".take(8)
      val lonStr = "${metadata.coord.lon}".take(8)
      val prettyLatLng = "$latStr, $lonStr"
      btnLocation.text = prettyLatLng
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