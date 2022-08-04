package cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.SMAS

/**
 * DB Entity Based on [CvFingerprintRow]
 * On SMAS this is a view
 */
@Entity(tableName = SMAS.DB_FINGERPRINT)
data class FINGERPRINT(
        @PrimaryKey(autoGenerate = false)
        val foid: Int,
        val flid: Int, val uid: String,
        val time: Long, val timestr: String,
        val buid: String, val x: Double, val y: Double, val deck: Int,
        val modelid: Int,
        val flid1: Int,
        val oid: Int,
        val height: Double, val width: Double,
        val ocr: String?,
)
