package cy.ac.ucy.cs.anyplace.lib.android.data.smas.db.entities

import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import cy.ac.ucy.cs.anyplace.lib.smas.models.ChatMsg
import cy.ac.ucy.cs.anyplace.lib.smas.models.ChatMsgsResp
import cy.ac.ucy.cs.anyplace.lib.smas.models.CONSTchatMsg.TP_SEND_IMG
import cy.ac.ucy.cs.anyplace.lib.smas.models.CvModelClass
import cy.ac.ucy.cs.anyplace.lib.smas.models.CvModelsResp

class DatabaseConverters {

  companion object {
    fun chatMsgtoEntity(msg: ChatMsg): ChatMsgEntity {
      // skip saving base64 on SQLite.
      // Those will be stored in [SmasCache] (file cache)
      val content = if (msg.mtype == TP_SEND_IMG) " " else msg.msg
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

    /**
     * NOT USED
     */
    fun entityTooCvModelClassesResp(tuples: List<CvModelClassEntity>): CvModelsResp {
      return CvModelsResp(entityTooCvModelClasses(tuples), NetworkResult.DB_LOADED , "db", null)
    }

    /**
     * Converts chat tuples to a list of [CvModelClass]es
     */
    fun entityTooCvModelClasses(tuples: List<CvModelClassEntity>): List<CvModelClass>{
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
