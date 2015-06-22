package me.dogeshiba.chat.persistance.topkek

import java.net.InetSocketAddress

import me.dogeshiba.chat.persistance.topkek.Users.{Nicknamed, User}

class InMemoryUserRepository extends UserRepository {

  private[this] val connectionsMap = scala.collection.concurrent.TrieMap[InetSocketAddress, User]()
  private[this] val channelsMap = scala.collection.concurrent.TrieMap[String, Set[InetSocketAddress]]()

  override def +=(user: User): Unit =
    connectionsMap += user.address -> user

  override def -=(user: User): Unit = {
    connectionsMap -= user.address
    for (channel <- channelsMap.keys) {
      channelsMap(channel) = channelsMap(channel) - user.address
    }
  }

  override def users(): Seq[User] =
    connectionsMap.values.toSeq

  override def apply(nick: String): Option[User] = connectionsMap.values.collectFirst {
    case user: User with Nicknamed if user.nick == nick => user
  }

  override def apply(actor: InetSocketAddress): Option[User] =
    connectionsMap.get(actor)

  override def contains(address: InetSocketAddress): Boolean =
    connectionsMap.contains(address)

  override def contains(nick: String): Boolean =
    apply(nick).isDefined

  override def +=(channel: String): Unit =
    channelsMap += channel -> Set.empty[InetSocketAddress]

  override def -=(channel: String): Unit =
    channelsMap -= channel

  override def -=(pair: (String, User)): Unit = pair match {
    case (channel, user) =>
      channelsMap(channel) = channelsMap(channel) - user.address
  }

  override def +=(pair: (String, User)): Unit = pair match {
    case (channel, user) =>
      channelsMap(channel) = channelsMap(channel) + user.address
  }

  override def users(channel: String): Seq[User] =
    channelsMap(channel).map(connectionsMap).toSeq

  override def containsChannel(name: String): Boolean =
    channelsMap.contains(name)

  override def channels(): Seq[String] =
    channelsMap.keys.toSeq
}
