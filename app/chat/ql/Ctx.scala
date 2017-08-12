package chat.ql

import akka.actor.ActorRef
import chat.model._
import org.reactivestreams.Publisher

case class Ctx(
  messagesView:     ActorRef,
  messageStore:     ActorRef,
  messagePublisher: Publisher[NewChatMessage])
  extends Mutation {

  def addEvent[T](
    messagesView: ActorRef,
    message:      T): Unit = {
    messageStore ! message
  }

}
