package cy.ac.ucy.cs.anyplace.lib.android.consts

import cy.ac.ucy.cs.anyplace.lib.android.data.db.entities.SpaceType
import cy.ac.ucy.cs.anyplace.lib.android.data.db.entities.UserOwnership

class CONST {
  companion object
  {
    const val STATUS_OK = 200

    // EXCEPTIONS (messages set by 3rd party libs)
    val EXCEPTION_MSG_HTTP_FORBIDEN = "not permitted by network security policy"
    val EXCEPTION_MSG_ILLEGAL_STATE= "java.lang.IllegalStateException"
    val MSG_ERR_ONLY_SSL = "Only SSL connections allowed (HTTPS)"
    val EXCEPTION_MSG_NPE = "NullPointerException"
    val MSG_ERR_COMMUNICATION= "Failed to communicate"
    val MSG_ERR_ILLEGAL_STATE= "$MSG_ERR_COMMUNICATION (Illegal State)"
    val MSG_ERR_NPE = "$MSG_ERR_COMMUNICATION (NPE)"

    // PREFERENCES
    //// BACKEND SERVER
    const val PREF_SERVER = "pref_server"
    const val PREF_SERVER_PROTOCOL = "pref_server_protocol"
    const val PREF_SERVER_HOST = "pref_server_host"
    const val PREF_SERVER_PORT = "pref_server_port"
    const val PREF_SERVER_VERSION= "pref_server_version"

    const val DEFAULT_PREF_SERVER_PROTOCOL = "https"
    const val DEFAULT_PREF_SERVER_HOST = "ap.cs.ucy.ac.cy" // TODO ap.cs
    const val DEFAULT_PREF_SERVER_PORT = "44"

    //// CV LOGGER
    const val PREF_CVLOG = "pref_cvlog"
    const val PREF_CVLOG_WINDOW_SECONDS = "pref_cvlog_window_seconds"
    const val PREF_CVLOG_DEV_MODE = "pref_cvlog_dev_mode"
    const val PREF_CVLOG_EPX_IMG_PADDING = "pref_cvlog_exp_img_padding"

    const val DEFAULT_PREF_CVLOG_WINDOW_SECONDS = "5"
    const val DEFAULT_PREF_CVLOG_DEV_MODE = false
    const val DEFAULT_PREF_CVLOG_EPX_IMG_PADDING = false

    //// USER
    const val PREF_USER = "pref_user"
    const val PREF_USER_ACCESS_TOKEN = "pref_user_access_token"
    const val PREF_USER_NAME= "pref_user_name"
    const val PREF_USER_EMAIL = "pref_user_email"
    const val PREF_USER_TYPE = "pref_user_type"
    const val PREF_PHOTO_URI = "pref_photo_uro"
    const val PREF_USER_ACCOUNT= "pref_user_account"
    const val PREF_USER_ID= "pref_user_id"
    const val PREF_USER_USERNAME= "pref_user_username"

    //// ROOM
    const val DB_NAME = "anyplace_db"
    const val DB_TBL_SPACES = "spaces"

    //// MISC
    const val PREF_MISC_NAME = "pref_misc"
    const val PREF_MISC_BACK_ONLINE = "pref_misc_backOnline"
    const val PREF_MISC_BACK_FROM_SETTINGS = "pref_misc_backFromSettings"

    // public, accessible, owned
    const val PREF_MISC_QUERY_SPACE_OWNERSHIP= "pref_misc_queryType_space_ownership"
    const val PREF_MISC_QUERY_SPACE_OWNERSHIP_ID= "pref_misc_queryType_space_ownershipId"
    // building or vessel
    const val PREF_MISC_QUERY_SPACE_TYPE= "pref_misc_queryType_space_type"
    const val PREF_MISC_QUERY_SPACE_TYPE_ID="pref_misc_queryType_space_typeId"

    ///// QUERY TYPES
    val DEFAULT_QUERY_SPACE_OWNERSHIP = UserOwnership.PUBLIC.toString().uppercase()
    val DEFAULT_QUERY_SPACE_TYPE = SpaceType.BUILDING.toString().uppercase()
  }
}