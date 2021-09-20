package cy.ac.ucy.cs.anyplace.lib.android.sensors.thermal

import android.content.Context
import com.flir.thermalsdk.ErrorCode
import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid
import com.flir.thermalsdk.live.CommunicationInterface
import com.flir.thermalsdk.live.Identity
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener
import com.flir.thermalsdk.live.discovery.DiscoveryFactory
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG

class FlirUtils {
  companion object {
    fun initialize(ctx: Context) {
      ThermalSdkAndroid.init(ctx)
      //^(?!.*(Accessing hidden method|[socket][connection]))
      //cy.ac.ucy.ac.anyplace*
      LOG.D(TAG, "Version: " + ThermalSdkAndroid.getVersion())
    }

    fun isFlirOne() {
      // val handler: UsbPermissionHandler()
      // handler.requestFlirOnePermisson()
    }
  }

  class FlirDiscovery: DiscoveryEventListener {

    // file:///Users/paschalis/src/dmsl/anyplace/git.clients/clients/android-new/lib/libs/javadoc/index.html
    fun startDiscovery() {
      DiscoveryFactory.getInstance().scan(this, CommunicationInterface.USB)
    }

    fun stopDiscovery() {
      DiscoveryFactory.getInstance().stop()
    }

    override fun onCameraFound(identity: Identity) {
      // called for every Camera found
      LOG.D(TAG, "Camera found: $identity.cameraType ${identity.deviceId}")
    }

    override fun onCameraLost(identity: Identity) {
      //Camera was lost from scanning
      LOG.D(TAG, "Camera lost: $identity.cameraType ${identity.deviceId}")
    }

    override fun onDiscoveryError(communicationInterface: CommunicationInterface, error: ErrorCode) {
      LOG.D(TAG, "Discovery Error:${error.code}: ${error.message} ${communicationInterface.name} ")
      // communicationInterface is the communication interface where the error occurred,
      // the client might scan on several interfaces.
    }

    override fun onDiscoveryFinished(communicationInterface: CommunicationInterface ) {
      // invoked if the OS is shutting down the scan (will NOT be called if the user calls "stop()"
    }

  }

}