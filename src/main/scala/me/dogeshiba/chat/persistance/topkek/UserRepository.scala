package me.dogeshiba.chat.persistance.topkek

import akka.actor.ActorRef
import me.dogeshiba.chat.persistance.topkek.Persistance.User

import scala.concurrent.Future

trait UserRepository {
  def +=(user : User) : Future[User]
  def -=(user : User) : Future[User]

  def contains(nick : String) : Future[Boolean]
  def contains(actor : ActorRef) : Future[Boolean]

  def all() : Future[Seq[User]]
  def apply(nick : String) : Future[Option[User]]
  def apply(actor : ActorRef) : Future[Option[User]]

  def update(user: User) : Future[User]
}
