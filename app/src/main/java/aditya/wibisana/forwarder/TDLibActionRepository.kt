package aditya.wibisana.forwarder

import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

/**
 * Send message to targetUserId.
 * return TdApi.message if success, and null if error
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
          it.resume(null)
          println("Not a TdApi.Message")
        }
      }
    } else {
      it.resume(null)
      println("Not a TdApi.Message")
    }
  }
}

suspend fun Client.forwardMessage(targetUserId: Long, messageId: Long, chatId: Long, messageThreadId: Long = 0) = suspendCoroutine {
  // Create the forwardMessage request
  val forwardMessage = TdApi.ForwardMessages(
    targetUserId, //Identifier of the chat to which to forward messages.
    messageThreadId, // messageThreadId If not 0, the message thread identifier in which the message will be sent; for forum threads only.
    chatId, // fromChatId Identifier of the chat from which to forward messages.
    longArrayOf(messageId), // messageIds Identifiers of the messages to forward. Message identifiers must be in a strictly increasing order. At most 100 messages can be forwarded simultaneously. A message can be forwarded only if message.canBeForwarded.
    TdApi.MessageSendOptions(
      true, // disableNotification
      true, // fromBackground
      false, // protectContent
      false, // updateOrderOfInstalledStickerSets
      null, // schedulingState. null == immediate sending
      Random.nextInt(), // messageIdentifier
      false), // options Options to be used to send the messages; pass null to use default options.
    false, // sendCopy Pass true to copy content of the messages without reference to the original sender. Always true if the messages are forwarded to a secret chat or are local.
    false, // removeCaption Pass true to remove media captions of message copies. Ignored if sendCopy is false.
  )

  // Send the forwardMessage request
  send(forwardMessage) { forwardResults ->
    if (forwardResults is TdApi.Messages) {
      it.resume(forwardResults)
      return@send
    }
    println("Not returning TdApi.Messages. Should not happened")
    it.resume(null)
  }
}