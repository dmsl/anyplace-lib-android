package cy.ac.ucy.cs.anyplace.lib.android.data.smas.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.entities.*

@Database(
        entities = [
          ChatMsgEntity::class,
          OBJECT::class,
          FINGERPRINT::class,
          FINGERPRINT_LOCALIZE_TEMP::class,
        ],
        views =[
          OBJECT_COUNT::class,
          MODEL_SUM::class,
          OBJECT_FREQUENCY::class,
        ],
        // INFO we need to update this number on schema changes, or uninstall/reinstall the app
        version = 5,
        exportSchema = false
)
@TypeConverters(ConverterDB::class)
abstract class SmasDB: RoomDatabase() {
  abstract fun DAO(): SmasDAO
}
