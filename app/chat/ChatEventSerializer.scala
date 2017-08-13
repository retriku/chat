package chat

import akka.actor.ExtendedActorSystem
import akka.serialization.{BaseSerializer, SerializerWithStringManifest}
import chat.model._
import play.api.libs.json.Json

/**
  * Since messages sent through distributed pubsub go over Akka remoting, they need to be
  * serialized. This serializer serializes them as JSON.
  */
class ChatEventSerializer(val system: ExtendedActorSystem)
  extends SerializerWithStringManifest
    with BaseSerializer {

  override def manifest(o: AnyRef): String = o match {
    case _: NewChatMessage ⇒ "M"
    case _: UpdatedChatMessage ⇒ "U"
    case _: JoinRoom ⇒ "J"
    case _: LeaveRoom ⇒ "L"
    case other ⇒ sys.error("Don't know how to serialize " + other)
  }

  override def toBinary(o: AnyRef): Array[Byte] = {
    val json = o match {
      case cm: NewChatMessage ⇒ Json.toJson(cm)
      case cm: UpdatedChatMessage ⇒ Json.toJson(cm)
      case jr: JoinRoom ⇒ Json.toJson(jr)
      case lr: LeaveRoom ⇒ Json.toJson(lr)
      case other ⇒ sys.error("Don't know how to serialize " + other)
    }
    Json.toBytes(json)
  }

  override def fromBinary(
                           bytes: Array[Byte],
                           manifest: String): ChatRoomEvent = {
    val json = Json.parse(bytes)
    manifest match {
      case "M" ⇒ json.as[NewChatMessage]
      case "U" ⇒ json.as[UpdatedChatMessage]
      case "J" ⇒ json.as[JoinRoom]
      case "L" ⇒ json.as[LeaveRoom]
      case other ⇒ sys.error("Unknown manifest " + other)
    }
  }
}
