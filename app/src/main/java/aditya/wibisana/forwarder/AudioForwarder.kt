package aditya.wibisana.forwarder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi.*
import org.thunderdog.challegram.data.TD
import org.thunderdog.challegram.telegram.GlobalMessageListener
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.telegram.TdlibManager
import java.util.concurrent.Executors

object AudioForwarder : GlobalMessageListener  {
  @Suppress("SpellCheckingInspection")
  private const val VOICEHOTKEYBOT = 6215296775
  private lateinit var context: TdlibManager
  private lateinit var client: Client

  private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

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

  override fun onNewMessage(tdlib: Tdlib?, message: Message?) {
    message ?: return

    when(message.content) {
      is MessageVoiceNote -> {
        CoroutineScope(dispatcher).launch {
          val targetUserId = VOICEHOTKEYBOT
          val result = client.forwardMessage(targetUserId, message.id, message.chatId, message.messageThreadId)
          result?.messages?.forEach {
            println("targetUserId:${targetUserId} messageId:${message.id} chatId:${message.chatId} it.chatId:${it.chatId} it.Id:${it.id} it.senderId:${it.senderId}")
            if (it.content is MessageVoiceNote) {
              currentTargetId = message.chatId
            }
          }
        }
      }
      is MessageText -> {
        val replyTo = message.replyTo as? MessageReplyToMessage? ?: return
        currentTargetId?.also {
          if (replyTo.chatId == VOICEHOTKEYBOT) {
            CoroutineScope(dispatcher).launch {
              val result = client.forwardMessage(it, message.id, message.chatId, message.messageThreadId)
              if (result != null) { // only change state if success
                currentTargetId = null
              }
              markMessageAsRead(replyTo.chatId, arrayOf(replyTo.messageId).toLongArray())
            }
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

