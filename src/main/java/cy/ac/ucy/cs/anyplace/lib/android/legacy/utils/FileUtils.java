package cy.ac.ucy.cs.anyplace.lib.android.legacy.utils;

import android.content.Context;

public class FileUtils {

  public static String getExternalDir(Context ctx) {
    return ctx.getExternalFilesDir(null).getAbsolutePath();
  }

  public static String getExternalCache(Context ctx) {
    return ctx.getExternalCacheDir().getAbsolutePath();
  }
}
