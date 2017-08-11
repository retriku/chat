package chat.ql

import akka.actor.ActorRef
import chat.ChatMessage
import org.reactivestreams.Publisher

case class Ctx(messagesView: ActorRef,
               messageStore: ActorRef,
               messagePublisher: Publisher[ChatMessage]) {

}
