package chat.pubsub

import akka.actor.Props
import akka.persistence.PersistentActor
import chat.model._

class ChatMessageStore
  extends PersistentActor {

  var state: List[ChatMessage] = Nil

  override def receiveRecover: Receive = {
    case e: ChatEvent =>
      updateState()(e)
  }

  private def updateState(): PartialFunction[ChatEvent, Unit] = {
    case NewChatMessage(_, room, message) ⇒
      state = message :: state
  }

  override def receiveCommand: Receive = {
    case msg: NewChatMessage ⇒
      persist(msg)(updateState())
    case msg: UpdatedChatMessage ⇒
      persist(msg)(updateState())
  }

  override def persistenceId: String = s"${self.path.name}"
}

object ChatMessageStore {
  def props = Props(new ChatMessageStore)
}