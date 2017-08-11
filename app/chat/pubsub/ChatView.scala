package chat.pubsub

import akka.actor.ActorLogging
import akka.stream.actor.{ActorSubscriber, OneByOneRequestStrategy}

class ChatView
  extends ActorSubscriber
    with ActorLogging{

  override protected def requestStrategy = OneByOneRequestStrategy

  override def receive = ???
}

object ChatView{
  case class List(offset: Int, limit: Int)
  case class Get(id: String, version: Option[Long] = None)
  case class GetMany(ids: Seq[String])
}