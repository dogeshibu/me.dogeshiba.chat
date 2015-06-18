package me.dogeshiba.chat.system

import akka.actor.ActorSystem

trait ActorSystemOwner extends AutoCloseable {
  implicit val system = ActorSystem("me-dogeshiba-chat")

  override def close(): Unit = {
    system.shutdown()
  }
}
