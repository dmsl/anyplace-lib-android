package cy.ac.ucy.cs.anyplace.lib.android.ui.smas.chat.components

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.SmasApp
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.smas.models.ChatMsg
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.helpers.ChatMsgHelper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.theme.AnyplaceBlue
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.theme.Black
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.theme.White
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.theme.WineRed
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.smas.SmasChatViewModel

/**
 *
 * The main conversation which includes a(n):
 * - [LazyColumn]: for showing the incoming/ already sent messages (scrollable)
 * - [MessageCard]: for each message separately
 * - [DeliveryCard]: for details about where each message is sent
 * - [ReplyCard]: for all the replying functions
 */
@ExperimentalPermissionsApi
@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalMaterialApi
@Composable
fun Conversation(
        app: SmasApp,
        VMchat: SmasChatViewModel,
        manager: FragmentManager,
        repo: RepoSmas,
        returnCoords: (lat: Double, lng: Double, level: Int) -> Unit) {
  Column {
    LazyColumn(
            modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 5.dp),
            //state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            reverseLayout = true // not hiding msgs when keyboard is open
    ) {
      LOG.W(TAG, "LazyColumn: msgsList size: ${app.msgList.size}")

      LOG.D2(TAG, "LazyColumn: resetting new msgs")
      app.setUnreadMsgsState(VMchat.viewModelScope, false)

      if (!app.msgList.isEmpty()) {
        // val lastMsgs = app.msgList.take(5)
        itemsIndexed(app.msgList) { _, message ->
          MessageCard(message, VMchat, manager, repo, returnCoords)
        }
      }
    }
    DeliveryCard(VMchat, manager)
    ReplyCard(VMchat)
  }
}

/**
 *
 *  Information for each message separately. It includes:
 *  - The sender on top of the box
 *  - The actual message (text, image, location, alert) in a box
 *  - The time & date the message was sent
 *  - Any <reply to> information
 *
 */
