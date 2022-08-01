package cy.ac.ucy.cs.anyplace.lib.android.data.smas.di

import android.app.Application
import cy.ac.ucy.cs.anyplace.lib.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.*
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/** Specializing [OkHttpClient], only to make DI to pick a version with authenticator set*/
data class OkHttpClientBearer(val client: OkHttpClient)

/**
 * Dependency Injection for the Chat backend
 */
@Module
@InstallIn(SingletonComponent::class)
class ModuleSmasNW {

  @Singleton
  @Provides
  fun provideHttpClientWithBearer(): OkHttpClientBearer {
    return getUnsafeOkHttpClientBearer()
  }

  private fun getUnsafeOkHttpClientBearer(): OkHttpClientBearer {
    try {
      // Create a trust manager that does not validate certificate chains
      val trustAllCerts: Array<TrustManager> = arrayOf(
              object : X509TrustManager {
                @Throws(CertificateException::class)
                override fun checkClientTrusted(chain: Array<X509Certificate?>?,
                                                authType: String?) = Unit

                @Throws(CertificateException::class)
                override fun checkServerTrusted(chain: Array<X509Certificate?>?,
                                                authType: String?) = Unit

                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
              }
      )
      // Install the all-trusting trust manager
      val sslContext: SSLContext = SSLContext.getInstance("SSL")
      sslContext.init(null, trustAllCerts, SecureRandom())
      // Create an ssl socket factory with our all-trusting manager
      val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory
      return OkHttpClientBearer(OkHttpClient.Builder()
              .authenticator(SmasBearerAuth())
              .sslSocketFactory(sslSocketFactory,
                      trustAllCerts[0] as X509TrustManager)
              .hostnameVerifier { _, _ -> true }
              .readTimeout(15, TimeUnit.SECONDS) // TODO: Make SETTINGS
              .connectTimeout(15, TimeUnit.SECONDS)
              .build())
    } catch (e: Exception) {
      throw RuntimeException(e)
    }
  }


  @Singleton
  @Provides
  @Inject
  fun provideRetrofitHolderChat(
          app: Application, // injected from ContextModule
          okHttpClientBearer: OkHttpClientBearer,
          gsonConverterFactory: GsonConverterFactory,
  ): RetrofitHolderSmas {
    val baseUrl = RetrofitHolderSmas.getDefaultBaseUrl(app)

    return RetrofitHolderSmas(app, okHttpClientBearer.client, gsonConverterFactory).set(baseUrl)
  }
}

/**
 * Bearer Authentication for the SMAS Chat
 */
class SmasBearerAuth : Authenticator {
  override fun authenticate(route: Route?, response: Response): Request? {
    if (response.request.header("Authorization") != null) {
      return null
    }
    val token = BuildConfig.SMAS_API_KEY
    return response.request.newBuilder().header("Authorization", "Bearer $token").build()
  }
}