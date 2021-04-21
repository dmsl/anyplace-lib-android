package cy.ac.ucy.cs.anyplace.lib.android.googlemap

import android.widget.SearchView
import com.google.android.gms.maps.GoogleMap
import cy.ac.ucy.cs.anyplace.lib.android.nav.AnyPlaceSeachingHelper.SearchTypes

class MapUtils     // private void initListeners() {
(private val mMap: GoogleMap, private val searchType: SearchTypes) {


  private val searchView: SearchView? = null
  private val visiblePois: VisibleObject<*>? = null
  private val pathLineInside: Any? = null
  //   mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
  //
  //
  //     @Override
  //     public void onCameraChange(CameraPosition position) {
  //
  //       // change search box message and clear pois
  //       if (searchType != AnyPlaceSeachingHelper.getSearchType(position.zoom)) {
  //         searchType = AnyPlaceSeachingHelper.getSearchType(position.zoom);
  //         if (searchType == AnyPlaceSeachingHelper.SearchTypes.INDOOR_MODE) {
  //           searchView.setQueryHint("Search indoor");
  //           visiblePois.showAll();
  //           if (pathLineInside != null)
  //             pathLineInside.setVisible(true);
  //         } else if (searchType == AnyPlaceSeachingHelper.SearchTypes.OUTDOOR_MODE) {
  //           searchView.setQueryHint("Search outdoor");
  //           visiblePois.hideAll();
  //           if (pathLineInside != null)
  //             pathLineInside.setVisible(false);
  //         }
  //       }
  //
  //       bearing = position.bearing;
  //       mClusterManager.onCameraChange(position);
  //     }
  //   });
  //
  //   mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
  //
  //     @Override
  //     public boolean onMarkerClick(Marker marker) {
  //
  //       // mClusterManager returns true if is a cluster item
  //       if (!mClusterManager.onMarkerClick(marker)) {
  //
  //         PoisModel poi = visiblePois.getPoisModelFromMarker(marker);
  //         if (poi != null) {
  //           return false;
  //         } else {
  //           // Prevent Popup dialog
  //           return true;
  //         }
  //       } else {
  //         // Prevent Popup dialog
  //         return true;
  //       }
  //     }
  //   });
  //
  //   mClusterManager.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<BuildingModel>() {
  //
  //     @Override
  //     public boolean onClusterClick(Cluster<BuildingModel> cluster) {
  //       // Prevent Popup dialog
  //       return true;
  //     }
  //   });
  //
  //   mClusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<BuildingModel>() {
  //
  //     @Override
  //     public boolean onClusterItemClick(final BuildingModel b) {
  //       if (b != null) {
  //
  //         bypassSelectBuildingActivity(b, "0", false);
  //       }
  //       // Prevent Popup dialog
  //       return true;
  //     }
  //   });
  //
  // }
}