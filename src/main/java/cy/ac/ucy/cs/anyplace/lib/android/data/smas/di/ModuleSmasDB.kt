package cy.ac.ucy.cs.anyplace.lib.android.data.smas.di

import android.content.Context
import androidx.room.Room
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.SMAS
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.SmasDB
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Module for the Database (DI / Dependency Injection)
 * - if unfamiliar: see tutorials on Hilt/Dagger for Android
 */
@Module
@InstallIn(SingletonComponent::class)
object ModuleSmasDB {

  @Singleton
  @Provides
  fun provideDatabase(
          @ApplicationContext ctx: Context) = Room.databaseBuilder(
          ctx,
          SmasDB::class.java,
          SMAS(ctx).DB_SMAS_NAME)
          .build()

  @Singleton
  @Provides
  fun provideDao(db: SmasDB) = db.DAO()
}
