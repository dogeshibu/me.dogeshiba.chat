package me.dogeshiba.chat.persistance.topkek

import java.net.InetSocketAddress

import me.dogeshiba.chat.protocols.topkek.Messages.TopKekMessage
import org.reactivestreams.Subscriber

object Persistance {

  sealed case class User(nick : Option[String],
                         passwordHash : Option[String],
                         claim : Claim,
                         subscriber : Option[Subscriber[TopKekMessage]],
                         address : InetSocketAddress)

  sealed trait Claim
  object Admin extends Claim
  object Authorized extends Claim
  object Anonymous extends Claim
}
