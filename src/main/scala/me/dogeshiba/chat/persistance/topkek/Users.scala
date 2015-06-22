package me.dogeshiba.chat.persistance.topkek

import java.net.InetSocketAddress

import me.dogeshiba.chat.protocols.topkek.Messages.TopKekMessage
import org.reactivestreams.Subscriber

object Users {

  sealed trait User {
    val address: InetSocketAddress
    val subscriber: Option[Subscriber[TopKekMessage]]

    def send(topKekMessage: TopKekMessage) = subscriber.foreach(_.onNext(topKekMessage))
  }

  sealed trait Nicknamed extends User {
    val nick: String
  }

  sealed case class Anonymous(address: InetSocketAddress, subscriber: Option[Subscriber[TopKekMessage]]) extends User

  sealed case class Authorized(nick: String, address: InetSocketAddress, subscriber: Option[Subscriber[TopKekMessage]]) extends Nicknamed

  sealed case class Locked(nick: String, password: String, address: InetSocketAddress, subscriber: Option[Subscriber[TopKekMessage]]) extends Nicknamed

  sealed case class Admin(nick: String, password: String, address: InetSocketAddress, subscriber: Option[Subscriber[TopKekMessage]]) extends Nicknamed

  sealed trait Operation

  sealed case class SetNick(nick: String) extends Operation

  sealed case class Authorize(nick: String, password: String, against: Option[User]) extends Operation

  sealed case class Lock(password: String) extends Operation

  object Unlock extends Operation

  object Elevate extends Operation

  object Drop extends Operation

  def process(operation: Operation)(user: User): Option[User] = user -> operation match {
    case (Anonymous(address, subscriber), SetNick(nick)) => Some(Authorized(nick, address, subscriber))
    case (Anonymous(address, subscriber), Authorize(nick, password, Some(Locked(n, pwd, _, _)))) if n == nick && password == pwd =>
      Some(Locked(nick, password, address, subscriber))
    case (Anonymous(address, subscriber), Authorize(nick, password, Some(Admin(n, pwd, _, _)))) if n == nick && password == pwd =>
      Some(Admin(nick, password, address, subscriber))
    case (Authorized(nick, address, subscriber), Lock(password)) => Some(Locked(nick, password, address, subscriber))
    case (Locked(nick, password, address, subscriber), Elevate) => Some(Admin(nick, password, address, subscriber))
    case (Locked(nick, password, address, subscriber), Unlock) => Some(Authorized(nick, address, subscriber))
    case (Admin(nick, password, address, subscriber), Drop) => Some(Locked(nick, password, address, subscriber))
    case _ => None
  }

}
