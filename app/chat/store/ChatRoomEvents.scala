package chat.store

import akka.actor.{ ExtendedActorSystem, Props }
import akka.persistence.PersistentActor
import akka.persistence.journal.{ Tagged, WriteEventAdapter }
import chat.model._

private[store] class ChatRoomEvents
  extends PersistentActor {

  case class State(chatRoomEvents: Map[String, ChatRoomEvent] = Map.empty) {
    def updated(event: ChatRoomEvent): State = {
      copy(chatRoomEvents = chatRoomEvents.updated(event.room, event))
    }
  }

  var state = State()

  override def receiveRecover: Receive = {
    case e: ChatEvent =>
      updateState()(e)
  }

  private def updateState(): PartialFunction[ChatRoomEvent, Unit] = {
    case event ⇒
      state = state.updated(event)
  }

  override def receiveCommand: Receive = {
    case msg: ChatRoomEvent ⇒
      persist(msg)(updateState())
  }

  override def persistenceId: String = s"${self.path.name}"
}

object ChatRoomEvents {
  def props = Props(new ChatRoomEvents)
}

class ChatRoomEventsEventAdapter(system: ExtendedActorSystem)
  extends WriteEventAdapter {

  override def manifest(event: Any): String = ""

  override def toJournal(event: Any): Any = event match {
    case e: ChatRoomEvent ⇒
      Tagged(event, Set(e.room))
    case other ⇒ other
  }

}