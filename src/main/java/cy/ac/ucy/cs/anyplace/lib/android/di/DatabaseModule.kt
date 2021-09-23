package cy.ac.ucy.cs.anyplace.lib.android.di

import android.content.Context
import androidx.room.Room
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST.Companion.DB_NAME
import cy.ac.ucy.cs.anyplace.lib.android.data.db.AnyplaceDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

  @Singleton
  @Provides
  fun provideDatabase(
    @ApplicationContext ctx: Context
  ) = Room.databaseBuilder(
    ctx,
    AnyplaceDatabase::class.java,
    DB_NAME)
    .build()

  @Singleton
  @Provides
  fun provideDao(database: AnyplaceDatabase) = database.anyplaceDao()

}
