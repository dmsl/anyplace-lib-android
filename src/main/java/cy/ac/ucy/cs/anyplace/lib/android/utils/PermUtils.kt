package cy.ac.ucy.cs.anyplace.lib.android.utils

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.LOG.Companion.TAG


/** Permission utilities */
class PermUtils {

  companion object {

    fun checkLoggerPermissionsAndSettings(ctx: Context) {
      checkLocationService(ctx)
      checkLocationFineGrain(ctx)
    }

    private fun checkLocationService(ctx: Context) {
      if(!hasLocationService(ctx)) {
        LOG.E(TAG, "LocationService is disabled!")
        AlertDialog.Builder(ctx)
                .setTitle("Location is disabled!")
                .setMessage(ctx.getString(R.string.app_name)
                        + " needs the Location to be enabled.\nYou will be redirected to the OS Settings.")
                .setPositiveButton("Accept") { dialogInterface, i ->
                  ctx.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .create().show()
      }
    }

    private fun checkLocationFineGrain(ctx: Context){
      LOG.D4("checkLocationFineGrain()")
      if (!hasLocationFineGrain(ctx)) {
        LOG.E(TAG, "Need fine-grain location!")
        requestLocationFine(ctx)
      }
    }

    private fun requestLocationFine(ctx: Context) {
      requestPermission(
              ctx,
              Manifest.permission.ACCESS_FINE_LOCATION,
              "Location required",
              ctx.getString(R.string.app_name) +
                      " needs the fine-grain location permission to operate.\n"+
                      "It is used to get coarse/fine grain location when this is available," +
                      "to focus the app on the relevant map areas.",
              REQ_LOC_FINE_GRAIN
      )
    }

    // LEFTHERE FILTER
    // ^(?!.*(Adreno|artvv|libEGL|chatty|DynamiteModule))

    private fun requestPermission(ctx: Context, perm: String, title: String, msg: String, code: Int) {
      AlertDialog.Builder(ctx)
              .setTitle(title).setMessage(msg)
              .setPositiveButton("Okay") { dialogInterface, i ->
                ActivityCompat.requestPermissions(ctx as Activity, arrayOf(perm), code)
              }
              .create().show()
    }

    /** Location in Android OS settings */
    private fun hasLocationService(ctx: Context) : Boolean {
      val locationManager = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
      return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    /** Fine-Grain Location in anyplace apps */
    private fun hasLocationFineGrain(ctx: Context) : Boolean {
      return (ActivityCompat.checkSelfPermission(ctx,
              Manifest.permission.ACCESS_FINE_LOCATION)
              == PackageManager.PERMISSION_GRANTED)
    }




    //////////////////
    var requesting: Boolean = false
    val REQ_LOC_FINE_GRAIN: Int = 1  // MY_PERMISSIONS_REQUEST_LOCATION CLR in Unified
    val REQ_LOC_COARSE_GRAIN: Int = 2
    val REQ_LOC_BG: Int = 3

    fun hasPermission(ctx: Context, perm: String) =
            (ActivityCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED)


    // fun showAlertLocation(ctx: Activity) {
    //   AlertDialog.Builder(ctx)
    //           .setTitle(title)
    //           .setMessage(msg)
    //           .setPositiveButton("OK") { dialogInterface, i ->
    //             // Prompt the user once explanation has been shown
    //             // ActivityCompat.requestPermissions(
    //             //         ctx, arrayOf(perm), MY_PERMISSIONS_REQUEST_LOCATION)
    //
    //             ActivityCompat.requestPermissions(ctx, arrayOf(perm), REQ_CODE)
    //
    //           }
    //           .create().show()
    //
    //   // val dialog = AlertDialog.Builder(ctx)
    //   // dialog.setMessage("Your location settings is set to Off, Please enable location to use this application")
    //   // dialog.setPositiveButton("Settings") { _, _ ->
    //   //   val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    //   //   ctx.startActivity(myIntent)
    //   // }
    //   // dialog.setNegativeButton("Cancel") { _, _ ->
    //   //   ctx.finish()
    //   // }
    //   // dialog.setCancelable(false)
    //   // dialog.show()
    // }




    fun FF_requestPermission(ctx: Activity, perm: String, title: String, msg: String, reqCode: Int) {

          LOG.D("Requesting permissions: with rationale")

          // Show an explanation to the user *asynchronously* -- don't block
          // this thread waiting for the user's response! After the user
          // sees the explanation, try again to request the permission.
          AlertDialog.Builder(ctx)
                  .setTitle(title)
                  .setMessage(msg)
                  .setPositiveButton("OK") { dialogInterface, i ->
                    // Prompt the user once explanation has been shown
                    // ActivityCompat.requestPermissions(
                    //         ctx, arrayOf(perm), MY_PERMISSIONS_REQUEST_LOCATION)

                    ActivityCompat.requestPermissions(ctx, arrayOf(perm), reqCode)

                  }
                  .create().show()
    }


    fun requestPermission(ctx: Context, perm: String, title: String, msg: String) {
      LOG.D("requestPermission: $perm: has:${hasPermission(ctx, perm)}")
      // Manifest.permission.ACCESS_FINE_LOCATION
      if (!hasPermission(ctx, perm)) {
        LOG.D("PERMISSION NOT GRANTED: $perm")

        // Should we show an explanation?
        if (ActivityCompat.shouldShowRequestPermissionRationale(ctx as Activity, perm)) {
          LOG.D("Requesting permissions: with rationale")

          // Show an explanation to the user *asynchronously* -- don't block
          // this thread waiting for the user's response! After the user
          // sees the explanation, try again to request the permission.
          AlertDialog.Builder(ctx)
                  .setTitle(title)
                  .setMessage(msg)
                  .setPositiveButton("OK") { dialogInterface, i ->
                    // Prompt the user once explanation has been shown
                    // ActivityCompat.requestPermissions(
                    //         ctx, arrayOf(perm), MY_PERMISSIONS_REQUEST_LOCATION)

                    ActivityCompat.requestPermissions(ctx, arrayOf(perm), REQ_LOC_FINE_GRAIN)

                  }
                  .create().show()
        } else {
          LOG.D("Requesting permissions: without rationale!")
          // No explanation needed, we can request the permission.
          ActivityCompat.requestPermissions(ctx, arrayOf(perm),  REQ_LOC_FINE_GRAIN)
        }
      } else {
        LOG.D("Permission existed: $perm")
      }
      // if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
      //         != PackageManager.PERMISSION_GRANTED) {
      //
      //   // Should we show an explanation?
      //   if (ActivityCompat.shouldShowRequestPermissionRationale(this,
      //                   Manifest.permission.ACCESS_COARSE_LOCATION)) {
      //
      //     // Show an explanation to the user *asynchronously* -- don't block
      //     // this thread waiting for the user's response! After the user
      //     // sees the explanation, try again to request the permission.
      //     AlertDialog.Builder(this)
      //             .setTitle("Location Permission Needed")
      //             .setMessage("This app needs the Location permission, please accept to use location functionality")
      //             .setPositiveButton("OK") { dialogInterface, i -> //Prompt the user once explanation has been shown
      //               ActivityCompat.requestPermissions(this@AnyplaceLoggerActivity, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
      //                       MY_PERMISSIONS_REQUEST_LOCATION)
      //             }
      //             .create()
      //             .show()
      //   } else {
      //     // No explanation needed, we can request the permission.
      //     ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
      //             MY_PERMISSIONS_REQUEST_LOCATION)
      //   }
      // }
    }

  }

}