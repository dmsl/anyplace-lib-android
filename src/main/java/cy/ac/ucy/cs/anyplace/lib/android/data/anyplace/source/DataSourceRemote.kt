package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.source

import cy.ac.ucy.cs.anyplace.lib.android.utils.net.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.*
import retrofit2.Response
import javax.inject.Inject
import okhttp3.ResponseBody

/**
 * Anyplace DataSource
 */
class DataSourceRemote @Inject constructor(private val retrofitHolderAP: RetrofitHolderAP) {

  // FLOORS
  // TODO FloorsAll

  // FLOORPLANS
  suspend fun getFloorplanBase64(buid: String, floorNum: String) : Response<ResponseBody>
      = retrofitHolderAP.api.floorplanBase64(buid, floorNum)

  // MISC
  suspend fun getVersion(): Response<Version>  = retrofitHolderAP.api.getVersion()





















  // DEMO CODE:
  // USER
  suspend fun userLoginLocal(obj: UserLoginLocalForm) : Response<UserLoginResponse>
      = retrofitHolderAP.api.userLoginLocal(obj)

  suspend fun userLoginGoogle(obj: UserLoginGoogleData) : Response<UserLoginResponse>
      = retrofitHolderAP.api.userLoginGoogle(obj)

  // SPACES
  suspend fun getSpacesPublic() : Response<Spaces> = retrofitHolderAP.api.getSpacesPublic()
}