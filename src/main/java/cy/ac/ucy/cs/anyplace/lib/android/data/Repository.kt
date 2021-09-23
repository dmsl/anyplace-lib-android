package cy.ac.ucy.cs.anyplace.lib.android.data

import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class Repository @Inject constructor(
  remoteDataSource: RemoteDataSource) {
  val remote = remoteDataSource
}