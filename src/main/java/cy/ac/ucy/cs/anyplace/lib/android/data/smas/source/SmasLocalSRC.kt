package cy.ac.ucy.cs.anyplace.lib.android.data.smas.source

import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.SmasDAO
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.entities.ChatMsgEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.ConverterDB.Companion.chatMsgtoEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.ConverterDB.Companion.convertToGeneric
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.ConverterDB.Companion.cvMapRowToEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.ConverterDB.Companion.cvModelClassToEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.ConverterDB.Companion.entityToCvModelClasses
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.ConverterDB.Companion.localizationFingerprintTempToEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.smasQueries
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.helpers.ChatMsgHelper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.notify
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.smas.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Smas Local DataStore (uses SQLite)
 */
class SmasLocalSRC @Inject constructor(private val DAO: SmasDAO) {
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

  suspend fun insertCvFingerprintRow(o: CvFingerprintRow) {
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

  suspend fun localize(app: AnyplaceApp, VM: CvViewModel, modelid: Int, buid: String,
                       detectionsReq: List<CvObjectReq>, user: SmasUser) {
    val MT = ::localize.name

    // Preparation: fill tmp array with scans
    DAO.dropLocalizeFpTemp()
    detectionsReq.forEach {
      DAO.insertLocalizeTemp(localizationFingerprintTempToEntity(user.uid, it))
    }

    val uid = user.uid

    val prevCoord  = app.locationSmas.value.coord
    val requestedAlgo = app.dsCvMap.read.first().cvAlgoChoice
    var algoChosen = requestedAlgo
    var requestedAlgoStr = requestedAlgo

    if (algoChosen == VM.C.CV_ALGO_CHOICE_AUTO) requestedAlgoStr="auto"
    /*
     * Whether it's AUTO or LOCAL, if there is no prev record, then we have to do global
     * Basically there is no AUTO in offline mode, as LOCAL requires a prev position
     */
    if (algoChosen == VM.C.CV_ALGO_CHOICE_AUTO || algoChosen == VM.C.CV_ALGO_EXEC_LOCAL) {
      algoChosen = if (prevCoord != null) { // has prev coordinates: run local
        VM.C.CV_ALGO_CHOICE_LOCAL
      } else { // otherwise: global
        VM.C.CV_ALGO_CHOICE_GLOBAL
      }
    }

    val result = if (algoChosen == VM.C.CV_ALGO_CHOICE_LOCAL) { // run local
      LOG.E(TG, "RUNNING ALGO 3")
      DAO.runRawAlgo3(smasQueries.algo3(uid, modelid, buid))
    } else { // run global
      LOG.E(TG, "SHOULD RUN ALGO 4")
      DAO.runRawAlgo3(smasQueries.algo3(uid, modelid, buid))
    }

    var msg =""
    if (result.isNotEmpty()) {
      val location = result[0]
      LOG.W(TG, "$MT: ALGO: $algoChosen $location")
      VM.nwCvLocalize.postOfflineResult(convertToGeneric(location))
    } else {
      msg+="Offline algorithm returned no results"
    }

    if ((VM.app.hasDevMode() && !VM.isTracking()) || msg.isNotEmpty()) {
      var strInfo = "Recognitions: ${detectionsReq.size}. \nAlgo: Offline: "
      strInfo+= "(requested ${requestedAlgoStr})"
      val devMsg = if (msg.isNotEmpty()) "$msg\n$strInfo" else strInfo
      VM.notify.longDEV(VM.viewModelScope, devMsg)
    } else if (msg.isNotEmpty()) {
      VM.notify.long(VM.viewModelScope, msg)
    }
  }
}
