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

  // FLOORPLANS
  suspend fun getFloorplanBase64(buid: String, floorNum: String) : Response<ResponseBody> = RH.api.floorplanBase64(buid, floorNum)

  // MISC
  suspend fun getVersion(): Response<Version>  = RH.api.getVersion()


  suspend fun getSpaceConnectionsAll(buid: String) : Response<ConnectionsResp> = RH.api.connectionsSpaceAll(ReqSpaceId(buid))

  suspend fun getSpacePOIsAll(buid: String) : Response<POIsResp> = RH.api.poisSpaceAll(ReqSpacePOIs(buid))

  suspend fun userLoginLocal(obj: UserLoginLocalForm) : Response<UserLoginResponse> = RH.api.userLoginLocal(obj)

  suspend fun userLoginGoogle(obj: UserLoginGoogleData) : Response<UserLoginResponse> = RH.api.userLoginGoogle(obj)

  // SPACES
  /** Get the spaces owned by a user */
  suspend fun getSpacesUser(token: String) : Response<Spaces> = RH.api.getSpacesUser(token)

  /** Get the accessible spaces (spaces the user owns, or co-owns) */
  suspend fun getSpacesAccessible(token: String) : Response<Spaces> = RH.api.getSpacesAccessible(token)

  /** Get the public spaces */
  suspend fun getSpacesPublic() : Response<Spaces> = RH.api.getSpacesPublic()

  /** Get more details of a particualr space */
  suspend fun getSpace(buid: String) : Response<Space> = RH.api.space(ReqSpaceId(buid))

  /** Get all floors' information of a Space */
  suspend fun getFloors(buid: String) : Response<Floors> = RH.api.floors(ReqSpaceId(buid))

}