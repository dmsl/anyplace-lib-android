package cy.ac.ucy.cs.anyplace.lib.android.extensions

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceFragmentCompat
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.NavigatorAppBase
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.*
import cy.ac.ucy.cs.anyplace.lib.android.utils.UtilSnackBarNotifier

fun ComponentActivity.registerForActivityResult(
        callback: ActivityResultCallback<ActivityResult>):ActivityResultLauncher<Intent> {
  return this.registerForActivityResult(
          ActivityResultContracts.StartActivityForResult(), callback)
}

/** Snackbar for notifications (from [AnyplaceApp.notify]) */
val DialogFragment.app: AnyplaceApp get() = requireActivity().application as AnyplaceApp
val DialogFragment.notify: UtilSnackBarNotifier get() = app.notify

/** Snackbar for notifications (from [AnyplaceApp.notify]) */
val AndroidViewModel.app: AnyplaceApp get() = getApplication<AnyplaceApp>()
val PreferenceFragmentCompat.app: AnyplaceApp get() = this.requireActivity().app

// NAVIGATOR APP: can be [NavigatorApp] or [SmasApp]
val PreferenceFragmentCompat.appSmas: NavigatorAppBase get() = this.requireActivity().appSmas
val Activity.appSmas: NavigatorAppBase get() = this.application as NavigatorAppBase
val AndroidViewModel.appSmas: NavigatorAppBase get() = getApplication<NavigatorAppBase>()

val Activity.dsAnyplace: AnyplaceDataStore get() = this.app.dsAnyplace
val Activity.dsCv: CvDataStore get() = this.app.dsCv
val Activity.dsCvMap: CvMapDataStore get() = this.app.dsCvMap
val Activity.dsMisc: MiscDS get() = this.app.dsMisc
val Activity.dsUserAP: ApUserDataStore get() = this.app.dsUserAP

val Activity.app: AnyplaceApp get() = this.application as AnyplaceApp
/** Snackbar for notifications (from [AnyplaceApp.notify]) */
val Activity.notify: UtilSnackBarNotifier get() = app.notify

val AndroidViewModel.notify: UtilSnackBarNotifier get() = app.notify

const val TAG_ANYPLACE = "anyplace"

/** NOT DO NOT USE IT. See [TG] variables in several places instead
 *
 * Extending Any (Java Object): name convention for loggin: ap_<className>
 * This does not always work (especially when wrapped in lambdas).
 * It goes through the stack frame and finds the name of the class that it is being used
 *
 * Simpler version:
 * val Any.TAG : String get() = "ap_${this::class.java.simpleName}"
*/
@Deprecated("dont use. See LOGGING.md")
val Any.TAG: String get()  {
  var i=2
  while (true) {
    try {

      val frame = Thread.currentThread().stackTrace[i]
      val methodName = frame.methodName
      val className = frame.className
      val simpleClassName = frame.className.substringAfterLast(".", "<class>")
      when  {
        className == null -> {return "<null-class>"}
        methodName == "getTAG" -> {}
        // anonymous classes end with a number
        // example: package.$internalClass.$5
        className.substringAfterLast("\$", "").toIntOrNull() != null -> {}

        // internal classes
        className.endsWith("LOG\$Companion") ||
                className.startsWith("kotlin") -> { }
        // this kotlin file
        simpleClassName == "ActivityEXT" -> { }
        else -> { return simpleClassName
        }
      }
      i++
    } catch (e: Exception) {
      return TAG_ANYPLACE
    }
  }
}


/**
 * Extending Any (Java Object): name convention for logging: ap_<className>
 * This does not always work (especially when wrapped in lambdas).
 * It goes through the stack frame and finds the name of thelass method that it is being used.
 * (something equivalent to the __func__ that the C++ compiler provides)
 *
 *
 * object{}.javaClass.enclosingMethod?.name.toString()
 * frame 0: getThreadStackTrace
 * frame 1: getStackTrace
 * frame 2: getMETHOD
 * frame 3: getTAG_METHOD (some times.. depends where it is called from
 *
 * TODO: use val MT = ::theMethodsName.name
 */
@Deprecated("dont use. See LOGGING.md")
val Any.METHOD: String get()  {
  try {
    var i=2
    while (true) {
      val frame = Thread.currentThread().stackTrace[i]
      val methodName = frame.methodName
      val className = frame.className
      // className.
      when  {
        methodName == null -> { return "<null-method>" }
        methodName == "getMETHOD" ||
                methodName == "getTAG_METHOD" -> {}

        // anonymous classes end with a number
        // example: package.$internalClass.$5
        className.substringAfterLast("\$", "").toIntOrNull() != null -> {}

        // internal classes
        className.endsWith("LOG\$Companion") ||
                className.endsWith("EXTKt") ||
                className.startsWith("kotlin") -> { } // Log.w("anyplace", "IGN: $methodName KT")
        else -> {
          // remove any lambda method endings (e.g., methodName$lambda-1)
          return methodName.substringBeforeLast("$")
        }
      }
      i++
    }
  } catch (e: Exception) {
    return "<method>"
  }
}

val Any.TAG_METHOD: String get() = "$TAG/$METHOD"
