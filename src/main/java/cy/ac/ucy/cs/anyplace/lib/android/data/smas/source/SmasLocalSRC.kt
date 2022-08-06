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
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.wrappers.ChatMsgWrapper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.notify
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.HeatmapWeights
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvViewModel
import cy.ac.ucy.cs.anyplace.lib.smas.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * SMAS Local Data Store. (local = database on device)
 * It's a wrapper on top of ROOM (which manages SQLite)
 */
class SmasLocalSRC @Inject constructor(private val DAO: SmasDAO) {
  private val TG = "ds-local-smas"

  fun readMsgs(): Flow<List<ChatMsgEntity>> {
    return DAO.readMsgs()
  }

  suspend fun insertMsg(msg: ChatMsg) {
    val MT = ::insertMsg.name
    LOG.D4("$MT: DB: insert: ${msg.mid}: ${ChatMsgWrapper.content(msg)}")
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
    val cnt = DAO.countCvFingerprintRows()
    return cnt!=null && cnt>0
  }

  fun getCvFingerprintsHeatmapWeights(modelid: Int, deck: Int) : List<HeatmapWeights> {
    val MT = ::getCvFingerprintsHeatmapWeights.name
    LOG.W(TG, "$MT: modelid: $modelid deck: $deck")
    return DAO.getCvFingerprintsHeatmapWeights(modelid, deck)
  }

  fun dropCvFingerprints() {
    val MT = ::dropCvFingerprints.name
    LOG.D2(TG, "$MT: deleting CvMap")
    DAO.dropCvFingerprints()
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
    val algoRequested = app.dsCvMap.read.first().cvAlgoChoice
    var algoChosen = algoRequested
    var algoRequestedPretty = algoRequested

    if (algoChosen == VM.C.CV_ALGO_CHOICE_AUTO) algoRequestedPretty="auto"

    // without prev coords we have to run GLOBAL (ALGO3)
    if (prevCoord == null) {
      algoChosen=VM.C.CV_ALGO_CHOICE_GLOBAL
    // otherwise, and if it's auto, prefer the LOCAL (ALGO4)
    } else if (algoChosen == VM.C.CV_ALGO_CHOICE_AUTO) {
      algoChosen=VM.C.CV_ALGO_CHOICE_LOCAL
    }
    // if it was LOCAL, it would have stayed LOCAL
    // if it was GLOBAL, it would have stayed GLOBAL (given prevCoords not null)

    // RUN THE QUERY: ALGO3 or ALGO4
    val result = if (algoChosen == VM.C.CV_ALGO_CHOICE_GLOBAL) {
      LOG.E(TG, "RUNNING ALGO 3")
      DAO.runRawAlgo(smasQueries.global_algo3(uid, modelid, buid))
    } else { // run global
      LOG.E(TG, "RUNNING ALGO 4")
      val prev=prevCoord!!
      DAO.runRawAlgo(smasQueries.local_algo4(
              prev.lat, prev.lon, prev.level,
              uid, modelid, buid))
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
      strInfo+="$algoChosen "
      if (algoChosen!=algoRequested) strInfo+= "(requested ${algoRequestedPretty})"
      val devMsg = if (msg.isNotEmpty()) "$msg\n$strInfo" else strInfo
      VM.notify.longDEV(VM.viewModelScope, devMsg)
    } else if (msg.isNotEmpty()) {
      VM.notify.long(VM.viewModelScope, msg)
    }
  }
}
