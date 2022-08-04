package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceEntity

/**
 * The Anyplace ROOM Database (for SQLite)
 */
@Database(
  entities = [SpaceEntity::class],
  version = 1, // INFO we need to update this number on schema changes, or uninstall & reinstall the app
  exportSchema = false
)
@TypeConverters(SpaceTypeConverter::class)
abstract class AnyplaceDB: RoomDatabase() {
  abstract fun anyplaceDao(): AnyplaceDAO
}
