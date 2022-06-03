package cy.ac.ucy.cs.anyplace.lib.android.data.smas.db
import androidx.room.*
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.CHAT
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.entities.ChatMsgEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.entities.CvModelClassEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmasDAO {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertChatMsg(tuple: ChatMsgEntity)

  @Query("SELECT * FROM ${CHAT.DB_SMAS_MSGS} ORDER BY time ASC")
  fun readMsgs(): Flow<List<ChatMsgEntity>>

  @Query("DELETE FROM ${CHAT.DB_SMAS_MSGS}")
  fun dropMsgs()

  @Query("SELECT time FROM ${CHAT.DB_SMAS_MSGS} ORDER BY time DESC LIMIT 1")
  fun lastMsgTimestamp(): Long?

  @Query("SELECT COUNT(mid) FROM ${CHAT.DB_SMAS_MSGS}")
  fun countMsgs(): Int?

  @Query("DELETE FROM ${CHAT.DB_SMAS_CV_MODELS}")
  fun dropCvModelClasses()

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCvModelClass(tuple: CvModelClassEntity)

  @Query("SELECT * FROM ${CHAT.DB_SMAS_CV_MODELS} WHERE modelid == :modelId")
  fun readCvModelClasses(modelId: Int): Flow<List<CvModelClassEntity>>

  @Query("SELECT COUNT(oid) FROM ${CHAT.DB_SMAS_CV_MODELS}")
  fun countCvModelClasses(): Int?

  @Query("SELECT DISTINCT modelid FROM ${CHAT.DB_SMAS_CV_MODELS}")
  fun getModelIds(): List<Int>

}
