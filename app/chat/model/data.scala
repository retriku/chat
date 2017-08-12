package chat.model

import play.api.libs.functional.syntax._
import play.api.libs.json.{ Format, Json }

sealed trait ChatRoomEvent {
  def user: Option[User]

  def room: String
}

sealed trait ChatEvent extends ChatRoomEvent

case class ChatMessage(
  id:      String,
  message: String)

object ChatMessage {
  implicit val format: Format[ChatMessage] = Json.format
}

case class NewChatMessage(
  room:    String,
  user:    Option[User],
  message: ChatMessage) extends ChatEvent

object NewChatMessage {
  implicit val format: Format[NewChatMessage] = Json.format
}

case class UpdatedChatMessage(
  room:    String,
  user:    Option[User],
  message: ChatMessage) extends ChatEvent

object UpdatedChatMessage {
  implicit val format: Format[UpdatedChatMessage] = Json.format
}

case class JoinRoom(user: Option[User], room: String) extends ChatRoomEvent

object JoinRoom {
  implicit val format: Format[JoinRoom] = Json.format
}

case class LeaveRoom(user: Option[User], room: String) extends ChatRoomEvent

object LeaveRoom {
  implicit val format: Format[LeaveRoom] = Json.format
}

case class User(name: String)

object User {
  implicit val format: Format[User] = implicitly[Format[String]].inmap(User.apply, _.name)
}

object ChatProtocol {

  import play.socketio.scaladsl.SocketIOEventCodec._

  val decoder: SocketIOEventsDecoder[ChatRoomEvent] = decodeByName {
    case "new chat message"    ⇒ decodeJson[NewChatMessage]
    case "update chat message" ⇒ decodeJson[UpdatedChatMessage]
    case "join room"           ⇒ decodeJson[JoinRoom]
    case "leave room"          ⇒ decodeJson[LeaveRoom]
  }

  val encoder: SocketIOEventsEncoder[ChatRoomEvent] = encodeByType[ChatRoomEvent] {
    case _: NewChatMessage     ⇒ "new chat message" → encodeJson[NewChatMessage]
    case _: UpdatedChatMessage ⇒ "update chat message" → encodeJson[UpdatedChatMessage]
    case _: JoinRoom           ⇒ "join room" → encodeJson[JoinRoom]
    case _: LeaveRoom          ⇒ "leave room" → encodeJson[LeaveRoom]
  }
}
