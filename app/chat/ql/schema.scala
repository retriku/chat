package chat.ql

import akka.stream.Materializer
import akka.util.Timeout
import sangria.macros.derive.{ Interfaces, deriveContextObjectType, deriveObjectType }
import sangria.schema.{ Field, InterfaceType, LongType, ObjectType, Schema, StringType, fields }

import scala.concurrent.ExecutionContext

object schema {
  //  def createSchema(implicit timeout: Timeout,
  //                   ec: ExecutionContext,
  //                   mat: Materializer): Schema[Ctx, Unit] = {
  //    val ChatMessageType = deriveObjectType[Unit, NewChatMessage](Interfaces(InterfaceType("Roomed", fields[Ctx, ChatRoomEvent](
  //      Field("user", StringType, resolve = _.value.user),
  //      Field("room", StringType, resolve = _.value.room)))
  //    ))
  //    val QueryType = ObjectType("Query",
  //      entityFields[NewChatMessage](
  //        "author",
  //        tpe = ChatMessageType,
  //        actor = _.m))
  //
  //    val MutationType = deriveContextObjectType[Ctx, Mutation, Unit](identity)
  //
  //    Schema(
  //      query = QueryType,
  //      mutation = Some(MutationType),
  //      subscription = Some(SubscriptionType))
  //  }
}