@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalMaterialApi
@Composable
fun MessageCard(
        message: ChatMsg,
        VMchat: SmasChatViewModel,
        manager: FragmentManager,
        repo: RepoSmas,
        returnCoords: (lat: Double, lng: Double, level: Int) -> Unit
) {
  val senderIsLoggedUser = (VMchat.getLoggedInUser() == message.uid)
  val ctx = LocalContext.current
  val msg = ChatMsgHelper(ctx,repo, message)

  Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = when {
            senderIsLoggedUser -> Alignment.End
            else -> Alignment.Start
          }
  ) {

    val app = VMchat.app
    val senderUsername = message.uid
    // TODO: no reply support in SMAS API (backend DB)
    var reply by remember { mutableStateOf(false) }

    Text(
            text = senderUsername,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold/*, color = senderColor*/
    )
    Spacer(modifier = Modifier.height(3.dp))
    Row {
      Card(
              onClick = {
                reply = !reply
              },
              modifier = Modifier
                      .widthIn(max = 250.dp, min = 50.dp),
              shape = when {
                senderIsLoggedUser -> RoundedCornerShape(10.dp, 0.dp, 10.dp, 10.dp)
                else -> RoundedCornerShape(0.dp, 10.dp, 10.dp, 10.dp)
              },
              elevation = 5.dp,
              backgroundColor = when {
                senderIsLoggedUser -> AnyplaceBlue
                message.mtype == 4 -> WineRed  // when it is an alert
                else -> White
              },
              content = {
                /*TODO*/ // ReplyTo in database
                //val replyMsg = message.replyToMessage
                Column(modifier = Modifier.padding(vertical = 5.dp)) {
                  // if (replyMsg != null) {
                  //     Row(modifier = Modifier.padding(end = 15.dp)) {
                  //         Icon(
                  //             imageVector = Icons.Filled.Reply,
                  //             contentDescription = "reply-icon",
                  //             modifier = Modifier
                  //                 .size(30.dp)
                  //                 .fillMaxHeight()
                  //                 .padding(horizontal = 7.dp),
                  //             tint = when {
                  //                 senderIsLoggedUser -> White
                  //                 else -> AnyplaceBlue
                  //             }
                  //         )
                  //         Column() {
                  //             Text(
                  //                 text = replyMsg.sender,
                  //                 fontWeight = FontWeight.Bold,
                  //                 modifier = Modifier
                  //                     .padding(vertical = 3.dp),
                  //                 color = when {
                  //                     senderIsLoggedUser -> White
                  //                     else -> AnyplaceBlue
                  //                 }
                  //             )
                  //             when {
                  //                 replyMsg.message != null -> replyMsg.message
                  //                 else -> replyMsg.attachment
                  //             }?.let {
                  //                 Text(
                  //                     text = it,
                  //                     color = when {
                  //                         senderIsLoggedUser -> WhiteR
                  //                         else -> AnyplaceBlueR
                  //                     },
                  //                     maxLines = 2, overflow = TextOverflow.Ellipsis
                  //                 )
                  //             }
                  //         }
                  //     }
                  // }
                  // When the message is a normal text message..
                  if (message.mtype == 1 && message.msg != null) {
                    Text(
                            text = message.msg!!,
                            style = MaterialTheme.typography.subtitle1,
                            modifier = Modifier
                                    .padding(vertical = 3.dp, horizontal = 10.dp),
                            color = when {
                              senderIsLoggedUser -> White
                              else -> Black
                            }
                    )
                  }
                  // When the message is an image in base64 encoding..
                  if (msg.isImage() && message.msg != null) {


                    val bitmapImgTiny: Bitmap? = remember {
                      message.msg.let { VMchat.chatCache.readBitmapTiny(message) }
                    }

                    if (bitmapImgTiny != null) {
                      Image(
                              modifier = Modifier
                                      .widthIn(max = 300.dp)
                                      .heightIn(max = 300.dp)
                                      .clickable {
                                        val bitmapImg = VMchat.chatCache.readBitmap(message)
                                        if (bitmapImg != null)
                                          VMchat.openImgDialog(manager, bitmapImg)
                                      },
                              bitmap = bitmapImgTiny.asImageBitmap(),
                              contentDescription = "image"
                      )
                    }
                  }
                  // When the message is either a location or an alert..
                  if (message.mtype == 3 || message.mtype == 4) {
                    val deckInfo = "${app.wSpace.prettyLevel} ${message.deck}"
                    Column {
                      Text(
                              text = when (message.mtype) {
                                3 -> {
                                  var txt = "View on map ($deckInfo)"
                                  if (!message.msg.isNullOrEmpty()) {
                                    txt="${message.msg}\n$txt"
                                  }

                                  txt
                                }
                                else -> "ALERT ($deckInfo)"
                              },
                              style = MaterialTheme.typography.subtitle1,
                              modifier = Modifier
                                      .padding(vertical = 3.dp, horizontal = 10.dp),
                              color = when {
                                senderIsLoggedUser -> White
                                message.mtype == 4 -> White  //When the message is an alert
                                else -> Black
                              }
                      )
                      IconButton(
                              onClick = {
                                returnCoords(message.x, message.y, message.deck)
                              },
                              modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
                      ) {
                        Icon(
                                imageVector = Icons.Filled.Place,
                                contentDescription = "location",
                                modifier = Modifier.size(35.dp),
                                tint = when {
                                  senderIsLoggedUser -> White
                                  message.mtype == 4 -> White // when the message is an alert
                                  else -> AnyplaceBlue
                                }
                        )
                      }
                    }
                  }

                  val sameDay = utlTime.isSameDay(message.timestr)
                  Text(
                          text = when (sameDay) {
                            true -> utlTime.getTimeFromStrFull(message.timestr)
                            else -> {
                              val msgHour = utlTime.getTimeFromStr(message.timestr)
                              val msgDate = utlTime.getDateFromStr(message.timestr)

                              "$msgDate $msgHour"
                            }
                          },
                          fontSize = 10.sp,
                          modifier = Modifier
                                  .padding(vertical = 3.dp, horizontal = 10.dp),
                          color = when {
                            senderIsLoggedUser -> White
                            message.mtype == 4 -> White  //When it is an alert..
                            else -> Black
                          }
                  )
                }
              })

      // if (reply) {
      //     IconButton(
      //         onClick = {
      //             reply = false
      //             if (message.mtype == 1)
      //                 viewModel.replyToMessage =
      //                     ReplyToMessage(senderUsername, message.msg, null)
      //             if (message.mtype == 2)
      //                 viewModel.replyToMessage = ReplyToMessage(senderUsername, null, "Image")
      //             if (message.mtype == 3)
      //                 viewModel.replyToMessage =
      //                     ReplyToMessage(senderUsername, null, "Location")
      //             if (message.mtype == 4)
      //                 viewModel.replyToMessage =
      //                     ReplyToMessage(senderUsername, null, "Alert")
      //         },
      //     ) {
      //         Icon(
      //             imageVector = Icons.Filled.Reply,
      //             contentDescription = "reply",
      //             modifier = Modifier
      //                 .size(25.dp),
      //             tint = MildGray
      //         )
      //     }
      // }

    }
  }
}