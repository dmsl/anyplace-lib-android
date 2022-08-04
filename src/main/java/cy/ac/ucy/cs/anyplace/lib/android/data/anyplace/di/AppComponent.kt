package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.di

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

/**
 * Application Component (DI / Dependency Injection)
 * - if unfamiliar: see tutorials on Hilt/Dagger for Android
 */
@Component
@Singleton
interface AppComponent {
  @Component.Builder
  interface Builder {
    fun build(): AppComponent

    @BindsInstance
    fun application(application: Application): Builder
  }
}