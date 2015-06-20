package me.dogeshiba.chat.persistance.topkek

import akka.actor.ActorRef

object Persistance {
  sealed case class User(nick : String, passwordHash : Option[String], claim : Claim, actor : Option[ActorRef])

  sealed trait Claim
  object Admin extends Claim
  object Authorized extends Claim
}
