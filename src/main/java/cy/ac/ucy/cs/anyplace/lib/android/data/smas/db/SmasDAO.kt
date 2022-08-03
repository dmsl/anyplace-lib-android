package cy.ac.ucy.cs.anyplace.lib.android.data.smas.db
import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.SMAS
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SmasDAO {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertChatMsg(tuple: ChatMsgEntity)

  @Query("SELECT * FROM ${SMAS.DB_MSGS} ORDER BY time ASC")
  fun readMsgs(): Flow<List<ChatMsgEntity>>

  @Query("DELETE FROM ${SMAS.DB_MSGS}")
  fun dropMsgs()

  @Query("SELECT time FROM ${SMAS.DB_MSGS} ORDER BY time DESC LIMIT 1")
  fun lastMsgTimestamp(): Long?

  @Query("SELECT COUNT(mid) FROM ${SMAS.DB_MSGS}")
  fun countMsgs(): Int?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCvModelClass(tuple: OBJECT)

  @Query("DELETE FROM ${SMAS.DB_OBJECT}")
  fun dropCvModelClasses()

  @Query("SELECT * FROM ${SMAS.DB_OBJECT} WHERE modelid == :modelId")
  fun readCvModelClasses(modelId: Int): Flow<List<OBJECT>>

  @Query("SELECT COUNT(oid) FROM ${SMAS.DB_OBJECT}")
  fun countCvModelClasses(): Int?

  @Query("SELECT DISTINCT modelid FROM ${SMAS.DB_OBJECT}")
  fun getModelIds(): List<Int>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCvMapRow(tuple: FINGERPRINT)

  // TODO:PMX
  @Query("SELECT COUNT(foid) FROM ${SMAS.DB_FINGERPRINT}")
  fun countCvMapRows(): Int?

  @Query("DELETE FROM ${SMAS.DB_FINGERPRINT}")
  fun dropCvMap()

  /** Used to fetch only the newest fingerprints */
  @Query("SELECT time FROM ${SMAS.DB_FINGERPRINT} ORDER BY time DESC LIMIT 1")
  fun lastFingerprintTimestamp(): Long?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLocalizeTemp(tuple: FINGERPRINT_LOCALIZE_TEMP)

  @Query("DELETE FROM ${SMAS.DB_LOCALIZE_TEMP}")
  fun dropLocalizeFpTemp()

  /**
   * LOCALIZATION ALGORITHM 1
   * (just for testing purposes)
   */
  @Query(
          "    SELECT FL.flid, FL.time, FL.buid, FL.X, FL.Y, FL.deck, FL.modelid\n" +
                  "    FROM ${SMAS.DB_FINGERPRINT} FL, ${SMAS.DB_FINGERPRINT} FO\n" +
                  "    WHERE FL.flid = FO.flid and FL.modelid=:modelid and FL.buid=:buid and\n" +
                  "    NOT EXISTS\n" +
                  "    (\n" +
                  "     -- all oid of radiomap fingerprint\n" +
                  "     SELECT F.oid FROM ${SMAS.DB_FINGERPRINT} F WHERE F.flid = FO.flid\n" +
                  "     EXCEPT\n" +
                  "     -- all oid of radiomap fingerprint\n" +
                  "     SELECT FLT.oid FROM FINGERPRINT_LOCALIZE_TEMP FLT where FLT.uid=:uid\n" +
                  "    )\n" +
                  "    GROUP BY FL.X, FL.Y, FL.deck -- keep only x,y,deck\n" +
                  "    ORDER BY FL.time DESC\n" +
                  "    LIMIT 1; -- sort by newest match to oldest MATCH\n" +
                  "    -- SELECT * FROM NN;\n"
  )
  fun localizeAlgo1(modelid: Int, buid: String, uid: String): Flow<List<OfflineLocalizationAlgo1>>

  @RawQuery
  fun runRawAlgo3(query: SupportSQLiteQuery): List<OfflineLocalizationAlgo3>
}



// @Entity(tableName = SMAS.DB_FINGERPRINT)
data class TestEntity(
        val oid: Int,
)
