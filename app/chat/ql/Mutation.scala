package chat.ql

import chat.model.{ ChatMessage, NewChatMessage, UpdatedChatMessage, User }
import sangria.macros.derive.GraphQLField

trait Mutation {
  self: Ctx ⇒

  @GraphQLField
  def addChatMessage(
    user:    Option[User],
    room:    String,
    message: String,
    id:      String): Unit =
    addEvent(
      messagesView = messagesView,
      message = NewChatMessage(
        user = user,
        room = room,
        message = ChatMessage(
          id = id,
          message = message)))

  @GraphQLField
  def changeChatMessage(
    user:    Option[User],
    room:    String,
    message: String,
    id:      String): Unit =
    addEvent(
      messagesView = messagesView,
      message = UpdatedChatMessage(
        user = user,
        room = room,
        message = ChatMessage(
          id = id,
          message = message)))

}
