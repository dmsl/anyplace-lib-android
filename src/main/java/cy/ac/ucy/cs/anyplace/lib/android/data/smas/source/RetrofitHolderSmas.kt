package cy.ac.ucy.cs.anyplace.lib.android.data.smas.source

import android.content.Context
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.CHAT
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.store.ChatPrefs
import cy.ac.ucy.cs.anyplace.lib.smas.ChatAPI
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Retrofit Holder For Chat:
 * It's purpose:
 * - enable dynamical changes of backend URLs
 * - DI (DepInjection) alone would have stale data)
 */
data class RetrofitHolderSmas(
        val ctx: Context,
        val okHttpClientChat: OkHttpClient,
        val gsonCF: GsonConverterFactory) {

  lateinit var baseURL: String
  lateinit var retrofit: Retrofit
  lateinit var api: ChatAPI

  companion object {
    fun getDefaultBaseUrl(ctx: Context): String {
      val c = CHAT(ctx)
      val protocol = c.DEFAULT_PREF_CHAT_SERVER_PROTOCOL
      val host = c.DEFAULT_PREF_CHAT_SERVER_HOST
      val port = c.DEFAULT_PREF_CHAT_SERVER_PORT

      return getBaseUrl(protocol, host, port)
    }

    private fun getBaseUrl(protocol: String, host: String, port: String)
            = "${protocol}://${host}:${port}"
  }

  private fun getBaseUrl(p: ChatPrefs) = getBaseUrl(p.protocol, p.host, p.port)

  fun set(prefs: ChatPrefs) = set(getBaseUrl(prefs))
  fun set(baseUrl: String) : RetrofitHolderSmas {
    this.baseURL = baseUrl

    this.retrofit = Retrofit.Builder()
        .baseUrl(this.baseURL)
        .client(okHttpClientChat)
        .addConverterFactory(gsonCF)
        .build()
    this.api = this.retrofit.create(ChatAPI::class.java)
    return this
  }
}