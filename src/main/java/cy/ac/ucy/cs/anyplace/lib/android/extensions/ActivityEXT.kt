package cy.ac.ucy.cs.anyplace.lib.android.extensions

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.AndroidViewModel
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.data.datastore.*

// CLR:PM
// fun AppCompatActivity.getViewModelFactory(): ViewModelFactory {
//     return ViewModelFactory(this)
// }

fun ComponentActivity.registerForActivityResult(
        callback: ActivityResultCallback<ActivityResult>
): ActivityResultLauncher<Intent> {
  return this.registerForActivityResult(
          ActivityResultContracts.StartActivityForResult(),
          callback
  )
}

// EXTENSION FUNCTIONS
val Activity.app: AnyplaceApp get() = this.application as AnyplaceApp
val Activity.dataStoreServer: DataStoreServer get() = this.app.dataStoreServer
val Activity.dataStoreCvLogger: DataStoreCvLogger get() = this.app.dataStoreCvLogger
val Activity.dataStoreCv: DataStoreCv get() = this.app.dataStoreCv
val Activity.dataStoreMisc: DataStoreMisc get() = this.app.dataStoreMisc
val Activity.dataStoreUser: DataStoreUser get() = this.app.dataStoreUser

val DialogFragment.app: AnyplaceApp get() = this.activity?.application as AnyplaceApp
val AndroidViewModel.app: AnyplaceApp get() = getApplication<AnyplaceApp>()

val TAG_ANYPLACE = "anyplace"

// Extending Any (Java Object): name convention for loggin: ap_<className>
// val Any.TAG : String get() = "ap_${this::class.java.simpleName}"
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
        simpleClassName == "ActivityExtensionsKt" -> { }
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
 * NOTE: Might be better not to use it inside 'collect' (Kotlin's Flows)
 * object{}.javaClass.enclosingMethod?.name.toString()
 * frame 0: getThreadStackTrace
 * frame 1: getStackTrace
 * frame 2: getMETHOD
 * frame 3: getTAG_METHOD (some times.. depends where it is called from
 */
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