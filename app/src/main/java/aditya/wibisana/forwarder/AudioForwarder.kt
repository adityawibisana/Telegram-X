package aditya.wibisana.forwarder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi.*
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
              client.forwardMessage(it, message.id, message.chatId, message.messageThreadId)
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

