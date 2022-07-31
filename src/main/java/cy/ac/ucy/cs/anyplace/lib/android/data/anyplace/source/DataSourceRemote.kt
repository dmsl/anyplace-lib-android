package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.source

import cy.ac.ucy.cs.anyplace.lib.android.utils.net.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.*
import retrofit2.Response
import javax.inject.Inject
import okhttp3.ResponseBody

/**
 * Anyplace DataSource
 */
class DataSourceRemote @Inject constructor(private val RH: RetrofitHolderAP) {

  // FLOORS
  // TODO FloorsAll

  // FLOORPLANS
  suspend fun getFloorplanBase64(buid: String, floorNum: String) : Response<ResponseBody>
      = RH.api.floorplanBase64(buid, floorNum)

  // MISC
  suspend fun getVersion(): Response<Version>  = RH.api.getVersion()

  suspend fun getSpace(buid: String) : Response<Space> = RH.api.space(ReqSpaceId(buid))

  suspend fun getFloors(buid: String) : Response<Floors> = RH.api.floors(ReqSpaceId(buid))

  suspend fun getSpaceConnectionsAll(buid: String) : Response<ConnectionsResp>
          = RH.api.connectionsSpaceAll(ReqSpaceId(buid))

  suspend fun getSpacePOIsAll(buid: String) : Response<POIsResp>
          = RH.api.poisSpaceAll(ReqSpacePOIs(buid))

  suspend fun userLoginLocal(obj: UserLoginLocalForm) : Response<UserLoginResponse>
      = RH.api.userLoginLocal(obj)

  suspend fun userLoginGoogle(obj: UserLoginGoogleData) : Response<UserLoginResponse>
      = RH.api.userLoginGoogle(obj)

  // SPACES
  suspend fun getSpacesPublic() : Response<Spaces> = RH.api.getSpacesPublic()

  suspend fun getSpacesAccessible(token: String) : Response<Spaces> = RH.api.getSpacesAccessible(token)
}