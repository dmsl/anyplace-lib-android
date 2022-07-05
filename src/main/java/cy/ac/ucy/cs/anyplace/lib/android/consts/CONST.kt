package cy.ac.ucy.cs.anyplace.lib.android.consts

import android.content.Context
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceType
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.UserOwnership

open class CONST(ctx: Context) {

  //// ROOM
  companion object {
    const val DB_TBL_SPACES = "spaces"
  }
  val DB_NAME = "anyplace_db"

  val STATUS_OK = 200
  // EXCEPTIONS (messages set by 3rd party libs)
  val ERR_MSG_NO_INTERNET= "No Internet Connection."
  val ERR_NO_CV_CLASSES= "Cannot recognize objects.\n(CvModel not initialized)"

  val ERR_MSG_HTTP_FORBIDEN = "not permitted by network security policy"
  val ERR_MSG_ILLEGAL_STATE = "java.lang.IllegalStateException"
  val MSG_ERR_ONLY_SSL = "Only SSL connections allowed (HTTPS)"
  val EXCEPTION_MSG_NPE = "NullPointerException"
  val MSG_ERR_COMMUNICATION = "Failed to communicate"
  val MSG_ERR_ILLEGAL_STATE = "$MSG_ERR_COMMUNICATION (Illegal State)"
  val MSG_ERR_NPE = "$MSG_ERR_COMMUNICATION (NPE)"

  // TODO CONVERT all properties to xml strings (R.strings... also put in a separate file!)
  // PREFERENCES
  //// BACKEND SERVER
  val PREF_SERVER = "pref_server"
  val PREF_SERVER_PROTOCOL = "pref_server_protocol"
  val PREF_SERVER_HOST = "pref_server_host"
  val PREF_SERVER_PORT = "pref_server_port"
  val PREF_SERVER_VERSION = "pref_server_version"

  val DEFAULT_PREF_SERVER_PROTOCOL = ctx.getString(R.string.default_pref_server_protocol)
  val DEFAULT_PREF_SERVER_HOST = ctx.getString(R.string.default_pref_server_host)
  val DEFAULT_PREF_SERVER_PORT = ctx.getString(R.string.default_pref_server_port)

  /** shared settings in cv activities*/
  val PREF_CV = "pref_cv"
  val PREF_MODEL_NAME = "pref_cv_model_name"
  val PREF_RELOAD_CVMAPS = "pref_cv_reload_cvmaps"
  val PREF_RELOAD_FLOORPLAN = "pref_cv_reload_floorplans"  // TODO

  val DEFAULT_PREF_MODEL_NAME = "coco"

  // Settings used in both contexts (Logger, Navigator)
  val PREF_CV_WINDOW_LOCALIZATION_MS = ctx.getString(R.string.pref_cv_localization_ms)
  val PREF_CV_SCAN_DELAY = ctx.getString(R.string.pref_cv_scan_delay)
  val PREF_CV_DEV_MODE = ctx.getString(R.string.pref_cv_dev_mode)
  //// Settings for Cv Logger
  val PREF_CVLOG = ctx.getString(R.string.pref_cvlog)
  val PREF_CV_WINDOW_LOGGING_MS = ctx.getString(R.string.pref_cv_logging_ms)

  // COMMON DEFAULTS
  val DEFAULT_PREF_CV_DEV_MODE = false
  val DEFAULT_PREF_CV_SCAN_DELAY= "500"
  //// LOGGING
  val DEFAULT_PREF_CVLOG_WINDOW_LOGGING_MS = "2000"
  //// LOCALIZATION
  val DEFAULT_PREF_CVLOG_WINDOW_LOCALIZATION_MS = "2000"

  //// Settings for Cv Navigator/Localization apps
  val PREF_CVNAV = ctx.getString(R.string.pref_cvnav)
  val PREF_CVNAV_MAP_ALPHA= ctx.getString(R.string.pref_cvnav_map_alpha)
  //// NAVIGATION
  // val DEFAULT_PREF_CVNAV_WINDOW_LOCALIZATION_SECONDS = "2"
  val DEFAULT_PREF_CVNAV_MAP_ALPHA= "90"

  //// SMAS
  // this should have been in separate SMAS settings
  val PREF_SMAS_LOCATION_REFRESH_MS= ctx.getString(R.string.pref_smas_location_refresh)
  val DEFAULT_PREF_SMAS_LOCATION_REFRESH_MS= "2000"

  //// MISC
  val PREF_MISC_NAME = "pref_misc"
  val PREF_MISC_BACK_ONLINE = "pref_misc_backOnline"
  val PREF_MISC_BACK_FROM_SETTINGS = "pref_misc_backFromSettings"

  //// USER
  val PREF_USER = "pref_user"
  val PREF_USER_ACCESS_TOKEN = "pref_user_access_token"
  val PREF_USER_NAME = "pref_user_name"
  val PREF_USER_EMAIL = "pref_user_email"
  val PREF_USER_TYPE = "pref_user_type"
  val PREF_PHOTO_URI = "pref_photo_uro"
  val PREF_USER_ACCOUNT = "pref_user_account"
  val PREF_USER_ID = "pref_user_id"
  val PREF_USER_USERNAME = "pref_user_username"

  // public, accessible, owned
  val PREF_MISC_QUERY_SPACE_OWNERSHIP = "pref_misc_queryType_space_ownership"
  val PREF_MISC_QUERY_SPACE_OWNERSHIP_ID = "pref_misc_queryType_space_ownershipId"

  // building or lashco-wrong
  val PREF_MISC_QUERY_SPACE_TYPE = "pref_misc_queryType_space_type"
  val PREF_MISC_QUERY_SPACE_TYPE_ID = "pref_misc_queryType_space_typeId"

  ///// QUERY TYPES
  val DEFAULT_QUERY_SPACE_OWNERSHIP = UserOwnership.PUBLIC.toString().uppercase()
  val DEFAULT_QUERY_SPACE_TYPE = SpaceType.BUILDING.toString().uppercase()
}