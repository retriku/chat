package chat

import akka.NotUsed
import akka.actor.{ ActorRef, ActorSystem }
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{ Publish, Subscribe }
import akka.stream._
import akka.stream.scaladsl.{ BroadcastHub, Flow, MergeHub, Sink, Source }
import chat.model._
import com.typesafe.scalalogging.LazyLogging
import play.api.Logger
import play.engineio.EngineIOController
import play.socketio.scaladsl.SocketIO

class ChatEngine(
  socketIO: SocketIO,
  system:   ActorSystem)(implicit mat: Materializer)
  extends LazyLogging {

  import chat.model.ChatProtocol._

  val mediator: ActorRef = DistributedPubSub(system).mediator

  // This gets a chat room using Akka distributed pubsub
  private def getChatRoom(
    user: User,
    room: String): Flow[ChatRoomEvent, ChatRoomEvent, NotUsed] = {

    // Create a sink that sends all the messages to the chat room
    val sink = Sink.foreach[ChatRoomEvent] { message =>
      mediator ! Publish(room, message)
    }

    // Create a source that subscribes to messages from the chatroom
    val source = Source.actorRef[ChatRoomEvent](16, OverflowStrategy.dropHead)
      .mapMaterializedValue { ref ⇒
        mediator ! Subscribe(room, ref)
      }

    Flow.fromSinkAndSourceCoupled(
      sink = Flow[ChatRoomEvent]
        // Add the join and leave room events
        .prepend(Source.single(JoinRoom(Some(user), room)))
        .concat(Source.single(LeaveRoom(Some(user), room)))
        .to(sink),
      source = source)
  }

  // Creates a chat flow for a user session
  def userChatFlow(user: User): Flow[ChatRoomEvent, ChatRoomEvent, NotUsed] = {

    // broadcast source and sink for demux/muxing multiple chat rooms in this one flow
    // They'll be provided later when we materialize the flow
    var broadcastSource: Source[ChatRoomEvent, NotUsed] = null
    var mergeSink: Sink[ChatRoomEvent, NotUsed] = null

    Flow[ChatRoomEvent] map {
      case event @ JoinRoom(_, room) ⇒
        logger.debug(s"received: $event")
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

      case msg @ NewChatMessage(room, _, message) ⇒
        logger.debug(s"received: $msg")
        NewChatMessage(
          user = Some(user),
          room = room,
          message = message)

      case msg @ UpdatedChatMessage(room, _, message) ⇒
        logger.debug(s"received: $msg")
        UpdatedChatMessage(
          user = Some(user),
          room = room,
          message = message)

      case other ⇒
        logger.debug(s"received: $other")
        other

    } via {
      Flow.fromSinkAndSourceCoupledMat(
        sink = BroadcastHub.sink[ChatRoomEvent],
        source = MergeHub.source[ChatRoomEvent]) { (source, sink) ⇒
        broadcastSource = source
        mergeSink = sink
        NotUsed
      }
    }
  }

  val controller: EngineIOController = socketIO.builder
    .onConnect { (request, sid) ⇒
      Logger.info(s"Starting $sid session")
      // Extract the username from the header
      val username = request.getQueryString("user").getOrElse {
        throw new RuntimeException("No user parameter")
      }
      // And return the user, this will be the data for the session that we can read when we add a namespace
      User(username)
    }.addNamespace(decoder, encoder) {
      case (session, chat) if chat.split('?').head == "/chat" ⇒
        userChatFlow(session.data)
    }.createController()
}