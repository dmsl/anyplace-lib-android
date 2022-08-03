package cy.ac.ucy.cs.anyplace.lib.android.data.smas.source

import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.SmasDAO
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.entities.ChatMsgEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.ConverterDB.Companion.chatMsgtoEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.ConverterDB.Companion.convertToGeneric
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.ConverterDB.Companion.cvMapRowToEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.ConverterDB.Companion.cvModelClassToEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.ConverterDB.Companion.entityToCvModelClasses
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.ConverterDB.Companion.localizationFingerprintTempToEntity
import cy.ac.ucy.cs.anyplace.lib.smas.models.ChatMsg
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.helpers.ChatMsgHelper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.notify
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.TrackingMode
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.UserAP
import cy.ac.ucy.cs.anyplace.lib.smas.models.CvObjectReq
import cy.ac.ucy.cs.anyplace.lib.smas.models.CvMapRow
import cy.ac.ucy.cs.anyplace.lib.smas.models.CvModelClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Smas Local DataStore (uses SQLite)
 */
class SmasLocalDS @Inject constructor(private val DAO: SmasDAO) {
  private val TG = "ds-local-smas"

  fun readMsgs(): Flow<List<ChatMsgEntity>> {
    return DAO.readMsgs()
  }

  suspend fun insertMsg(msg: ChatMsg) {
    val MT = ::insertMsg.name
    LOG.D4("$MT: DB: insert: ${msg.mid}: ${ChatMsgHelper.content(msg)}")
    DAO.insertChatMsg(chatMsgtoEntity(msg))
  }

  fun dropMsgs() {
    val MT = ::dropMsgs.name
    LOG.D2(TG, "$MT: deleting all msgs")
    DAO.dropMsgs()
  }

  fun hasMsgs() : Boolean {
    val cnt = DAO.countMsgs()
    return cnt!=null && cnt>0
  }

  /** Get last msg timestamp from local DB */
  fun getLastMsgTimestamp(): Long? {
    return DAO.lastMsgTimestamp()
  }

  suspend fun insertCvModelClass(o: CvModelClass) {
    val MT = ::insertCvModelClass.name
    LOG.D4(TG, "$MT DB: insert: CvModelClass: ${o.cid}: ${o.name}")
    DAO.insertCvModelClass(cvModelClassToEntity(o))
  }

  fun hasCvModelClassesDownloaded() : Boolean {
    val cnt = DAO.countCvModelClasses()
    return cnt!=null && cnt>0
  }

  suspend fun readCvModelClasses(modelId: Int): List<CvModelClass> {
    return entityToCvModelClasses(DAO.readCvModelClasses(modelId).first())
  }

  fun dropCvModelClasses() {
    val MT = ::dropCvModelClasses.name
    LOG.D2(TG, "$MT: deleting all Cv Models")
    DAO.dropCvModelClasses()
  }

  fun getCvModelIds() : List<Int> = DAO.getModelIds()

  suspend fun insertCvFingerprintRow(o: CvMapRow) {
    val MT = ::insertCvFingerprintRow.name
    LOG.D2("$MT: DB: insert: CvModelClass: ${o.flid}: ${o.buid}")
    DAO.insertCvMapRow(cvMapRowToEntity(o))
  }

  fun hasCvFingerprints() : Boolean {
    val cnt = DAO.countCvMapRows()
    return cnt!=null && cnt>0
  }

  fun dropCvFingerprints() {
    val MT = ::dropCvFingerprints.name
    LOG.D2(TG, "$MT: deleting CvMap")
    DAO.dropCvMap()
  }

  /** Get last fingerprint timestamp from local DB */
  fun getLastFingerprintTimestamp(): Long? {
    return DAO.lastFingerprintTimestamp()
  }

  suspend fun localize(VM: CvViewModel, modelid: Int, buid: String, detectionsReq: List<CvObjectReq>, chatUserAP: UserAP) {
    val MT = ::localize.name
    // Preparation: fill tmp array with scans
    DAO.dropLocalizeFpTemp()
    detectionsReq.forEach {
      DAO.insertLocalizeTemp(localizationFingerprintTempToEntity(chatUserAP.id, it))
    }

    val strInfo = "Recognitions: ${detectionsReq.size}. Algo: Offline"
    // detections.forEach { strInfo+= "${it.oid} " }

    val res3 = DAO.localizeAlgo3(DAO.getQueryAlgo3(modelid, buid))
    var msg =""
    if (res3.isNotEmpty()) {
      val loc3 = res3[0]
      LOG.W(TG, "$MT: ALGO3: $loc3")
      VM.nwCvLocalize.postOfflineResult(convertToGeneric(loc3))
    } else {
      msg+="Offline algorithm returned no results"
    }

    val tracking = VM.trackingMode.first() == TrackingMode.on
    if ((VM.app.hasDevMode() && !tracking) || msg.isNotEmpty()) {
      val devMsg = if (msg.isNotEmpty()) "$msg\n$strInfo" else strInfo
      VM.notify.longDEV(VM.viewModelScope, devMsg)
    } else if (msg.isNotEmpty()) {
      VM.notify.long(VM.viewModelScope, msg)
    }
  }
}
