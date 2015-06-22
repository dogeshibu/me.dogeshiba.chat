package me.dogeshiba.chat.persistance.topkek

import java.net.InetSocketAddress

import me.dogeshiba.chat.persistance.topkek.Users.User

trait UserRepository {

  def +=(user: User): Unit

  def -=(user: User): Unit

  def +=(channel: String): Unit

  def -=(channel: String): Unit

  def +=(pair: (String, User)): Unit

  def -=(pair: (String, User)): Unit

  def contains(address: InetSocketAddress): Boolean

  def contains(nick: String): Boolean

  def containsChannel(name: String): Boolean

  def users(): Seq[User]

  def channels(): Seq[String]

  def users(channel: String): Seq[User]

  def apply(nick: String): Option[User]

  def apply(actor: InetSocketAddress): Option[User]

}
