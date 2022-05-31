// DEPRECATE

/*
 * Anyplace: A free and open Indoor Navigation Service with superb accuracy!
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

package cy.ac.ucy.cs.anyplace.lib.android.legacy.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp;
import cy.ac.ucy.cs.anyplace.lib.android.utils.DBG;
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG;
import cy.ac.ucy.cs.anyplace.lib.android.legacy.nav.BuildingModel;
import cy.ac.ucy.cs.anyplace.lib.android.legacy.nav.PoisModel;
import cy.ac.ucy.cs.anyplace.lib.android.legacy.tasks.FetchBuildingsTask;
import cy.ac.ucy.cs.anyplace.lib.android.utils.net.OLDNetworkUtils;


/**
 * TODO:PM deprecate and eventually remove this.
 * This should become a BaseModel from ViewModel
 *
 * Share cache between different apps?
 * https://medium.com/androidsrc/share-cache-files-with-other-android-apps-using-fileprovider-897fe5705e45
 *
 * This class should provide the last fetched Buildings, Floors and POIs. At the
 * moment it's just static data structures but they should be implemented as a
 * local database and being retrieved as a ContentProvider.
 */
@SuppressWarnings("serial")
@Deprecated()
public class ObjectCache implements Serializable {
  private static final String TAG = "obj_cache";
  private static ObjectCache mInstance = null;

  private transient BackgroundFetch bf = null;

  private int selectedBuilding = 0;
  // last fetched Buildings
  private List<BuildingModel> mSpinnerBuildings; //= new ArrayList<>(0);
  private List<BuildingModel> buildingsAll;// = new ArrayList<>(0);

  // last fetched pois
  private Map<String, PoisModel> mLoadedPoisMap;
  private String poisBUID;

  private AnyplaceApp app;

  public static ObjectCache getInstance(AnyplaceApp app) {
    if (mInstance == null) {
      synchronized (app) {
        if (mInstance == null) { mInstance = getObject(app); }
        if (mInstance == null) {
          mInstance = new ObjectCache();
          mInstance.app = app;
          LOG.D(TAG, "Initialized cache: " + GetCacheFile(app).getAbsolutePath());
        }
      }
    }
    return mInstance;
  }

  public static void saveInstance(AnyplaceApp app) { saveObject(app); }

  private ObjectCache() {
    this.mSpinnerBuildings = new ArrayList<>(); // last fetched Buildings
    this.mLoadedPoisMap = new HashMap<>(); // last fetched pois
    this.buildingsAll = new ArrayList<>();
  }

  // All Buildings
  public List<BuildingModel> loadWorldBuildings(
          final Activity activity,
          final FetchBuildingsTask.FetchBuildingsTaskListener listener,
          Boolean forceReload) {
    if ((forceReload && OLDNetworkUtils.isOnline(activity)) || buildingsAll.isEmpty()) {
      new FetchBuildingsTask(activity, new FetchBuildingsTask.FetchBuildingsTaskListener() {

        @Override
        public void onSuccess(String result, List<BuildingModel> buildings) {
          buildingsAll = buildings;
          LOG.D2("loadWorldBuildings: file-cached");
          // ObjectCache.saveInstance(ctx); // CLR:PM
          // this will load the buildings on the map
          listener.onSuccess(result, buildings);
        }

        @Override
        public void onErrorOrCancel(String result) {
          listener.onErrorOrCancel(result);
        }
      }, forceReload).execute();
    } else {
      // these reside in memory now...
      LOG.D2("loadWorldBuildings: read from live-cache");
      listener.onSuccess("Successfully read from cache", buildingsAll);
    }
    return buildingsAll;
  }

  public void loadBuilding(Activity activity, final String buid, final BuildingModel.Callback l) {

    loadWorldBuildings(activity, new FetchBuildingsTask.FetchBuildingsTaskListener() {
      @Override
      public void onSuccess(String result, List<BuildingModel> buildings) {
        BuildingModel fcb = null;
        for (BuildingModel b : buildings) {
          if (b.buid.equals(buid)) {
            fcb = b;
            break;
          }
        }

        if (fcb != null) {
          l.onSuccess("Success", fcb);
        } else {
          l.onErrorOrCancel("Building not found");
        }
      }

      @Override
      public void onErrorOrCancel(String result) {
        l.onErrorOrCancel(result);
      }
    }, false);
  }

  // Buildings Spinner in Select Building Activity
  public List<BuildingModel> getSpinnerBuildings() {
    return mSpinnerBuildings;
  }

  public void setSpinnerBuildings(AnyplaceApp app, List<BuildingModel> mLoadedBuildings) {
    this.mSpinnerBuildings = mLoadedBuildings;
    // LOG.D(TAG, "ObjectCache: saving spinner buildings");
    // ObjectCache.saveInstance(app);
  }

  // Use nav/AnyUserData for the loaded building in Navigator
  public BuildingModel getSelectedBuilding() {
    BuildingModel b = null;
    try {
      b = mSpinnerBuildings.get(selectedBuilding);
    } catch (IndexOutOfBoundsException e) {
      LOG.E("getSelectedBuilding: out of bounds: " + e.getMessage());
    }
    return b;
  }

