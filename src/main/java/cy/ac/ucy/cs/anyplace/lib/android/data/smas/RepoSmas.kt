package cy.ac.ucy.cs.anyplace.lib.android.data.smas

import cy.ac.ucy.cs.anyplace.lib.android.data.smas.source.SmasLocalSRC
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.source.SmasRemoteSRC
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Smas Chat Repository:
 * Has
 * - [SmasRemoteSRC]: Chat Remote Server connection
 * - [DsLocalAP]: Chat Local Storage TODO Room/SQLite
 */
@Singleton
class RepoSmas @Inject constructor(
        dsRemoteSmas: SmasRemoteSRC,
        dsLocalAP: SmasLocalSRC) {
  /** Talks to the net */
  val remote = dsRemoteSmas
  /** Talks to ROOM */
  val local = dsLocalAP
}