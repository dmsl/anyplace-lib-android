package cy.ac.ucy.cs.anyplace.lib.android.utils.network

import cy.ac.ucy.cs.anyplace.lib.API
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.DEFAULT_PREF_SERVER_HOST
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.DEFAULT_PREF_SERVER_PORT
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.DEFAULT_PREF_SERVER_PROTOCOL
import cy.ac.ucy.cs.anyplace.lib.android.data.datastore.ServerPrefs
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class RetrofitHolder(
  val okHttpClient: OkHttpClient,
  val gsonConverterFactory: GsonConverterFactory)  {

  lateinit var baseURL: String
  lateinit var retrofit: Retrofit
  lateinit var api: API

  companion object {
    fun getDefaultBaseUrl(): String {
      val protocol = DEFAULT_PREF_SERVER_PROTOCOL
      val host = DEFAULT_PREF_SERVER_HOST
      val port = DEFAULT_PREF_SERVER_PORT

      return "${protocol}://${host}:${port}"
    }
  }

  private fun getBaseUrl(prefs: ServerPrefs) : String {
    return getBaseUrl(prefs.protocol, prefs.host, prefs.port)
  }

  private fun getBaseUrl(protocol: String, host: String, port: String)
      = "${protocol}://${host}:${port}"

  fun set(prefs: ServerPrefs) = set(getBaseUrl(prefs))
  fun set(baseUrl: String) : RetrofitHolder {
    this.baseURL = baseUrl
    this.retrofit = Retrofit.Builder()
        .baseUrl(this.baseURL)
        .client(okHttpClient)
        .addConverterFactory(gsonConverterFactory)
        .build()
    this.api = this.retrofit.create(API::class.java)
    return this
  }
}
