package cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.SMAS

/**
 * DB Entity Based on [CvModelClass]
 */
@Entity(tableName = SMAS.DB_OBJECT)
data class OBJECT(
  /** SMAS generated OID (alt key: smas model id + class id) */
  @PrimaryKey(autoGenerate = false)
  var oid: Int,
  val cid: Int,
  val modeldescr: String,
  val modelid: Int,
  val name: String,
)