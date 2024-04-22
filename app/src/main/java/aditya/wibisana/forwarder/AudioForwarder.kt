package aditya.wibisana.forwarder

import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi.*
import org.thunderdog.challegram.data.TD
import org.thunderdog.challegram.telegram.GlobalMessageListener
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.telegram.TdlibManager
import kotlin.random.Random

object AudioForwarder : GlobalMessageListener  {
  @Suppress("SpellCheckingInspection")
  private const val VOICEHOTKEYBOT = 6215296775
  private lateinit var context: TdlibManager
  private lateinit var client: Client

  private var currentTargetId : Long? = null

  fun initialize(tdLibManager: TdlibManager) {
    this.context = tdLibManager
    context.global().addMessageListener(this)
    client = context.current().client()
  }

  private fun initializeChatAndSendVoiceNoteMessage(targetUserId: Long, voiceNotePath: String) {
    // Send the createChat request
    client.send(CreatePrivateChat(targetUserId, true)) { result ->
      if (result is Chat) {
        // Chat created successfully, now send the voice note
        val inputMessageVoiceNote = InputMessageVoiceNote(
          TD.createInputFile(voiceNotePath),
          0, // duration (in seconds), set to 0 for auto-detection
          null, // waveform data (optional)
          null, // caption (optional)
          null  // selfDestructType (optional)
        )

        val sendMessage = SendMessage(
          targetUserId,
          0,
          null, // disableNotification
          null, // fromBackground
          null, // schedulingState
          inputMessageVoiceNote
        )

        client.send(sendMessage) { sendMessageResult ->
          if (sendMessageResult is Ok) {
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
  private fun forwardMessage(targetUserId: Long, messageId: Long, chatId: Long, messageThreadId: Long = 0) {
    // Create the forwardMessage request
    val forwardMessage = ForwardMessages(
      targetUserId, //Identifier of the chat to which to forward messages.
      messageThreadId, // messageThreadId If not 0, the message thread identifier in which the message will be sent; for forum threads only.
      chatId, // fromChatId Identifier of the chat from which to forward messages.
      longArrayOf(messageId), // messageIds Identifiers of the messages to forward. Message identifiers must be in a strictly increasing order. At most 100 messages can be forwarded simultaneously. A message can be forwarded only if message.canBeForwarded.
      MessageSendOptions(
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
    client.send(forwardMessage) { forwardResults ->
      (forwardResults as? Messages?)?.messages?.forEach {
        println("targetUserId:${targetUserId} messageId:${messageId} chatId:${chatId} it.chatId:${it.chatId} it.Id:${it.id} it.senderId:${it.senderId}")
        if (it.content is MessageVoiceNote) {
          currentTargetId = chatId
        }
      }
    }
  }


  override fun onNewMessage(tdlib: Tdlib?, message: Message?) {
    message ?: return

    when(message.content) {
      is MessageVoiceNote -> {
        forwardMessage(VOICEHOTKEYBOT, message.id, message.chatId, message.messageThreadId)
      }
      is MessageText -> {
        val replyTo = message.replyTo as? MessageReplyToMessage? ?: return
        currentTargetId?.also {
          if (replyTo.chatId == VOICEHOTKEYBOT) {
            forwardMessage(it, message.id, message.chatId, message.messageThreadId)
            currentTargetId = null
            markMessageAsRead(replyTo.chatId, arrayOf(replyTo.messageId).toLongArray())
          }
        }
      }
    }
  }

  override fun onNewMessages(tdlib: Tdlib?, messages: Array<out Message>?) {
    println(messages.toString())
  }

  override fun onMessageSendSucceeded(tdlib: Tdlib?, message: Message?, oldMessageId: Long) { }
  override fun onMessageSendFailed(tdlib: Tdlib?, message: Message?, oldMessageId: Long, error: Error?) { }
  override fun onMessagesDeleted(tdlib: Tdlib?, chatId: Long, messageIds: LongArray?) { }

  private fun markMessageAsRead(chatId: Long, messageIds: LongArray) {
    listOf(MessageSourceNotification(), MessageSourceChatHistory(), MessageSourceChatList(), MessageSourceOther()).forEach {
      val readMessage = ViewMessages(chatId, messageIds, it, true)
      client.send(readMessage) {
        // no use
      }
    }
  }
}

