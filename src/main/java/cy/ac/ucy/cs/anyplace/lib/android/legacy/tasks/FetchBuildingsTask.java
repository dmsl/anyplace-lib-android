/*
 * Anyplace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Timotheos Constambeys
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

package cy.ac.ucy.cs.anyplace.lib.android.legacy.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;


import com.google.android.gms.maps.model.LatLng;

import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp;
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG;
import cy.ac.ucy.cs.anyplace.lib.android.legacy.nav.BuildingModel;
import cy.ac.ucy.cs.anyplace.lib.android.utils.net.OLDNetworkUtils;

@Deprecated
public class FetchBuildingsTask extends AsyncTask<Void, Void, String> {
  private static final String TAG = FetchNearBuildingsTask.class.getSimpleName();

  public interface FetchBuildingsTaskListener {
    void onErrorOrCancel(String result);
    void onSuccess(String result, List<BuildingModel> buildings);
  }

  private final boolean forceReload;
  private final FetchBuildingsTaskListener listener;
  private final Activity activity;
  private final AnyplaceApp app;

  private final List<BuildingModel> buildings = new ArrayList<BuildingModel>();
  private boolean success = false;
  private ProgressDialog dialog;
  private final Boolean showDialog = true;

  public FetchBuildingsTask(Activity activity,
                            FetchBuildingsTaskListener listener,
                            boolean forceReload) {
    this.activity=activity;
    this.app = (AnyplaceApp) activity.getApplication();
    this.listener = listener;
    this.forceReload=forceReload;
  }

  // CLR:PM
  // public FetchBuildingsTask(FetchBuildingsTaskListener fetchBuildingsTaskListener, Context ctx,
  //                           boolean forceReload, boolean showDialog) {
  //   this.mListener = fetchBuildingsTaskListener;
  //   this.ctx = ctx;
  //   this.showDialog = showDialog;
  //   this.forceReload=forceReload;
  //   this.prefs = new Preferences(ctx);
  //   this.fileCache = new FileCache(prefs);
  // }

  @Override
  protected void onPreExecute() {
    if (showDialog) {
      dialog = new ProgressDialog(activity);
      dialog.setIndeterminate(true);
      dialog.setTitle("Fetching Buildings");
      dialog.setMessage("Please be patient...");
      dialog.setCancelable(true);
      dialog.setCanceledOnTouchOutside(false);
      dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
        @Override
        public void onCancel(DialogInterface dialog) {
          FetchBuildingsTask.this.cancel(true);
        }
      });
      dialog.show();
    }
  }

  @Override
  protected String doInBackground(Void... params) {
    if (!OLDNetworkUtils.isOnline(app)) { return "ERROR: FetchBuildings: No network connection!"; }

    try {
      // TODO:PM make this in a modern way: kotlin coroutines + something else...
      // TODO:PM get in a single file...
      // TODO:PM initialize this in App class

      app.fileCache.initDirs(); // TODO this in App class
      // TODO also delete caches (in other places of the app..)

      if(forceReload) {
        LOG.E(TAG, "forceReload: deleting file-cache");
        app.fileCache.deleteBuildingsAll();
      }

      JSONObject json;
      if(app.fileCache.hasBuildingsAll()) {
        LOG.D2(TAG, "Fetch buildings: using file-cache");
        json = app.fileCache.readBuildingsAll();
      } else {
        LOG.D2(TAG, "Fetch buildings: downloading..");

        // Anyplace anyplace = new Anyplace(prefs.getIp(), prefs.getPort(), prefs.getCacheDir());
        String response =  app.apiOld.buildingAll(); // CHECK:PM get only nearby buildings..
        if(!app.fileCache.storeBuildingsAll(response)) {
          LOG.E(TAG, "ERROR: Failed to store buildings in file cache.");
        }
        json = new JSONObject(response);
      }

      // Missing in Zip Format
      if (json.has("status") && json.getString("status").equalsIgnoreCase("1")) {
        return "Error Message: " + json.getString("message");
      }
      success = parseBuildings(json);
      return "Successfully fetched buildings";
    }  catch (Exception e) {
      return "ERROR: fetching buildings: " + e.getMessage();
    }
  }

  private boolean parseBuildings(JSONObject json) {
    try {
      // parse the buildings received
      BuildingModel b;
      JSONArray buids = new JSONArray(json.getString("buildings"));
      for (int i = 0, sz = buids.length(); i < sz; i++) {
        JSONObject cp = (JSONObject) buids.get(i);

        double lat = Double.parseDouble(cp.getString("coordinates_lat"));
        double lon = Double.parseDouble(cp.getString("coordinates_lon"));
        String buid= cp.getString("buid");
        String name= cp.getString("name");
        String description= cp.getString("description");
        LatLng latLng = new LatLng(lat, lon);
        b = new BuildingModel(latLng, name, description, buid);
        // b.address = cp.getString("address"); CHECK
        // b.description = cp.getString("description"); CHECK
        // b.url = cp.getString("url");

        buildings.add(b); // the anyplace Cache list
      }
      Collections.sort(buildings);
      return true;
    } catch (JSONException e) {
      LOG.E(TAG, "ERROR: failed while parsing buildings.");
      app.fileCache.deleteBuildingsAll();
      e.printStackTrace();
      buildings.clear();
      return false;
    }
  }

  @Override
  protected void onPostExecute(String result) {
    if (showDialog) dialog.dismiss();

    if (success) {
      listener.onSuccess(result, buildings);
    } else {  // there was an error during the process
      listener.onErrorOrCancel(result);
    }
  }

  @Override
  protected void onCancelled(String result) {
    if (showDialog)
      dialog.dismiss();
    listener.onErrorOrCancel("Buildings Fetch cancelled.");
  }

  @Override
  protected void onCancelled() { // just for < API 11
    if (showDialog) dialog.dismiss();
    listener.onErrorOrCancel("Buildings Fetch cancelled.");
  }
}