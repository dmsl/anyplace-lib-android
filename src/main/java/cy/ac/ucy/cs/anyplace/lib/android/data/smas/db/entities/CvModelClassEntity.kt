package cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.CHAT

/**
 * Based on [CvModelClass]
 */
@Entity(tableName = CHAT.DB_SMAS_CV_MODELS)
data class CvModelClassEntity(
  /** SMAS generated OID (alt key: smas model id + class id) */
  @PrimaryKey(autoGenerate = false)
  var oid: Int,
  val cid: Int,
  val modeldescr: String,
  val modelid: Int,
  val name: String,
)