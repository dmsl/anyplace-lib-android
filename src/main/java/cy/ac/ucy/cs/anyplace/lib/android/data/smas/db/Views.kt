package cy.ac.ucy.cs.anyplace.lib.android.data.smas.db

import androidx.room.DatabaseView
import androidx.room.Entity
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.SMAS

/**
 * OBJECT: it's CHAT.DB_SMAS_CVMODELS
 */
@DatabaseView("SELECT O.*, COUNT(*) AS object_count\n" +
        "FROM (\n" +
        "SELECT F.*, O.*\n" +
        "FROM ${SMAS.DB_FINGERPRINT} F, ${SMAS.DB_OBJECT} O\n" +
        "WHERE F.oid = O.oid\n" +
        "GROUP BY F.oid, F.x, F.y, F.deck\n" +
        "-- ORDER BY  O.oid ASC;\n" +
        ") AS A, ${SMAS.DB_OBJECT} O\n" +
        "WHERE A.oid = O.oid\n" +
        "GROUP BY A.oid;")
data class OBJECT_COUNT(
        val oid: Int,
        val cid: Int,
        val modeldescr: String,
        val modelid: Int,
        val name: String,
        val object_count: Int,
)


@DatabaseView("SELECT O.modelid, SUM(O.object_count) AS model_sum\n" +
        "FROM OBJECT_COUNT O\n" +
        "GROUP BY O.modelid;")
data class MODEL_SUM(
        val _id: Int,
        val modelid: Int,
        val model_sum: Int,
)

@Entity(tableName = SMAS.DB_OBJECT_FREQUENCY)
@DatabaseView("SELECT O.oid, O.cid, O.modelid, O.modeldescr, O.name, " +
        "(CAST(O.object_count as real) / CAST(MS.model_sum as real)) as weight\n" +
        "        FROM OBJECT_COUNT O, MODEL_SUM MS\n" +
        "        WHERE  O.modelid=MS.modelid;")
data class OBJECT_FREQUENCY(
        val _id: Int,
        val oid: Int,
        val cid: Int,
        val modelid: Int,
        val modeldescr: String,
        val name: String,
        val weight: Float,
)
