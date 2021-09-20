package cy.ac.ucy.cs.anyplace.lib.android.consts

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
    const val DEFAULT_PREF_SERVER_HOST = "ap-dev.cs.ucy.ac.cy" // TODO ap.cs
    const val DEFAULT_PREF_SERVER_PORT = "9001" // TODO: 443?

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

    //// MISC
    const val PREF_MISC_NAME = "pref_misc"
    const val PREF_MISC_BACK_ONLINE = "pref_misc_backOnline"
    const val PREF_MISC_BACK_FROM_SETTINGS = "pref_misc_backFromSettings"
  }
}