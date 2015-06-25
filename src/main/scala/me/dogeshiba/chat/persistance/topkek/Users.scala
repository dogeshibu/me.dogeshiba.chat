package me.dogeshiba.chat.persistance.topkek

import java.net.InetSocketAddress

object Users {

  sealed trait User {
    val address: InetSocketAddress
  }

  sealed trait Nicknamed extends User {
    val nick: String
  }

  sealed case class Anonymous(address: InetSocketAddress) extends User

  sealed case class Authorized(nick: String, address: InetSocketAddress) extends Nicknamed

  sealed case class Locked(nick: String, password: String, address: InetSocketAddress) extends Nicknamed

  sealed case class Admin(nick: String, password: String, address: InetSocketAddress) extends Nicknamed


}
