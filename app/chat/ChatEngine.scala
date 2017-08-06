package chat

import akka.NotUsed
import akka.actor.{ ActorRef, ActorSystem }
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{ Publish, Subscribe }
import akka.stream._
import akka.stream.scaladsl.{ BroadcastHub, Flow, MergeHub, Sink, Source }
import play.api.Logger
import play.api.libs.json.{ Format, Json }
import play.engineio.EngineIOController
import play.api.libs.functional.syntax._
import play.socketio.scaladsl.SocketIO

/**
 * A chat event, either a message, a join room, or a leave room event.
 */
sealed trait ChatEvent {
  def user: Option[User]
  def room: String
}

case class ChatMessage(
  user: Option[User],
  room: String,
  message: String,
  id: String) extends ChatEvent
object ChatMessage {
  implicit val format: Format[ChatMessage] = Json.format
}

case class JoinRoom(user: Option[User], room: String) extends ChatEvent
object JoinRoom {
  implicit val format: Format[JoinRoom] = Json.format
}

case class LeaveRoom(user: Option[User], room: String) extends ChatEvent
object LeaveRoom {
  implicit val format: Format[LeaveRoom] = Json.format
}

case class User(name: String)
object User {
  // We're just encoding user as a simple string, not an object
  implicit val format: Format[User] = implicitly[Format[String]].inmap(User.apply, _.name)
}

object ChatProtocol {
  import play.socketio.scaladsl.SocketIOEventCodec._

  val decoder: SocketIOEventsDecoder[ChatEvent] = decodeByName {
    case "chat message" => decodeJson[ChatMessage]
    case "join room" => decodeJson[JoinRoom]
    case "leave room" => decodeJson[LeaveRoom]
  }

  val encoder: SocketIOEventsEncoder[ChatEvent] = encodeByType[ChatEvent] {
    case _: ChatMessage => "chat message" -> encodeJson[ChatMessage]
    case _: JoinRoom => "join room" -> encodeJson[JoinRoom]
    case _: LeaveRoom => "leave room" -> encodeJson[LeaveRoom]
  }
}

class ChatEngine(
  socketIO: SocketIO,
  system: ActorSystem)(implicit mat: Materializer) {

  import ChatProtocol._

  val mediator: ActorRef = DistributedPubSub(system).mediator

  // This gets a chat room using Akka distributed pubsub
  private def getChatRoom(
    user: User,
    room: String): Flow[ChatEvent, ChatEvent, NotUsed] = {

    // Create a sink that sends all the messages to the chat room
    val sink = Sink.foreach[ChatEvent] { message =>
      mediator ! Publish(room, message)
    }

    // Create a source that subscribes to messages from the chatroom
    val source = Source.actorRef[ChatEvent](16, OverflowStrategy.dropHead)
      .mapMaterializedValue { ref =>
        mediator ! Subscribe(room, ref)
      }

    Flow.fromSinkAndSourceCoupled(
      sink = Flow[ChatEvent]
        // Add the join and leave room events
        .prepend(Source.single(JoinRoom(Some(user), room)))
        .concat(Source.single(LeaveRoom(Some(user), room)))
        .to(sink),
      source = source)
  }

  // Creates a chat flow for a user session
  def userChatFlow(user: User): Flow[ChatEvent, ChatEvent, NotUsed] = {

    // broadcast source and sink for demux/muxing multiple chat rooms in this one flow
    // They'll be provided later when we materialize the flow
    var broadcastSource: Source[ChatEvent, NotUsed] = null
    var mergeSink: Sink[ChatEvent, NotUsed] = null

    Flow[ChatEvent] map {
      case event @ JoinRoom(_, room) =>
        val roomFlow = getChatRoom(user, room)

        // Add the room to our flow
        broadcastSource
          // Ensure only messages for this room get there
          // Also filter out JoinRoom messages, since there's a race condition as to whether it will
          // actually get here or not, so the room flow explicitly adds it.
          .filter(e => e.room == room && !e.isInstanceOf[JoinRoom])
          // Take until we get a leave room message.
          .takeWhile(!_.isInstanceOf[LeaveRoom])
          // And send it through the room flow
          .via(roomFlow)
          // Re-add the leave room here, since it was filtered out before
          .concat(Source.single(LeaveRoom(Some(user), room)))
          // And feed to the merge sink
          .runWith(mergeSink)

        event

      case ChatMessage(_, room, message, id) =>
        ChatMessage(
          user = Some(user),
          room = room,
          message = message,
          id = id)

      case other => other

    } via {
      Flow.fromSinkAndSourceCoupledMat(
        sink = BroadcastHub.sink[ChatEvent],
        source = MergeHub.source[ChatEvent]) { (source, sink) =>
        broadcastSource = source
        mergeSink = sink
        NotUsed
      }
    }
  }

  val controller: EngineIOController = socketIO.builder
    .onConnect { (request, sid) =>
      Logger.info(s"Starting $sid session")
      // Extract the username from the header
      val username = request.getQueryString("user").getOrElse {
        throw new RuntimeException("No user parameter")
      }
      // And return the user, this will be the data for the session that we can read when we add a namespace
      User(username)
    }.addNamespace(decoder, encoder) {
      case (session, chat) if chat.split('?').head == "/chat" => userChatFlow(session.data)
    }
    .createController()
}