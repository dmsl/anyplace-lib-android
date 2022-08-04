package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.*
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Module for the Anyplace API backend (DI / Dependency Injection)
 * - if unfamiliar: see tutorials on Hilt/Dagger for Android
 */
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