  public int getSelectedBuildingIndex() {
    if (!(selectedBuilding < mSpinnerBuildings.size()))
      selectedBuilding = 0;

    return selectedBuilding;
  }

  public void setSelectedBuildingIndex(int selectedBuildingIndex) {
    this.selectedBuilding = selectedBuildingIndex;
  }

  // POIS
  public Collection<PoisModel> getPois() {
    return this.mLoadedPoisMap.values();
  }

  public Map<String, PoisModel> getPoisMap() {
    return this.mLoadedPoisMap;
  }

  public void setPois(AnyplaceApp app, Map<String, PoisModel> lpID, String poisBUID) {
    this.mLoadedPoisMap = lpID;
    this.poisBUID = poisBUID;
    LOG.E(TAG, "ObjectCache: saving pois");
    ObjectCache.saveInstance(app);
  }

  // Check the loaded pois if match the Building ID
  public boolean checkPoisBUID(String poisBUID) {
    if (this.poisBUID != null && this.poisBUID.equals(poisBUID))
      return true;
    else
      return false;
  }


  // Fetch all Floor and Radiomaps of the current building
  public void fetchAllFloorsRadiomapsRun(Activity activity, BackgroundFetchListener l, final BuildingModel build) {
    if (bf == null) {
      l.onPrepareLongExecute();
      bf = new BackgroundFetch(activity, l, build);
      bf.run();
    } else if (!bf.getBuilding().buid.equals(build.buid)) {
      // Navigated to another building
      bf.cancel();
      l.onPrepareLongExecute();
      bf = new BackgroundFetch(activity, l, build);
      bf.run();
    } else if (bf.status == BackgroundFetchListener.Status.SUCCESS) {
      // Previously finished for the current building
      l.onSuccess("Already Downloaded");
    } else if (bf.status == BackgroundFetchListener.Status.STOPPED) {
      // Task Download Error Occurred
      l.onErrorOrCancel("Task Failed", BackgroundFetchListener.ErrorType.EXCEPTION);
    } else {
      l.onErrorOrCancel("Another instance is running", BackgroundFetchListener.ErrorType.SINGLE_INSTANCE);
    }
  }

  public void fetchAllFloorsRadiomapReset() {
    if (bf != null)
      bf = null;
  }

  public BackgroundFetchListener.Status fetchAllFloorsRadiomapStatus() {
    return bf.status;
  }


  // SAVE CACHE
  public static boolean saveObject(AnyplaceApp app) {  // , ctx.getCacheDir(), getInstance(ctx)
    // saveObject(ctx, ctx.getCacheDir(), getInstance(ctx));
    final File fcache = GetCacheFile(app);
    ObjectCache obj = getInstance(app);

    FileOutputStream fos;
    ObjectOutputStream oos;
    // boolean keep = true;

    try {
      fos = new FileOutputStream(fcache);
      oos = new ObjectOutputStream(fos);
      oos.writeObject(obj);
      oos.close();
      fos.close();
      return true;
    } catch (Exception e) {
      String msg = "ERROR: saveObject:" + e.getClass() + ": cause: " + e.getCause() + ": " + e.getMessage();
      LOG.E(TAG, msg);
      // e.printStackTrace();
      if (DBG.D1) Toast.makeText(app, msg, Toast.LENGTH_LONG).show();
      fcache.delete();
      return false;
    }// finally {
    // try {
    // if (oos != null)
    //   oos.close();
    // if (fos != null)
    //   fos.close();
    // if (keep == false) {
    //   LOG.E(TAG, "DELETING CACHE DIR: WHY?");
    //   cache.dir.delete();
    // }
    // } catch (Exception e) { /* do nothing */
    // }
    //}
    //
    // return keep;
  }

  private static File GetCacheFile(Context ctx) {
    return new File(ctx.getCacheDir(), TAG);
  }

  private static ObjectCache getObject(Context ctx) {
    File fcache = GetCacheFile(ctx);
    if(!fcache.exists()) return null;

    // CLR
    // final File suspend_f = new File(cacheDir, "ap_cache");
    // LOG.D("getObject: suspend_f: " + suspend_f);
    FileInputStream fis;
    ObjectInputStream is;
    ObjectCache cache;
    try {

      fis = new FileInputStream(fcache);
      is = new ObjectInputStream(fis);
      cache = (ObjectCache) is.readObject();
      return cache;

      // } catch (Exception e) {
      // 	LOG.E("ERROR: ap-cache: getObject :" + e.getMessage());
      // 	// if (AnyplaceDebug.DEBUG_MESSAGES) CLR
      // } finally {
      // 	try {
      // 		if (fis != null)
      // 			fis.close();
      // 		if (is != null)
      // 			is.close();
      // }
      // return new AnyplaceCache(ctx, fcache);

    } catch (IOException | ClassNotFoundException e) {
      LOG.E(TAG, "Failed to initialize: " + e.getMessage());
      Toast.makeText(ctx, "Failed to initialize cache!", Toast.LENGTH_LONG).show();
    }

    return null;
  }

}
