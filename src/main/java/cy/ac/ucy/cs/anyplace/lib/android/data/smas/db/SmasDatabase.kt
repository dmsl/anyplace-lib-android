package cy.ac.ucy.cs.anyplace.lib.android.data.smas.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.entities.ChatMsgEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.entities.CvModelClassEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.entities.DatabaseConverters

@Database(
  entities = [ChatMsgEntity::class,
             CvModelClassEntity::class],
  // INFO we need to update this number on schema changes, or uninstall/reinstall the app
  version = 2,
  exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class SmasDB: RoomDatabase() {
  abstract fun DAO(): SmasDAO
}
