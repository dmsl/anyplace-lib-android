package cy.ac.ucy.cs.anyplace.lib.android.ui.settings.base

import android.app.Activity
import android.os.Bundle
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.LevelWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.LevelsWrapper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.SpaceWrapper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD

object IntentExtras {

  fun getSpace(activity: Activity, repo: RepoAP, extras: Bundle?, key: String) : SpaceWrapper? {
    val spaceStr = extras?.getString(key)
    return if (spaceStr != null) {
      val space = SpaceWrapper.parse(spaceStr)
      SpaceWrapper(activity, repo, space)
    } else {
      LOG.W(TAG_METHOD, "No space given.")
      null
    }
  }

  fun getFloors(spaceH: SpaceWrapper?, extras: Bundle?, key: String) : LevelsWrapper? {
    val floorsStr = extras?.getString(key)
    return if (spaceH != null && floorsStr != null) {
      val floors = LevelsWrapper.parse(floorsStr)
      LevelsWrapper(floors, spaceH)
    } else {
      LOG.W(TAG_METHOD, "No floors given.")
      null
    }
  }

  fun getFloor(spaceH: SpaceWrapper?, extras: Bundle?, key: String) : LevelWrapper? {
    val floorStr = extras?.getString(key)
    return if (spaceH != null && floorStr != null) {
      val floor = LevelWrapper.parse(floorStr)
      LevelWrapper(floor, spaceH)
    } else {
      LOG.W(TAG_METHOD, "No floor given.")
      null
    }
  }

}