package cy.ac.ucy.cs.anyplace.lib.android.data.smas.source

import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.SmasDAO
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.entities.ChatMsgEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.entities.CvModelClassEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.entities.DatabaseConverters.Companion.chatMsgtoEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.entities.DatabaseConverters.Companion.cvModelClassToEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.entities.DatabaseConverters.Companion.entityTooCvModelClasses
import cy.ac.ucy.cs.anyplace.lib.smas.models.ChatMsg
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.helpers.ChatMsgHelper
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

  suspend fun insertCvModelClass(o: CvModelClass) {
    LOG.D4("DB: insert: CvModelClass: ${o.cid}: ${o.name}")
    DAO.insertCvModelClass(cvModelClassToEntity(o))
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

  fun hasCvModelClassesDownloaded() : Boolean {
    val cnt = DAO.countCvModelClasses()
    return cnt!=null && cnt>0
  }

  suspend fun readCvModelClasses(modelId: Int): List<CvModelClass> {
    return entityTooCvModelClasses(DAO.readCvModelClasses(modelId).first())
  }

  fun dropCvModelClasses() {
    LOG.D2(TAG, "deleting all Cv Models")
    DAO.dropCvModelClasses()
  }

}
