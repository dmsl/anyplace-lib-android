package cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.entities
import androidx.room.Entity
import androidx.room.PrimaryKey
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.SMAS

/**
 * DB Entity based on [ChatMsg]
 */
@Entity(tableName = SMAS.DB_MSGS)
data class ChatMsgEntity(
  /** Message ID */
  @PrimaryKey(autoGenerate = false)
  var mid: String,
  val uid: String,
  val mdelivery: Int,
  val mtype: Int,
  val msg: String?,
  val mexten: String,
  val time: Long,
  val timestr: String,
  val x: Double,
  val y: Double,
  val buid: String,
  val deck: Int,
)