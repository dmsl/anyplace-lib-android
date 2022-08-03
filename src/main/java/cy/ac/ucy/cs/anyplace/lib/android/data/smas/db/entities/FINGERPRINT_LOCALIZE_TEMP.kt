package cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.entities

import androidx.room.Entity
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.SMAS

/**
 * Matching SMAS Backend SQLite Database
 */
@Entity(
        tableName = SMAS.DB_LOCALIZE_TEMP,
        primaryKeys = ["uid", "oid"]) // Composite PK
data class FINGERPRINT_LOCALIZE_TEMP(
        /** Message ID */
        val uid: String,
        val oid: Int,
        val width: Double,
        val height: Double,
        val ocr: String?,
)

data class OfflineLocalizationAlgo1(
        /** Message ID */
        val flid: Int,
        val time: Long,
        val buid: String,
        val x: Double,
        val y: Double,
        val deck: Int,
        val modelid: Int,
)

data class OfflineLocalizationAlgo3(
        /** Message ID */
        val flid: Int,
        val x: Double,
        val y: Double,
        val deck: Int,
        val dissimilarity: Float,
)

// Generic
data class OfflineLocalization(
        /** Message ID */
        val deck: Int,
        val dissimilarity: Float,
        val flid: Int,
        val x: Double,
        val y: Double,
        val buid: String?=null,
)