package cy.ac.ucy.cs.anyplace.lib.android.ui.smas.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.theme.AnyplaceBlue
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.theme.White
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasChatViewModel
import cy.ac.ucy.cs.anyplace.lib.smas.models.CONSTchatMsg.MDELIVERY_ALL
import cy.ac.ucy.cs.anyplace.lib.smas.models.CONSTchatMsg.MDELIVERY_BBOX
import cy.ac.ucy.cs.anyplace.lib.smas.models.CONSTchatMsg.MDELIVERY_KNN
import cy.ac.ucy.cs.anyplace.lib.smas.models.CONSTchatMsg.MDELIVERY_SAME_DECK

/**
 * A banner which shows information about where the messages are delivered to:
 * - All users
 * - Same deck users
 * - Nearest users
 * - Users in 100m
 * When clicked, the [MsgDeliveryDialog] is shown.
 */
@Composable
fun DeliveryCard(VMchat: SmasChatViewModel, manager: FragmentManager) {
  VMchat.setDeliveryMethod()
  val mdelivery = VMchat.mdelivery

  Row(
          modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = 5.dp)
                  .padding(horizontal = 5.dp)
                  .background(Color.Transparent)
                  .clickable {
                    VMchat.openMsgDeliveryDialog(manager)
                  },
          horizontalArrangement = Arrangement.Center
  ) {

    val app = VMchat.app

    Card(modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(10.dp))
            .border(Dp.Hairline, AnyplaceBlue, RoundedCornerShape(5.dp))) {
      Row {
        Text(text = "Messages are delivered to ",
                textAlign = TextAlign.Center,
                modifier = Modifier
                        .padding(vertical = 10.dp)
                        .padding(start = 10.dp))

        Text(
                text = when (mdelivery.toInt()) {
                  MDELIVERY_ALL -> "ALL USERS."
                  MDELIVERY_SAME_DECK -> "SAME ${app.wSpace.prettyFloorAllCaps} USERS."
                  MDELIVERY_KNN -> "NEAREST USERS."
                  MDELIVERY_BBOX -> "USERS IN 100M."
                  else -> "error"
                },
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                        .padding(vertical = 10.dp)
                        .padding(end = 10.dp),
                color = AnyplaceBlue
        )
      }
    }
  }
}