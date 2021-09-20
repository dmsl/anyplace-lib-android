/*
* AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
*
* Anyplace is a first-of-a-kind indoor information service offering GPS-less
* localization, navigation and search inside buildings using ordinary smartphones.
*
* Author(s): Timotheos Constambeys, Lambros Petrou
* 
* Supervisor: Demetrios Zeinalipour-Yazti
*
* URL: http://anyplace.cs.ucy.ac.cy
* Contact: anyplace@cs.ucy.ac.cy
*
* Copyright (c) 2015, Data Management Systems Lab (DMSL), University of Cyprus.
* All rights reserved.
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of
* this software and associated documentation files (the "Software"), to deal in the
* Software without restriction, including without limitation the rights to use, copy,
* modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
* and to permit persons to whom the Software is furnished to do so, subject to the
* following conditions:
*
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
* DEALINGS IN THE SOFTWARE.
*
*/

package cy.ac.ucy.cs.anyplace.lib.android.tasks;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import cy.ac.ucy.cs.anyplace.lib.Anyplace;
import cy.ac.ucy.cs.anyplace.lib.android.LOG;
import cy.ac.ucy.cs.anyplace.lib.android.nav.PoisNav;
import cy.ac.ucy.cs.anyplace.lib.android.utils.GeoPoint;

import static android.content.Context.MODE_PRIVATE;

@Deprecated
public class NavIndoorTask extends AsyncTask<Void, Void, String> {

	public interface NavRouteListener {
		void onNavRouteErrorOrCancel(String result);

		void onNavRouteSuccess(String result, List<PoisNav> points);
	}

	private NavRouteListener mListener;

	private Context mCtx;
	private String json_req;
	private List<PoisNav> mPuids = new ArrayList<PoisNav>();
	private boolean success = false;
	private String pois_to;
	private String lat;
	private String lon;
	private String flr;
	private String buid;


	public NavIndoorTask(NavRouteListener l, Context ctx, String poid, GeoPoint pos, String floor, String building) {
		this.mListener = l;
		this.mCtx = ctx;
		pois_to= poid;
		lat = pos.lat;
		lon =pos.lng;
		flr = floor;
		buid = building;

		// create the JSON object for the navigation API call
		JSONObject j = new JSONObject();
		try {
			j.put("username", "username");
			j.put("password", "pass");
			// insert the destination POI and the user's coordinates
			j.put("pois_to", poid);
			j.put("coordinates_lat", pos.lat);
			j.put("coordinates_lon", pos.lng);
			j.put("floor_number", floor);
			this.json_req = j.toString();

		} catch (JSONException e) {

		}
	}

	@Override
	protected String doInBackground(Void... params) {
		try {

			if (json_req == null)
				return "Error creating the request!";

			// changed to the coordnates function


          SharedPreferences pref = mCtx.getSharedPreferences("LoggerPreferences", MODE_PRIVATE);

          String host = pref.getString("server_ip_address", "ap.cs.ucy.ac.cy");
          String port = pref.getString("server_port", "443");

          Anyplace client = new Anyplace(host, port, mCtx.getCacheDir().getAbsolutePath());
          // Anyplace client = new Anyplace("ap.cs.ucy.ac.cy", "443", "");
          String access_token = pref.getString("server_access_token", "need an access token");
			// String response = NetworkUtils.downloadHttpClientJsonPost(AnyplaceAPI.getNavRouteXYUrl(mCtx), json_req);
			String response = client.navigationXY(access_token, pois_to,buid, flr, lat,lon);

			JSONObject json = new JSONObject(response);

			if (json.has("status") && json.getString("status").equalsIgnoreCase("error")) {
				return "Error Message: " + json.getString("message");
			}

			int num_of_pois = Integer.parseInt(json.getString("num_of_pois"));
			// If the list is empty it means that no navigation is possible
			if (0 == num_of_pois) {
				return "No valid path exists from your position to the POI selected!";
			}

			// convert the PUIDS received into NavPoints
			JSONArray pois = new JSONArray(json.getString("pois"));
			for (int i = 0; i < num_of_pois; i++) {
				JSONObject cp = (JSONObject) pois.get(i);
				PoisNav navp = new PoisNav();
				navp.lat = cp.getString("lat");
				navp.lon = cp.getString("lon");
				navp.puid = cp.getString("puid");
				navp.buid = cp.getString("buid");
				navp.floor_number = cp.getString("floor_number");
				mPuids.add(navp);
			}

			success = true;
			return "Successfully plotted navigation route!";

		}  catch (JSONException e) {
			return "Not valid response from the server! Contact the admin.";
		} catch (Exception e) {
			return "Error plotting navigation route. Exception[ " + e.getMessage() + " ]";
		}

	}

	@Override
	protected void onPostExecute(String result) {

		if (success) {
			// call the success listener
			mListener.onNavRouteSuccess(result, mPuids);
		} else {
			// call the error listener
			mListener.onNavRouteErrorOrCancel(result);
		}
	}

	@Override
	protected void onCancelled(String result) {

		mListener.onNavRouteErrorOrCancel("Navigation task cancelled!");
	}

	@Override
	protected void onCancelled() { // just for < API 11

		mListener.onNavRouteErrorOrCancel("Navigation task cancelled!");
	}
}// end of navroute task
