package cy.ac.ucy.cs.anyplace.lib.android.data

import cy.ac.ucy.cs.anyplace.lib.android.utils.network.RetrofitHolder
import cy.ac.ucy.cs.anyplace.lib.models.*
import retrofit2.Response
import javax.inject.Inject
import okhttp3.ResponseBody

class RemoteDataSource @Inject constructor(private val retrofitHolder: RetrofitHolder) {

  // FLOORS
  // TODO FloorsAll

  // FLOORPLANS
  suspend fun getFloorplanBase64(buid: String, floorNum: String) : Response<ResponseBody>
      = retrofitHolder.api.floorplanBase64(buid, floorNum)

  // MISC
  suspend fun getVersion(): Response<Version>  = retrofitHolder.api.getVersion()





















  // DEMO CODE:
  // USER
  suspend fun userLoginLocal(obj: UserLoginLocalForm) : Response<UserLoginResponse>
      = retrofitHolder.api.userLoginLocal(obj)

  suspend fun userLoginGoogle(obj: UserLoginGoogleData) : Response<UserLoginResponse>
      = retrofitHolder.api.userLoginGoogle(obj)

  // SPACES
  suspend fun getSpacesPublic() : Response<Spaces> = retrofitHolder.api.getSpacesPublic()
}