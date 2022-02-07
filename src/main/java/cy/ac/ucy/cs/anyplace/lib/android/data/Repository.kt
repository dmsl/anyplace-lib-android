package cy.ac.ucy.cs.anyplace.lib.android.data

import javax.inject.Inject
import javax.inject.Singleton

/**
 * TODO: Hold local and remote repositories
 * TODO: can we put file cache also in here?
 */
@Singleton
class Repository @Inject constructor(
        remoteDataSource: RemoteDataSource,
        localDataSource: LocalDataSource) {
  val remote = remoteDataSource
  val local = localDataSource
}