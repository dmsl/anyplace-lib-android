package cy.ac.ucy.cs.anyplace.lib.android.data

import cy.ac.ucy.cs.anyplace.lib.android.data.source.DataSourceLocal
import cy.ac.ucy.cs.anyplace.lib.android.data.source.DataSourceRemote
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Anyplace Repository:
 * Has
 * - [DataSourceRemote]: Anyplace Remote Server connection
 * - [DataSourceLocal]: Anyplace Local Storage TODO Room/SQLite
 */
@Singleton
class RepoAP @Inject constructor(
        dataSourceRemote: DataSourceRemote,
        dataSourceLocal: DataSourceLocal) {
  val remote = dataSourceRemote
  val local = dataSourceLocal
}