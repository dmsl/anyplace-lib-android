package cy.ac.ucy.cs.anyplace.lib.android.di

import cy.ac.ucy.cs.anyplace.lib.android.utils.network.RetrofitHolder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Formerly ApplicationComponent
class NetworkModule {

  @Singleton // For Application lifecycle
  @Provides  // External class
  fun provideHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
        .readTimeout(15, TimeUnit.SECONDS) // TODO:PM parameters..
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
  fun provideRetrofitHolder(
    okHttpClient: OkHttpClient,
    gsonConverterFactory: GsonConverterFactory,
  ): RetrofitHolder {
    val baseUrl = RetrofitHolder.getDefaultBaseUrl()
    return RetrofitHolder(okHttpClient, gsonConverterFactory).set(baseUrl)
  }

}
