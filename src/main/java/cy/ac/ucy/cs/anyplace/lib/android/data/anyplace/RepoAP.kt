package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace

import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.source.ApLocalDS
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.source.ApRemoteDS
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Anyplace Repository:
 * Has
 * - [ApRemoteDS]: Anyplace Remote Server connection
 * - [ApLocalDS]: Anyplace Local Storage TODO Room/SQLite
 */
@Singleton
class RepoAP @Inject constructor(
        apRemoteDS: ApRemoteDS,
        dsLocal: ApLocalDS) {
  val remote = apRemoteDS
  val local = dsLocal
}