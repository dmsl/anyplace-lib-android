package cy.ac.ucy.cs.anyplace.lib.android.utils

import cy.ac.ucy.cs.anyplace.lib.models.Version

/**
 * Anyplace Utils
 */
object utlAP {
    fun prettyVersion(version: Version) : String {
      var s = version.version
      if(version.variant.isNotEmpty()) s+="-"+version.variant
      return s
    }
  }