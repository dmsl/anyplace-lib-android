package cy.ac.ucy.cs.anyplace.lib.android.di

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

// import dagger.Component
// import javax.inject.Singleton
// @Singleton
// @Component(modules= [AppModule::class])
// interface AppComponent

// @Component(modules = [AppModule::class])
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