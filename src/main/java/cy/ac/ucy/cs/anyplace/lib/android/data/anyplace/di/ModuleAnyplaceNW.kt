package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.di

import android.app.Application
import cy.ac.ucy.cs.anyplace.lib.BuildConfig
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.di.OkHttpClientBearer
import cy.ac.ucy.cs.anyplace.lib.android.utils.net.RetrofitHolderAP
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

@Module
@InstallIn(SingletonComponent::class) // Formerly ApplicationComponent
class ModuleAnyplaceNW {

  @Singleton // Application lifecycle
  @Provides  // External class
  fun provideHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
            .readTimeout(15, TimeUnit.SECONDS) // TODO: Make SETTINGS
            .connectTimeout(15, TimeUnit.SECONDS)
            .build()
  }

  @Singleton
  @Provides
  fun provideConverterFactory(): GsonConverterFactory {
    return GsonConverterFactory.create()
  }

  @Singleton
  @Provides
  @Inject
  fun provideRetrofitHolder(
          app: Application,  // injected from ContextModule
          okHttpClient: OkHttpClient,
          gsonConverterFactory: GsonConverterFactory): RetrofitHolderAP {
    val baseUrl = RetrofitHolderAP.getDefaultBaseUrl(app)
    return RetrofitHolderAP(app, okHttpClient, gsonConverterFactory).set(baseUrl)
  }
}