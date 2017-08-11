package chat.pubsub

import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import chat.ChatMessage

class ChatMessageStore
  extends ActorPublisher[ChatMessage] {

  import ChatMessageStore._

  override def receive: Receive = withMessages(Nil)

  private def withMessages(messages: List[ChatMessage]): Receive = {
    case AddChatMessage(message) ⇒
      val newMessages = message :: messages

      context.become(withMessages(newMessages))
      sender() ! MessageAdded(message)
      deliverEvents(newMessages)
    case Request(_) ⇒
      deliverEvents(messages)
    case Cancel ⇒
      context.stop(self)
  }

  private def deliverEvents(messages: List[ChatMessage]): Unit = {
    if (isActive && totalDemand > 0) {
      val (use, keep) = messages.splitAt(totalDemand.toInt)

      context.become(withMessages(keep))

      use foreach onNext
    }
  }
}

object ChatMessageStore {

  case class AddChatMessage(chatMessage: ChatMessage)

  case class MessageAdded(chatMessage: ChatMessage)

}