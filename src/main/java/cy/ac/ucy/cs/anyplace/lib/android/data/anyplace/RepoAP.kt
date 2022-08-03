package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace

import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.source.ApLocalSRC
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.source.ApRemoteSRC
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Anyplace Repository:
 * Has
 * - [ApRemoteSRC]: Anyplace Remote Server connection
 * - [ApLocalSRC]: Anyplace Local Storage TODO Room/SQLite
 */
@Singleton
class RepoAP @Inject constructor(
        apRemoteSRC: ApRemoteSRC,
        dsLocal: ApLocalSRC) {
  val remote = apRemoteSRC
  val local = dsLocal
}