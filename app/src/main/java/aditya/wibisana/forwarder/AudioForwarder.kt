package aditya.wibisana.forwarder

import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.ForwardMessages
import org.drinkless.tdlib.TdApi.MessageVoiceNote
import org.thunderdog.challegram.data.TD
import org.thunderdog.challegram.telegram.GlobalMessageListener
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.telegram.TdlibManager

object AudioForwarder : GlobalMessageListener  {
  @Suppress("SpellCheckingInspection")
  private const val VOICEHOTKEYBOT = 6215296775
  private lateinit var context: TdlibManager

  private val forwardedList = mutableListOf<MessageForwardModel>()

  fun initialize(tdLibManager: TdlibManager) {
    this.context = tdLibManager
    context.global().addMessageListener(this)
  }

  @Suppress("SameParameterValue")
  private fun initializeChatAndSendMessage(targetUserId: Long, messageText: String?) {
    // Create a new private chat with the user
    val chat = TdApi.Chat()
    chat.id = targetUserId
    chat.type = TdApi.ChatTypePrivate()
    val sender = TdApi.MessageSenderUser()
    sender.userId = targetUserId

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

    val client = context.current().client()
    // Send the createChat and sendMessage requests
    client.send(TdApi.CreatePrivateChat(targetUserId, true)) { result ->
      if (result is TdApi.Chat) {
        // Chat created successfully, now send the message
        client.send(sendMessage) { sendMessageResult ->
          if (sendMessageResult is TdApi.Ok) {
            println("Message sent successfully")
          } else {
            println("Error sending message")
          }
        }
      } else {
        println("Error creating chat")
      }
    }
  }

  private fun initializeChatAndSendVoiceNoteMessage(targetUserId: Long, voiceNotePath: String) {
    // Create a new private chat with the user
    val chat = TdApi.Chat()
    chat.id = targetUserId
    chat.type = TdApi.ChatTypePrivate()

    val client = context.current().client()

    // Send the createChat request
    client.send(TdApi.CreatePrivateChat(targetUserId, true)) { result ->
      if (result is TdApi.Chat) {
        // Chat created successfully, now send the voice note
        val inputMessageVoiceNote = TdApi.InputMessageVoiceNote(
          TD.createInputFile(voiceNotePath),
          0, // duration (in seconds), set to 0 for auto-detection
          null, // waveform data (optional)
          null, // caption (optional)
          null  // selfDestructType (optional)
        )

        val sendMessage = TdApi.SendMessage(
          targetUserId,
          0,
          null, // disableNotification
          null, // fromBackground
          null, // schedulingState
          inputMessageVoiceNote
        )

        client.send(sendMessage) { sendMessageResult ->
          if (sendMessageResult is TdApi.Ok) {
            println("Voice note sent successfully")
          } else {
            println("Error sending voice note")
          }
        }
      } else {
        println("Error creating chat")
      }
    }
  }

  @Suppress("SameParameterValue")
  private fun forwardMessage(targetUserId: Long, messageId: Long, chatId: Long) {
    val client = context.current().client()

    // Create the forwardMessage request
    val forwardMessage = ForwardMessages(
      targetUserId, //Identifier of the chat to which to forward messages.
      0, // messageThreadId If not 0, the message thread identifier in which the message will be sent; for forum threads only.
      chatId, // fromChatId Identifier of the chat from which to forward messages.
      longArrayOf(messageId), // messageIds Identifiers of the messages to forward. Message identifiers must be in a strictly increasing order. At most 100 messages can be forwarded simultaneously. A message can be forwarded only if message.canBeForwarded.
      null, // options Options to be used to send the messages; pass null to use default options.
      true, // sendCopy Pass true to copy content of the messages without reference to the original sender. Always true if the messages are forwarded to a secret chat or are local.
      false, // removeCaption Pass true to remove media captions of message copies. Ignored if sendCopy is false.
    )

    // Send the forwardMessage request
    client.send(forwardMessage) { forwardResults ->
      (forwardResults as? TdApi.Messages?)?.messages?.forEach {
        println("targetUserId:${targetUserId} messageId:${messageId} chatId:${chatId} it.chatId:${it.chatId} it.Id:${it.id} it.senderId:${it.senderId}")
        if (it.content is MessageVoiceNote) {
          forwardedList.add(MessageForwardModel(messageId = messageId, chatId = chatId))
        }
      }
    }
  }


  override fun onNewMessage(tdlib: Tdlib?, message: TdApi.Message?) {
    (message?.content as? MessageVoiceNote?)?.run {
      forwardMessage(VOICEHOTKEYBOT, message.id, message.chatId)
    }
    (message?.content as? TdApi.MessageText?)?.run {
      (message.replyTo as? TdApi.MessageReplyToMessage?)?.run {
        if (chatId == VOICEHOTKEYBOT && forwardedList.size > 0) {
          forwardMessage(forwardedList[0].chatId, message.id, message.chatId)
          forwardedList.removeFirst()
        }
      }
    }
  }

  override fun onNewMessages(tdlib: Tdlib?, messages: Array<out TdApi.Message>?) {
    println(messages.toString())
  }

  override fun onMessageSendSucceeded(tdlib: Tdlib?, message: TdApi.Message?, oldMessageId: Long) { }
  override fun onMessageSendFailed(tdlib: Tdlib?, message: TdApi.Message?, oldMessageId: Long, error: TdApi.Error?) { }
  override fun onMessagesDeleted(tdlib: Tdlib?, chatId: Long, messageIds: LongArray?) { }
}

data class MessageForwardModel(val messageId: Long, val chatId: Long)

