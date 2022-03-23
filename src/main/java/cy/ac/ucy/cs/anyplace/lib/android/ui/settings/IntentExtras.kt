package cy.ac.ucy.cs.anyplace.lib.android.ui.settings

import android.app.Activity
import android.os.Bundle
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.models.helpers.FloorHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.models.helpers.FloorsHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.models.helpers.SpaceHelper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD

object IntentExtras {

  fun getSpace(activity: Activity, repo: RepoAP, extras: Bundle?, key: String) : SpaceHelper? {
    val spaceStr = extras?.getString(key)
    return if (spaceStr != null) {
      val space = SpaceHelper.parse(spaceStr)
      SpaceHelper(activity, repo, space)
    } else {
      LOG.W(TAG_METHOD, "No space given.")
      null
    }
  }

  fun getFloors(spaceH: SpaceHelper?, extras: Bundle?, key: String) : FloorsHelper? {
    val floorsStr = extras?.getString(key)
    return if (spaceH != null && floorsStr != null) {
      val floors = FloorsHelper.parse(floorsStr)
      FloorsHelper(floors, spaceH)
    } else {
      LOG.W(TAG_METHOD, "No floors given.")
      null
    }
  }

  fun getFloor(spaceH: SpaceHelper?, extras: Bundle?, key: String) : FloorHelper? {
    val floorStr = extras?.getString(key)
    return if (spaceH != null && floorStr != null) {
      val floor = FloorHelper.parse(floorStr)
      FloorHelper(floor, spaceH)
    } else {
      LOG.W(TAG_METHOD, "No floor given.")
      null
    }
  }

}