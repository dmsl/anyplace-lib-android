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

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import cy.ac.ucy.cs.anyplace.lib.legacy.Anyplace;
import cy.ac.ucy.cs.anyplace.lib.android.consts.MSG;
import cy.ac.ucy.cs.anyplace.lib.android.nav.PoisModel;
import cy.ac.ucy.cs.anyplace.lib.android.utils.net.OLDNetworkUtils;

import static android.content.Context.MODE_PRIVATE;

/**
 * TODO:PM Coroutines/new Async
 * Fetch the POIs of a Building's Floor
 */
@Deprecated
public class FetchPoiByPuidTask extends AsyncTask<Void, Void, String> {

	public interface FetchPoiListener {
		void onErrorOrCancel(String result);

		void onSuccess(String result, PoisModel poi);
	}

	private FetchPoiListener mListener;
	private Context mCtx;

	private String puid;
	private PoisModel poi;

	private ProgressDialog dialog;
	private boolean success = false;
	private String access_token;


	public FetchPoiByPuidTask(FetchPoiListener l, Context ctx, String puid, String token) {
		this.mCtx = ctx;
		this.mListener = l;
		this.puid = puid;
		access_token= token;
	}

	@Override
	protected void onPreExecute() {
		dialog = new ProgressDialog(mCtx);
		dialog.setIndeterminate(true);
		dialog.setTitle("Fetching POI Info");
		dialog.setMessage("Please be patient...");
		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				FetchPoiByPuidTask.this.cancel(true);
			}
		});
		dialog.show();
	}

	@Override
	protected String doInBackground(Void... params) {
		if (!OLDNetworkUtils.isOnline(mCtx)) {
			return MSG.INSTANCE.getWARN_NO_NETWORK();
		}

		try {
			JSONObject j = new JSONObject();
			try {
				j.put("username", "username");
				j.put("password", "pass");

				j.put("pois", this.puid);
			} catch (JSONException e) {
				return "Error Message: Could not create the request for the POIs!";
			}

			// fetch the pois of this floor
          SharedPreferences pref = mCtx.getSharedPreferences("LoggerPreferences", MODE_PRIVATE);

          String host = pref.getString("server_ip_address", "ap.cs.ucy.ac.cy");
          String port = pref.getString("server_port", "443");

          Anyplace client = new Anyplace(host, port, mCtx.getCacheDir().getAbsolutePath());

          String access_token = pref.getString("server_access_token", "need an access token");

          String response = client.poiDetails(access_token, puid) ;
          // Anyplace client = new Anyplace("ap.cs.ucy.ac.cy", "443", "");
			// String response = NetworkUtils.downloadHttpClientJsonPost(AnyplaceAPI.getFetchPoisByPuidUrl(mCtx), j.toString());

			// fetch the pois for the whole building
			// String response = NetworkUtils.downloadHttpClientJsonPost(
			// AnyplaceAPI.getFetchPoisByBuidUrl(),j.toString());

			JSONObject json = new JSONObject(response);
			if (json.has("status") && json.getString("status").equalsIgnoreCase("error")) {
				return "Error Message: " + json.getString("message");
			}

			// process the buildings received
			poi = new PoisModel();
			poi.lat = json.getString("coordinates_lat");
			poi.lng = json.getString("coordinates_lon");
			poi.buid = json.getString("buid");
			poi.floor_name = json.getString("floor_name");
			poi.floor_number = json.getString("floor_number");
			poi.description = json.getString("description");
			poi.name = json.getString("name");
			poi.pois_type = json.getString("pois_type");
			poi.puid = json.getString("puid");
			poi.is_building_entrance = json.getBoolean("is_building_entrance");

			success = true;
			return "Successfully fetched Points of Interest";

		}  catch (JSONException e) {
			return "Not valid response from the server! Contact the admin.";
		} catch (Exception e) {
			return "Error fetching Point of Interest. Exception[ " + e.getMessage() + " ]";
		}
	}

	@Override
	protected void onPostExecute(String result) {
		dialog.dismiss();

		if (success) {
			mListener.onSuccess(result, poi);
		} else {
			mListener.onErrorOrCancel(result);
		}
	}

	@Override
	protected void onCancelled(String result) {
		dialog.dismiss();
		mListener.onErrorOrCancel(result);
	}

	@Override
	protected void onCancelled() {
		dialog.dismiss();
		mListener.onErrorOrCancel("Fetching POI was cancelled!");
	}

} // end of fetch POIS by building and floor

