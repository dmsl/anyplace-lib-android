package cy.ac.ucy.cs.anyplace.lib.android.data.smas.di

import android.content.Context
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.SMAS
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.store.SmasPrefs
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.di.RetrofitHolderBase
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
        val gsonCF: GsonConverterFactory) : RetrofitHolderBase() {

  lateinit var api: ChatAPI
  lateinit var path: String

  companion object {
    fun getDefaultBaseUrl(ctx: Context): String {
      val c = SMAS(ctx)
      val protocol = c.DEFREF_SMAS_SERVER_PROTOCOL
      val host = c.DEFPREF_SMAS_SERVER_HOST
      val port = c.DEFPREF_SMAS_SERVER_PORT

      val path = c.DEFPREF_SMAS_SERVER_PATH

      return getBaseUrl(protocol, host, port)
    }

    private fun getBaseUrl(protocol: String, host: String, port: String)
            = "${protocol}://${host}:${port}/"
  }

  private fun getBaseUrl(p: SmasPrefs) = getBaseUrl(p.protocol, p.host, p.port)

  fun set(prefs: SmasPrefs)  {
    path=prefs.path
    LOG.D3(TAG,"Setting path: $path")
    set(getBaseUrl(prefs))
  }
  fun set(baseUrl: String) : RetrofitHolderSmas {
    this.baseURL = baseUrl

    try {
      this.retrofit = Retrofit.Builder()
              .baseUrl(this.baseURL)
              .client(okHttpClientChat)
              .addConverterFactory(gsonCF)
              .build()
      this.api = this.retrofit.create(ChatAPI::class.java)

      return this
    } catch(e: Exception) {
      this.baseURL = getDefaultBaseUrl(ctx)
      this.retrofit = Retrofit.Builder()
              .baseUrl(this.baseURL)
              .client(okHttpClientChat)
              .addConverterFactory(gsonCF)
              .build()
      return this
    }
  }
}