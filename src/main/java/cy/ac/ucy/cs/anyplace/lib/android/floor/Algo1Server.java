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

package cy.ac.ucy.cs.anyplace.lib.android.floor;

import org.json.JSONArray;
import org.json.JSONObject;


import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp;
import cy.ac.ucy.cs.anyplace.lib.android.LOG;
import cy.ac.ucy.cs.anyplace.lib.android.utils.network.OLDNetworkUtils;
import cy.ac.ucy.cs.anyplace.lib.core.LogRecord;

public class Algo1Server extends FloorSelector {
  private final String TAG = Algo1Server.class.getSimpleName();

  public Algo1Server(final AnyplaceApp app) {
    super(app);
  }
  private final static String PREDICT_FLOOR_ALGO1 = "/anyplace/position/predictFloorAlgo1"; // TODO app.api
  protected String calculateFloor(Args args) throws Exception {

    JSONObject request = new JSONObject();

    JSONObject f = new JSONObject();
    f.put("MAC", args.firstMac.getBssid());
    f.put("rss", args.firstMac.getRss());
    request.put("first", f);

    if (args.secondMac != null) {
      JSONObject s = new JSONObject();
      s.put("MAC", args.secondMac.getBssid());
      s.put("rss", args.secondMac.getRss());
      request.put("second", s);
    }

    JSONArray jsonArray = new JSONArray();
    for (LogRecord wifi : args.latestScanList) {
      JSONObject js = new JSONObject();
      js.put("MAC", wifi.getBssid());
      js.put("rss", wifi.getRss());
      jsonArray.put(js);
    }

    request.put("wifi", jsonArray);
    request.put("dlat", args.dlat);
    request.put("dlong", args.dlong);

    String response;

    if (OLDNetworkUtils.isOnline(app)) {
      LOG.E(TAG, "TODO: implement this through the app.api");
      // SharedPreferences pref = app.getSharedPreferences("LoggerPreferences", MODE_PRIVATE); // CLR
      // String host = pref.getString("server_ip_address", "ap.cs.ucy.ac.cy");
      response = OLDNetworkUtils
          .downloadHttpClientJsonPost(app.prefs.getIp() + PREDICT_FLOOR_ALGO1, request.toString());
      JSONObject json = new JSONObject(response);

      if (json.getString("status").equalsIgnoreCase("error")) {
        throw new Exception("ERROR: " + json.getString("message"));
      }
      return json.getString("floor");
    } else {
      return "";
    }
  }
}
