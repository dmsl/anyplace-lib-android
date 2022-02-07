package cy.ac.ucy.cs.anyplace.lib.android.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.preference.PreferenceDataStore
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Computer Vision DataStore
 * TODO SETTINGS:
 * - image
 */
@Singleton
class DataStoreCv @Inject constructor(@ApplicationContext private val ctx: Context)
  : PreferenceDataStore() {

  private val C by lazy { CONST(ctx) }
  private val Context.dataStoreCv by preferencesDataStore(name = C.PREF_CV)

  // TODO:TRIAL
  private val validKeys = setOf(
          C.PREF_MODEL_NAME,
          C.PREF_RELOAD_MODEL,
          C.PREF_RELOAD_CVMAPS,
          C.PREF_RELOAD_FLOORPLAN,
  )

  private class Keys(c: CONST) {
    val modelName= stringPreferencesKey(c.PREF_MODEL_NAME)
    val reloadCvMaps = booleanPreferencesKey(c.PREF_RELOAD_CVMAPS)
    val reloadFloorplans = booleanPreferencesKey(c.PREF_RELOAD_FLOORPLAN)
    val reloadModel = booleanPreferencesKey(c.PREF_RELOAD_MODEL)
  }
  private val KEY = Keys(C)

  val datastore = ctx.dataStoreCv

  private fun validKey(key: String?): Boolean {
    val found = validKeys.contains(key)
    if(!found) LOG.W(TAG, "Unknown key: $key")
    return found
  }

  override fun putBoolean(key: String?, value: Boolean) {
    if (!validKey(key)) return
    runBlocking {
      datastore.edit {
        when (key) {
          C.PREF_RELOAD_MODEL -> it[KEY.reloadModel] = value
          C.PREF_RELOAD_CVMAPS-> it[KEY.reloadCvMaps] = value
          C.PREF_RELOAD_FLOORPLAN-> it[KEY.reloadFloorplans] = value
        }
      }
    }
  }

  override fun putString(key: String?, value: String?) {
    LOG.D3(TAG, "putString: $key = $value")
    if (!validKey(key)) return
    runBlocking {
      datastore.edit {
        when (key) {
          C.PREF_MODEL_NAME -> it[KEY.modelName] = value ?: C.DEFAULT_PREF_MODEL_NAME
        }
      }
    }
  }

  override fun getBoolean(key: String?, defValue: Boolean): Boolean {
    if (!validKey(key)) return false
    return runBlocking(Dispatchers.IO) {
      val prefs = read.first()
      return@runBlocking when (key) {
        C.PREF_RELOAD_MODEL -> prefs.reloadModel
        C.PREF_RELOAD_CVMAPS -> prefs.reloadCvMaps
        C.PREF_RELOAD_FLOORPLAN -> prefs.reloadFloorplan
        else -> false
      }
    }
  }

  override fun getString(key: String?, defValue: String?): String? {
    if (!validKey(key)) return null
    return runBlocking(Dispatchers.IO) {
      val prefs = read.first()
      return@runBlocking when (key) {
        C.PREF_MODEL_NAME -> prefs.modelName
        else -> null
      }
    }
  }

  fun setModelName(value: String) = putString(C.PREF_MODEL_NAME, value)
  fun setReloadModel(value: Boolean) = putBoolean(C.PREF_RELOAD_MODEL, value)
  fun setReloadCvMaps(value: Boolean) = putBoolean(C.PREF_RELOAD_CVMAPS, value)
  fun setReloadFloorplan(value: Boolean) = putBoolean(C.PREF_RELOAD_FLOORPLAN, value)

  val read: Flow<CvActivitiesPrefs> = ctx.dataStoreCv.data
          .catch { exception ->
            if (exception is IOException) {
              emit(emptyPreferences())
            } else { throw exception }
          }
          .map { preferences ->
            val modelName = preferences[KEY.modelName] ?: C.DEFAULT_PREF_MODEL_NAME
            val reloadModel = preferences[KEY.reloadModel] ?: false
            val reloadCvMaps= preferences[KEY.reloadCvMaps] ?: false
            val reloadFloorplan = preferences[KEY.reloadFloorplans] ?: false

            CvActivitiesPrefs(modelName, reloadModel, reloadCvMaps, reloadFloorplan)
          }
}

data class CvActivitiesPrefs(
        val modelName: String,
        val reloadModel: Boolean,
        val reloadCvMaps: Boolean,
        val reloadFloorplan: Boolean,
)
