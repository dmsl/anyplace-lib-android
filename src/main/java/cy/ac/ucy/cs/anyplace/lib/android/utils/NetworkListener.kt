package cy.ac.ucy.cs.anyplace.lib.android.utils
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull

/**
 * Asynchronously updating internet connectivity.
 *
 * Could be used to provide an internet connectivity button to the app
 */
class NetworkListener: ConnectivityManager.NetworkCallback() {
  private val isNetworkAvailable = MutableStateFlow(false)

  fun checkNetworkAvailability(ctx: Context) : MutableStateFlow<Boolean> {
    val connectivityManager =
      ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    connectivityManager.registerDefaultNetworkCallback(this)
    var isConnected = false
    connectivityManager.allNetworks.forEach { network ->
      val networkCapability = connectivityManager.getNetworkCapabilities(network)
      networkCapability?.let {
        if (it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
          isConnected = true
          return@forEach
        }
      }
    }
    isNetworkAvailable.value = isConnected
    return isNetworkAvailable
  }

  override fun onAvailable(network: Network) {
    isNetworkAvailable.value = true
  }

  override fun onLost(network: Network) {
    isNetworkAvailable.value = false
  }
}
