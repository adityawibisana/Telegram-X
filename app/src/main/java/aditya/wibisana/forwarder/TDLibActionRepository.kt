package aditya.wibisana.forwarder

import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Send message to targetUserId.
 * Throw error if failed to send message
 */
suspend fun Client.sendMessage(targetUserId: Long, messageText: String) = suspendCoroutine {
  // Create the message content
  val inputMessageText = TdApi.InputMessageText(
    TdApi.FormattedText(messageText, null),
    null,
    false)

  val sendMessage = TdApi.SendMessage(
    targetUserId,
    0,
    null,
    null,
    null,
    inputMessageText)
  // Send the createChat and sendMessage requests
  send(TdApi.CreatePrivateChat(targetUserId, true)) { result ->
    if (result is TdApi.Chat) {
      // Chat created successfully, now send the message
      send(sendMessage) { sendMessageResult ->
        if (sendMessageResult is TdApi.Message) {
          it.resume(sendMessageResult)
        } else {
          it.resumeWithException(Error("Not a TdApi.Message"))
        }
      }
    } else {
      it.resumeWithException(Error("Not a TdApi.Chat"))
    }
  }
}