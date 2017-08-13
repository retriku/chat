package chat.store

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, ExtendedActorSystem, Props}
import akka.persistence.PersistentActor
import akka.persistence.journal.{Tagged, WriteEventAdapter}
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import akka.persistence.query.{NoOffset, PersistenceQuery}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Source}
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

  def persistenceFlow(implicit system: ActorSystem): Flow[ChatRoomEvent, ChatRoomEvent, NotUsed] = {
    val store: ActorRef = system.actorOf(ChatRoomEvents.props)
    Flow.fromFunction { message ⇒
      store ! message
      message
    }
  }

  def chatRoomEventSource(tag: String)
                         (implicit system: ActorSystem,
                          mat: Materializer): Source[ChatRoomEvent, NotUsed] = {
    val persistenceQuery = PersistenceQuery(system)
      .readJournalFor[LeveldbReadJournal](LeveldbReadJournal.Identifier)

    persistenceQuery.currentEventsByTag(tag, NoOffset)
      .map(_.event.asInstanceOf[ChatRoomEvent])
  }

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