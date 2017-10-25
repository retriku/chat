package chat

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Sink, Source}
import chat.model._
import chat.store.ChatRoomEvents
import com.typesafe.scalalogging.LazyLogging
import play.api.Logger
import play.engineio.EngineIOController
import play.socketio.scaladsl.SocketIO

class ChatEngine(socketIO: SocketIO)
                (implicit system: ActorSystem,
                 mat: Materializer)
  extends LazyLogging {

  import chat.model.ChatProtocol._

  var chatRooms =
    Map.empty[String, (Sink[ChatRoomEvent, NotUsed], Source[ChatRoomEvent, NotUsed])]

  private def getChatRoom(user: User,
                          room: String): Flow[ChatRoomEvent, ChatRoomEvent, NotUsed] = {
    val (sink, source) = chatRooms.getOrElse(room, {
      val p = MergeHub.source[ChatRoomEvent].toMat(BroadcastHub.sink[ChatRoomEvent])(Keep.both).run
      chatRooms += room → p
      p
    })

    Flow.fromSinkAndSourceCoupled(
      sink =
        Flow[ChatRoomEvent].prepend {
          Source.single(JoinRoom(
            user = Some(user),
            room = room)).concat(ChatRoomEvents.chatRoomEventSource(room))
        }.concat(Source.single(LeaveRoom(
          user = Some(user),
          room = room))).to(sink),
      source = source)
  }

  // Creates a chat flow for a user session
  def userChatFlow(user: User): Flow[ChatRoomEvent, ChatRoomEvent, NotUsed] = {
    logger.debug(s"for user: $user")
    var broadcastSource: Source[ChatRoomEvent, NotUsed] = null
    var mergeSink: Sink[ChatRoomEvent, NotUsed] = null

    Flow[ChatRoomEvent] map {
      case event@JoinRoom(_, room) ⇒
        logger.debug(s"received: $event")

        val roomFlow: Flow[ChatRoomEvent, ChatRoomEvent, NotUsed] =
          getChatRoom(
            user = user,
            room = room
          )

        broadcastSource
          .filter(e ⇒ e.room == room && !e.isInstanceOf[JoinRoom])
          .takeWhile(!_.isInstanceOf[LeaveRoom])
          .via(roomFlow)
          .concat(Source.single(LeaveRoom(
            user = Some(user),
            room = room)))
          .runWith(mergeSink)

        event
      case msg@NewChatMessage(room, _, message) ⇒
        logger.debug(s"received: $msg")
        NewChatMessage(
          user = Some(user),
          room = room,
          message = message)
      case msg@UpdatedChatMessage(room, _, message) ⇒
        logger.debug(s"received: $msg")
        UpdatedChatMessage(
          user = Some(user),
          room = room,
          message = message.copy(message = s"updated: ${message.message}"))
      case other ⇒
        logger.debug(s"received: $other")
        other

    } via {
      ChatRoomEvents.persistenceFlow
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

  val controller: EngineIOController = socketIO.builder.onConnect { (
                                                                      request,
                                                                      sid) ⇒
    Logger.info(s"Starting $sid session")
    // Extract the username from the header
    val username = request.getQueryString("user").getOrElse {
      throw new RuntimeException("No user parameter")
    }
    // And return the user, this will be the data for the session that we can read when we add a namespace
    User(username)
  }.addNamespace(
    decoder = decoder,
    encoder = encoder) {
    case (session, chat) if chat.split('?').head == "/chat" ⇒
      userChatFlow(session.data)
  }.createController()
}