package cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db

import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.SpaceEntity
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.db.entities.UserOwnership
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Space
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.Spaces

class SpaceTypeConverter {

  companion object {
    fun spaceToEntity(space: Space, ownership: UserOwnership): SpaceEntity {
      return SpaceEntity(
        space.id,
        space.type.uppercase(),
        space.bucode,
        space.name,
        space.description,
        space.address,
        space.coordinatesLat,
        space.coordinatesLon,
        space.url,
        ownership)
    }

    private fun entityToSpace(tuple: SpaceEntity): Space {
      val entity = Space(
        tuple.id,
        tuple.type.lowercase(),
        tuple.bucode?:"",
        tuple.name,
        tuple.description?:"",
        tuple.address?:"",
        tuple.coordinatesLat,
        tuple.coordinatesLon,
        tuple.url?:"",
      )
      return entity
    }

    /**
     * Convert a list of tuples (entities) to Spaces (that has a list of Space)
     */
    fun entityToSpaces(tuples: List<SpaceEntity>): Spaces {
      val spaces = mutableListOf<Space>()
      tuples.forEach { tuple ->
        spaces.add(entityToSpace(tuple))
      }
      return Spaces(spaces)
    }
  }

  val gson = Gson() // TODO replace with KotlinX Serialization?
   @TypeConverter
   fun ltnLngToString(ltnLng: LatLng) : String { // SHECK multiple spaces here?!
     return gson.toJson(ltnLng)
   }

   @TypeConverter
  fun stringToLtnLng(data: String)  : LatLng {
     val listType = object : TypeToken<LatLng>() {}.type
    return gson.fromJson(data, listType)
  }

}
