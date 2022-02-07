package cy.ac.ucy.cs.anyplace.lib.android.utils.network

import android.content.Context
import cy.ac.ucy.cs.anyplace.lib.API
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.data.datastore.ServerPrefs
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class RetrofitHolder(
        val ctx: Context,
        val okHttpClient: OkHttpClient,
        val gsonConverterFactory: GsonConverterFactory)  {

  lateinit var baseURL: String
  lateinit var retrofit: Retrofit
  lateinit var api: API

  companion object {
    fun getDefaultBaseUrl(ctx: Context): String {
      val c = CONST(ctx)
      val protocol = c.DEFAULT_PREF_SERVER_PROTOCOL
      val host = c.DEFAULT_PREF_SERVER_HOST
      val port = c.DEFAULT_PREF_SERVER_PORT

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
