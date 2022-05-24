package cy.ac.ucy.cs.anyplace.lib.android.data.smas

import cy.ac.ucy.cs.anyplace.lib.android.data.smas.source.SmasLocalDS
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.source.ChatRemoteDataSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Smas Chat Repository:
 * Has
 * - [ChatRemoteDataSource]: Chat Remote Server connection
 * - [DsLocalAP]: Chat Local Storage TODO Room/SQLite
 */
@Singleton
class RepoSmas @Inject constructor(
        chatRemoteDataSource: ChatRemoteDataSource,
        dsLocalAP: SmasLocalDS) {
  /** Talks to the net */
  val remote = chatRemoteDataSource
  /** Talks to ROOM */
  val local = dsLocalAP
}