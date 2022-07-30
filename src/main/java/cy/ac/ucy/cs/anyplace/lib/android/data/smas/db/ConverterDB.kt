package cy.ac.ucy.cs.anyplace.lib.android.data.smas.db

import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.entities.*
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.smas.models.*
import cy.ac.ucy.cs.anyplace.lib.smas.models.CONSTchatMsg.MTYPE_IMG

class ConverterDB {

  companion object {
    fun chatMsgtoEntity(msg: ChatMsg): ChatMsgEntity {
      // skip saving base64 on SQLite.
      // Those will be stored in [SmasCache] (file cache)
      val content = if (msg.mtype == MTYPE_IMG) " " else msg.msg
      return ChatMsgEntity(
              msg.mid,
              msg.uid,
              msg.mdelivery, msg.mtype,
              content,
              msg.mexten,
              msg.time, msg.timestr,
              msg.x, msg.y, msg.buid, msg.deck)
    }


    fun entityToChatMsg(tuple: ChatMsgEntity): ChatMsg {
      return ChatMsg(
              tuple.mid,
              tuple.uid,
              tuple.mdelivery, tuple.mtype,
              tuple.msg,
              tuple.mexten,
              tuple.time, tuple.timestr,
              tuple.x, tuple.y, tuple.buid, tuple.deck)
    }


    /**
     * Converts chat tuples to a list of messages
     */
    fun entityToChatMessages(tuples: List<ChatMsgEntity>): ChatMsgsResp {
      val spaces = mutableListOf<ChatMsg>()
      tuples.forEach { tuple ->
        spaces.add(entityToChatMsg(tuple))
      }
      return ChatMsgsResp(null, "msgs read locally", null, spaces)
    }

    fun cvModelClassToEntity(cvc: CvModelClass): CvModelClassEntity {
      // skip saving base64 on SQLite.
      // Those will be stored in [SmasCache] (file cache)
      // val content = if (msg.mtype == TP_SEND_IMG) " " else msg.msg
      return CvModelClassEntity(
              cvc.oid,
              cvc.cid,
              cvc.modeldescr,
              cvc.modelid,
              cvc.name)
    }

    fun entityToCvModelClass(e: CvModelClassEntity): CvModelClass {
      return CvModelClass(
              e.oid,
              e.cid,
              e.modeldescr,
              e.modelid,
              e.name)
    }

    fun cvMapRowToEntity(c: CvMapRow): CvMapRowEntity {
      return CvMapRowEntity(
              c.foid, c.flid, c.uid,
              c.time,  c.timestr,
              c.buid,  c.x, c.y, c.deck,
              c.modelid,
              c.flid1,
              c.oid,
              c.height, c.width, c.ocr
      )
    }

    fun entityToCvMapRow(c: CvMapRowEntity): CvMapRow {
      return CvMapRow(
              c.foid, c.flid, c.uid,
              c.time, c.timestr,
              c.buid, c.x, c.y, c.deck,
              c.modelid,
              c.flid1,
              c.oid,
              c.height, c.width, c.ocr
      )
    }

    fun localizationFingerprintTempToEntity(uid: String, c: CvObjectReq): FINGERPRINT_LOCALIZE_TEMP {
      return FINGERPRINT_LOCALIZE_TEMP(uid, c.oid, c.height, c.width, c.ocr)
    }

    fun convertToGeneric(lr: OfflineLocalizationAlgo1) : OfflineLocalization {
      return OfflineLocalization(lr.deck, 0f, lr.flid, lr.x, lr.y, lr.buid)
    }

    fun convertToGeneric(lr: OfflineLocalizationAlgo3) : OfflineLocalization {
      return OfflineLocalization(lr.deck, lr.dissimilarity, lr.flid, lr.x, lr.y)
    }

    /**
     * NOT USED
     */
    fun entityToCvModelClassesResp(tuples: List<CvModelClassEntity>): CvModelsResp {
      return CvModelsResp(entityToCvModelClasses(tuples), NetworkResult.DB_LOADED , "db", null)
    }

    /**
     * Converts chat tuples to a list of [CvModelClass]es
     */
    fun entityToCvModelClasses(tuples: List<CvModelClassEntity>): List<CvModelClass>{
      val list = mutableListOf<CvModelClass>()
      tuples.forEach {  list .add(entityToCvModelClass(it))  }
      return list
    }

  }

  val gson = Gson() // Kotlin Serialization?
  @TypeConverter
  fun ltnLngToString(ltnLng: LatLng) : String {
    return gson.toJson(ltnLng)
  }

  @TypeConverter
  fun stringToLtnLng(data: String)  : LatLng {
    val listType = object : TypeToken<LatLng>() {}.type
    return gson.fromJson(data, listType)
  }

}
