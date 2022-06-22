package cy.ac.ucy.cs.anyplace.lib.android.maps

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import cy.ac.ucy.cs.anyplace.lib.R

class UserInfoWindowAdapter(private val ctx: Context) : GoogleMap.InfoWindowAdapter {

 private val view: View = LayoutInflater.from(ctx).inflate(R.layout.marker_user_window, null)

  fun renderWindowText(marker: Marker, view: View) {
    val tvTitle = view.findViewById<TextView>(R.id.tv_title)
    val tvSubtitle = view.findViewById<TextView>(R.id.tv_subtitle)

    tvTitle.text=marker.title
    tvSubtitle.text=marker.snippet

    // TODO:PM how to set custom info?
    // 1. pass custom info
    // 2. make button to change floor

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