package cy.ac.ucy.cs.anyplace.lib.android.maps

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.widget.TextViewCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilColor
import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationMethod
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Coord

enum class UserInfoType {
  OwnUser,  /** Location of current user */
  OtherUser, /** Location of another user */
  SharedLocation, /** A chat location share */
  LoggerScan,  /** A scanned fingerprint, that was generated using the Logger */
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

  companion object {
    fun isUserLocation(type : UserInfoType): Boolean {
      return type == UserInfoType.OwnUser || type == UserInfoType.OtherUser
    }
  }

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

      // must set initial state to whatever we might modify below
      // (maybe these compoents are reused)
      tvTitle.visibility=View.VISIBLE
      tvTitle.setCompoundDrawablesWithIntrinsicBounds(null, null,null,null)
      tvSubtitle.visibility=View.VISIBLE
      tvSubtitle.setTextColor(utlColor.get(R.color.darkGray))

      if (metadata.alerting) {
        tvSubtitle.setTextColor(utlColor.get(R.color.locationAlert))
        val origText = tvSubtitle.text
        if (origText.isNullOrEmpty()) {
          tvSubtitle.text="(alerting)"
        } else {
          tvSubtitle.text="$origText\n\n(alerting)"
        }
      }

      val colorId =  when(metadata.type) {
        UserInfoType.OwnUser -> {

          val tvUserDrawable = ResourcesCompat.getDrawable(ctx.resources, R.drawable.ic_whereami, null)!!
          DrawableCompat.setTint(tvUserDrawable, utlColor.LocationSmas())
          tvTitle.setCompoundDrawablesWithIntrinsicBounds(tvUserDrawable, null,null,null)

          drawable.setColor(utlColor.GrayLighter())
          R.color.locationSmas
        }

         UserInfoType.OtherUser -> { // OTHER USER THAT IS:
          when { // either: inactive, alerting, or has normal status (active)
            metadata.alerting -> R.color.locationAlert
            MapMarkers.isUserConsideredInactive(metadata.secondsElapsed) -> R.color.locationOtherInactiveDark
            else ->  R.color.greenDarker
          }
        }

        // A CHAT-SHARED LOCATION
        UserInfoType.SharedLocation-> {
          // tvTitle.visibility=View.GONE
          R.color.black
        }
        UserInfoType.LoggerScan ->  {
          R.color.yellowDark
        }
      }

      val col = utlColor.get(colorId)

      drawable.setStroke(5, col)
      tvTitle.setTextColor(col)

      val btnLocation = view.findViewById<AppCompatButton>(R.id.btn_coordinates)
      btnLocation.backgroundTintList= AppCompatResources.getColorStateList(ctx, colorId)

      val prettyLatLng = "   X: ${metadata.coord.lat}\n   Y: ${metadata.coord.lon}"
      if (metadata.type == UserInfoType.SharedLocation) {
        val btnDrawable = ResourcesCompat.getDrawable(ctx.resources, R.drawable.ic_close, null)
        btnLocation.setCompoundDrawablesWithIntrinsicBounds(btnDrawable, null, null, null)
      } else if (metadata.type == UserInfoType.LoggerScan) { // computer vision scan
        val btnDrawable = ResourcesCompat.getDrawable(ctx.resources, R.drawable.ic_aperture, null)
        btnLocation.setCompoundDrawablesWithIntrinsicBounds(btnDrawable, null, null, null)
        tvSubtitle.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
      } else {
        val btnDrawable = ResourcesCompat.getDrawable(ctx.resources, R.drawable.ic_clipboard, null)
        btnLocation.setCompoundDrawablesWithIntrinsicBounds(btnDrawable, null,null,null)
      }
      btnLocation.text = prettyLatLng

      // set location info (how location was acquired)
      val tvOwnInfoLocation = view.findViewById<TextView>(R.id.tv_ownInfoLocation)
      val ownLocationMethodInfo = getLocationInfo(metadata.ownLocationMethod)
      val colorLocationMethod = getLocationInfoColor(metadata.ownLocationMethod)
      tvOwnInfoLocation.setTextColor(colorLocationMethod)
      val colorList = ColorStateList.valueOf(colorLocationMethod)
      TextViewCompat.setCompoundDrawableTintList(tvOwnInfoLocation, colorList)
      if (ownLocationMethodInfo.isNotEmpty()) {
        tvOwnInfoLocation.visibility = View.VISIBLE
        tvOwnInfoLocation.text = ownLocationMethodInfo
      } else if (metadata.type == UserInfoType.SharedLocation) {
        tvOwnInfoLocation.visibility = View.VISIBLE
        tvOwnInfoLocation.text = ": shared from chat"
      } else {
        tvOwnInfoLocation.visibility = View.GONE
      }

      // hide empty subtitles
      if (tvSubtitle.text.isNullOrEmpty()) {
        tvSubtitle.visibility = View.GONE
      }
    }
  }

  private fun getLocationInfo(locMethod: LocalizationMethod) : String {
    return when (locMethod) {
      LocalizationMethod.manualByUser -> " : set manually"
      LocalizationMethod.autoMostRecent -> " : most recent"
      LocalizationMethod.anyplaceIMU -> " : IMU Sensors"
      LocalizationMethod.anyplaceCvQueryOnline -> " : Vision (Online)"
      LocalizationMethod.anyplaceCvQueryOffline-> " : Vision (Offline)"
      LocalizationMethod.unknownMethod -> " : [something wrong]"
      else -> ""
    }
  }

  private fun getLocationInfoColor(locMethod: LocalizationMethod) : Int {
    return utlColor.get(when (locMethod) {
      LocalizationMethod.manualByUser -> R.color.locationManualDark
      LocalizationMethod.autoMostRecent -> R.color.locationRecent
      LocalizationMethod.anyplaceIMU,
      LocalizationMethod.anyplaceCvQueryOnline,
      LocalizationMethod.anyplaceCvQueryOffline,
        -> R.color.locationSmas
      else -> R.color.gray
    })
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