package cy.ac.ucy.cs.anyplace.lib.android.maps

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilColor
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.UtilUI
import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationMethod
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Coord
import kotlinx.coroutines.CoroutineScope

enum class UserInfoType {
  OwnUser,
  OtherUser,
  SharedLocation,
}
data class UserInfoMetadata(
        val type: UserInfoType,
        /** method used to derive the location for current user [UserInfoType.OwnUser].
         * not applicable for otherusers */
        val ownLocationMethod: LocalizationMethod,
        /** SMAS user id */
        val uid: String,
        val coord: Coord,
        val secondsElapsed: Long,
        val alerting: Boolean
)

/**
 * Custom Map Markers
 *
 * Can create buttons, etc here..
 */
class UserInfoWindowAdapter(
        private val ctx: Context) : GoogleMap.InfoWindowAdapter {

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
  @SuppressLint("SetTextI18n")
  fun specialize(marker: Marker, clayout: ConstraintLayout, tvTitle: TextView, tvSubtitle: TextView) {
    val metadata = marker.tag as UserInfoMetadata?
    if (metadata != null) {
      val drawable = clayout.background as GradientDrawable
      drawable.mutate() // only change this instance of the xml, not all components using this xml

      tvTitle.visibility=View.VISIBLE
      tvSubtitle.setTextColor(utlColor.get(R.color.darkGray))

      if (metadata.alerting) {
        tvSubtitle.setTextColor(utlColor.get(R.color.redDark))
        val origText = tvSubtitle.text
        if (origText.isNullOrEmpty()) {
          tvSubtitle.text="(alerting)"
        } else {
          tvSubtitle.text="$origText\n\n(alerting)"
        }
      }

      val colorId =  when(metadata.type) {
        UserInfoType.OwnUser -> {
          drawable.setColor(utlColor.GrayLighter())
          R.color.colorPrimary
        }

         UserInfoType.OtherUser -> { // OTHER USER THAT IS:
          when { // either: inactive, alerting, or has normal status (active)
            metadata.alerting -> R.color.redDark
            MapMarkers.isUserConsideredInactive(metadata.secondsElapsed) -> R.color.gray
            else ->  R.color.yellowDark
          }
        }

        // A CHAT-SHARED LOCATION
        UserInfoType.SharedLocation-> {
          // tvTitle.visibility=View.GONE
          R.color.black
        }
      }

      val col = utlColor.get(colorId)

      drawable.setStroke(5, col)
      tvTitle.setTextColor(col)

      val btnLocation = view.findViewById<AppCompatButton>(R.id.btn_coordinates)

      btnLocation.backgroundTintList= AppCompatResources.getColorStateList(ctx, colorId)

      // var prettyLatLng: String
      val prettyLatLng = "   X: ${metadata.coord.lat}\n   Y: ${metadata.coord.lon}"
      if (metadata.type == UserInfoType.SharedLocation) {
        val btnDrawable = ResourcesCompat.getDrawable(ctx.resources, R.drawable.ic_close, null)
        btnLocation.setCompoundDrawablesWithIntrinsicBounds(btnDrawable, null,null,null)
      } else {
        // val latStart = "${metadata.coord.lat}".take(6)
        // val lonStart = "${metadata.coord.lon}".take(6)
        // val latEnd= "${metadata.coord.lat}".takeLast(2)
        // val lonEnd= "${metadata.coord.lon}".takeLast(2)
        // prettyLatLng = "${latStart}..${latEnd}, ${lonStart}..${lonEnd}"
        val btnDrawable = ResourcesCompat.getDrawable(ctx.resources, R.drawable.ic_clipboard, null)
        btnLocation.setCompoundDrawablesWithIntrinsicBounds(btnDrawable, null,null,null)
      }
      btnLocation.text = prettyLatLng

      val tvOwnInfoLocation = view.findViewById<TextView>(R.id.tv_ownInfoLocation)
      val ownLocationMethodInfo = getInfoOwnLocation(metadata.ownLocationMethod)
      if (ownLocationMethodInfo.isNotEmpty()) {
        tvOwnInfoLocation.visibility = View.VISIBLE
        tvOwnInfoLocation.text = ownLocationMethodInfo
      } else if (metadata.type == UserInfoType.SharedLocation) {
        tvOwnInfoLocation.visibility = View.VISIBLE
        tvOwnInfoLocation.text = ": shared from chat"
      } else {
        tvOwnInfoLocation.visibility = View.GONE
      }

    }
  }

  private fun getInfoOwnLocation(locMethod: LocalizationMethod) : String {
    return when (locMethod) {
      LocalizationMethod.manualByUser -> ": set manually"
      LocalizationMethod.autoMostRecent -> ": most recent"
      LocalizationMethod.cvEngine -> ": Computer Vision"
      else -> ""
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