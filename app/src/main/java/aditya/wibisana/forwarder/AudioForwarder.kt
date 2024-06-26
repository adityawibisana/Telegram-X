package aditya.wibisana.forwarder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi.*
import org.thunderdog.challegram.telegram.GlobalMessageListener
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.telegram.TdlibManager

object AudioForwarder : GlobalMessageListener  {
  @Suppress("SpellCheckingInspection")
  private const val VOICEHOTKEYBOT = 6215296775
  private lateinit var context: TdlibManager
  private lateinit var client: Client

  private var currentTargetId : TargetMessage? = null

  fun initialize(tdLibManager: TdlibManager) {
    this.context = tdLibManager
    context.global().addMessageListener(this)
    client = context.current().client()
  }

  override fun onNewMessage(tdlib: Tdlib?, message: Message?) {
    message ?: return

    CoroutineScope(Dispatchers.IO).launch {
      when(message.content) {
        is MessageVoiceNote -> {
          val targetUserId = VOICEHOTKEYBOT
          val result = client.forwardMessage(targetUserId, message.id, message.chatId)
          result?.messages?.forEach {
            println("targetUserId:${targetUserId} messageId:${message.id} chatId:${message.chatId} it.chatId:${it.chatId} it.Id:${it.id} it.senderId:${it.senderId}")
            if (it.content is MessageVoiceNote) {
              currentTargetId = TargetMessage(
                chatId = message.chatId,
                messageThreadId = message.messageThreadId
              )
            }
          }
        }
        is MessageText -> {
          val replyTo = message.replyTo as? MessageReplyToMessage? ?: return@launch
          currentTargetId?.also {
            if (replyTo.chatId == VOICEHOTKEYBOT) {
              client.forwardMessage(it.chatId, message.id, message.chatId, it.messageThreadId)
              currentTargetId = null
              client.markMessageAsRead(replyTo.chatId, arrayOf(replyTo.messageId).toLongArray())
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
}

data class TargetMessage(val chatId: Long, val messageThreadId: Long = 0)

