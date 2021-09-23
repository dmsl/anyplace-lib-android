package cy.ac.ucy.cs.anyplace.lib.android.data

import cy.ac.ucy.cs.anyplace.lib.android.utils.network.RetrofitHolder
import cy.ac.ucy.cs.anyplace.lib.models.*
import retrofit2.Response
import javax.inject.Inject

class RemoteDataSource @Inject constructor(private val retrofitHolder: RetrofitHolder) {

  // USER
  suspend fun userLoginLocal(obj: UserLoginLocalForm) : Response<UserLoginResponse>
      = retrofitHolder.api.userLoginLocal(obj)

  suspend fun userLoginGoogle(obj: UserLoginGoogleData) : Response<UserLoginResponse>
      = retrofitHolder.api.userLoginGoogle(obj)

  // SPACES
  suspend fun getSpacesPublic() : Response<Spaces> = retrofitHolder.api.getSpacesPublic()

  // MISC
  suspend fun getVersion(): Response<Version>  = retrofitHolder.api.getVersion()
}