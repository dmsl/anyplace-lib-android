package cy.ac.ucy.cs.anyplace.lib.android.data.smas.source

import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.SmasDAO
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
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
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.nw.CvLocalizeNW
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.User
import cy.ac.ucy.cs.anyplace.lib.smas.models.CvObjectReq
import cy.ac.ucy.cs.anyplace.lib.smas.models.CvMapRow
import cy.ac.ucy.cs.anyplace.lib.smas.models.CvModelClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SmasLocalDS @Inject constructor(private val DAO: SmasDAO) {

  fun readMsgs(): Flow<List<ChatMsgEntity>> {
    return DAO.readMsgs()
  }

  suspend fun insertMsg(msg: ChatMsg) {
    LOG.D4("DB: insert: ${msg.mid}: ${ChatMsgHelper.content(msg)}")
    DAO.insertChatMsg(chatMsgtoEntity(msg))
  }

  fun dropMsgs() {
    LOG.D2(TAG, "deleting all msgs")
    DAO.dropMsgs()
  }

  fun hasMsgs() : Boolean {
    val cnt = DAO.countMsgs()
    return cnt!=null && cnt>0
  }

  /**
   * Get last msg timestamp from local DB
   */
  fun getLastMsgTimestamp(): Long? {
    return DAO.lastMsgTimestamp()
  }

  suspend fun insertCvModelClass(o: CvModelClass) {
    LOG.D4("DB: insert: CvModelClass: ${o.cid}: ${o.name}")
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
    LOG.D2(TAG, "deleting all Cv Models")
    DAO.dropCvModelClasses()
  }

  fun getCvModelIds() : List<Int> = DAO.getModelIds()

  suspend fun insertCvMapRow(o: CvMapRow) {
    LOG.D2("DB: insert: CvModelClass: ${o.flid}: ${o.buid}")
    DAO.insertCvMapRow(cvMapRowToEntity(o))
  }

  fun hasCvMap() : Boolean {
    val cnt = DAO.countCvMapRows()
    return cnt!=null && cnt>0
  }

  fun dropCvMap() {
    LOG.D2(TAG, "deleting CvMap")
    DAO.dropCvMap()
  }

  suspend fun localize(VM: CvViewModel, modelid: Int, buid: String, detectionsReq: List<CvObjectReq>, chatUser: User) {
    // Preparation: fill tmp array with scans
    DAO.dropLocalizeFpTemp()
    detectionsReq.forEach {
      DAO.insertLocalizeTemp(localizationFingerprintTempToEntity(chatUser.id, it))
    }

    val strInfo = "Recognitions: ${detectionsReq.size}. Algo: Offline"
    // detections.forEach { strInfo+= "${it.oid} " }

    val res3 = DAO.localizeAlgo3(DAO.getQueryAlgo3(modelid, buid))
    var msg =""
    if (res3.isNotEmpty()) {
      val loc3 = res3[0]
      LOG.W(TAG, "${CvLocalizeNW.tag}: ALGO3: $loc3")
      VM.nwCvLocalize.postOfflineResult(convertToGeneric(loc3))
    } else {
      msg+="Offline algorithm returned no results"
    }

    if (VM.app.hasDevMode()) {
      val devMsg = if (msg.isNotEmpty()) "$msg\n$strInfo" else strInfo
      VM.app.showSnackbarLongDEV(VM.viewModelScope, devMsg)
    } else if (msg.isNotEmpty()) {
      VM.app.showSnackbarLong(VM.viewModelScope, msg)
    }
  }
}
