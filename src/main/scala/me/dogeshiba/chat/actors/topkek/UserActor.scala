package me.dogeshiba.chat.actors.topkek

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, PoisonPill, _}
import akka.pattern._
import me.dogeshiba.chat.actors.topkek.Messages.UserDisconnected
import me.dogeshiba.chat.protocols.topkek.Messages._

class UserActor(private[this] val broadcaster: ActorRef) extends Actor {

  implicit val timeout = akka.util.Timeout(5, TimeUnit.SECONDS)
  implicit val executionContext = context.dispatcher

  private[this] def userLogined: Receive = {
    case msg : Request =>
      val sentFrom = sender()
      (broadcaster ? msg).onSuccess{case response => sentFrom ! response}
    case UserDisconnected =>
      broadcaster ! UserDisconnected
      context.self ! PoisonPill
  }

  override def receive: Receive = {
    case ClientHello(id) =>
      sender() ! ServerHello(id)
    case msg : SetNickname =>
      context.become(userLogined)
    case msg : Authorization =>
      context.become(userLogined)
  }
}
