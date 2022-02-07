package cy.ac.ucy.cs.anyplace.lib.android.di

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

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