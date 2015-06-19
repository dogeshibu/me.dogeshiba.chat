package me.dogeshiba.chat.actors.topkek

import me.dogeshiba.chat.actors.topkek.Messages.UserDisconnected
import me.dogeshiba.chat.protocols.topkek.Messages._
import akka.actor.{ActorRef, Actor}

class MessageBroadcaster extends Actor {

  private sealed case class User(nick : String, passwordHash : Option[String], claim : Claim, actor : Option[ActorRef])

  private sealed trait Claim
  private object Admin extends Claim
  private object Authorized extends Claim

  private var users = Map[String,User]()
  private var actors = Map[ActorRef,User]()

  private var channels = Map[String,Set[String]]()

  private def user = actors(sender())

  private def update(user: User) = {
    users += user.nick -> user
    if(user.actor.isDefined)
      actors += user.actor.get -> user
  }

  private def adminActions: Receive = {

    case msg : TopKekMessage with AdminMessage if user.claim != Admin =>
      sender() ! Unauthorized(msg.id)

    case CreateChannel(id, channel) if channels.contains(channel) =>
      sender() ! ChannelNotUnique(id)

    case CreateChannel(id, channel) =>
      channels += channel -> Set.empty
      sender() ! ServerOk(id)

    case DeleteChannel(id, channel) if channels.contains(channel) =>
      channels -= channel
      sender() ! ServerOk(id)

    case DeleteChannel(id, channel) =>
      sender() ! ChannelNotFound(id)

    case ElevatePrivileges(id, nick) if users.contains(nick) =>
      update(users(nick).copy(claim = Admin))
      sender() ! ServerOk(id)

    case DropPrivileges(id, nick) if users.contains(nick) =>
      update(users(nick).copy(claim = Authorized))
      sender() ! ServerOk(id)

    case msg : TopKekMessage with AdminMessage =>
      sender() ! UserNotFound(msg.id)
  }

  private def messagingActions: Receive = {
    case SendPrivateMessage(id, nick, text) if users.contains(nick) && users(nick).actor.isDefined =>
      val senderNick = actors(sender()).nick
      users(nick).actor.get ! PrivateMessage(id, senderNick, text)
      sender() ! ServerOk(id)

    case SendMessage(id, channel, text) if channels.contains(channel) =>
      val nick = actors(sender()).nick
      channels(channel).map(users(_)).foreach(_.actor.get ! Message(id, channel, nick, text))
      sender() ! ServerOk(id)

    case SendPrivateMessage(id, nick, _) =>
      sender() ! UserNotFound(id)

    case SendMessage(id, channel, _) =>
      sender() ! ChannelNotFound(id)
  }

  private def enumerationActions: Receive = {
    case EnumerateUsers(id, channel) if channels.contains(channel) =>
      sender() ! UserList(id, channels(channel).toArray)

    case EnumerateUsers(id, _) =>
      sender() ! ChannelNotFound(id)

    case EnumerateChannels(id) =>
      sender() ! ChannelList(id, channels.keys.toArray)
  }

  private def channelsActions: Receive = {
    case Join(id, channel) if channels.contains(channel) =>
      channels += channel -> (channels(channel) + user.nick)
      sender() ! ServerOk(id)
    case Unjoin(id, channel) if channels.contains(channel) =>
      channels += channel -> (channels(channel) - user.nick)
      sender() ! ServerOk(id)

    case Join(id, _) =>
      sender() ! ChannelNotFound(id)
    case Unjoin(id, _) =>
      sender() ! ChannelNotFound(id)
  }

  private def authorizationActions: Receive = {

    case Authorization(id, nick, password)
      if users.contains(nick) && users(nick).passwordHash.contains(password) && users(nick).actor.isEmpty =>
      update(users(nick).copy(actor = Some(sender())))
      sender() ! ServerOk(id)

    case Authorization(id, _, _) =>
      sender() ! Unauthorized(id)

    case SetNickname(id,nick) if actors.contains(sender()) && nick == actors(sender()).nick  =>
      sender() ! ServerOk(id)

    case SetNickname(id, nick) if users.contains(nick) =>
      sender() ! NickNotUnique(id)

    case SetNickname(id, nick) =>
      update(User(nick, None, Authorized, Some(sender())))
      sender() ! ServerOk(id)

    case LockAccount(id, password) =>
      update(user.copy(passwordHash = Some(password)))
      sender() ! ServerOk(id)

    case UnlockAccount(id) =>
      update(user.copy(passwordHash = None))
      sender() ! ServerOk(id)

  }

  private def userDisconnectedActions: Receive = {
    case UserDisconnected if user.passwordHash.isEmpty =>
      users -= user.nick
      channels = channels.mapValues(_ - user.nick)
      actors -= sender()

    case UserDisconnected =>
      users += user.nick -> user.copy(actor = None)
      channels = channels.mapValues(_ - user.nick)
      actors -= sender()
  }

  override def receive: Receive =
    adminActions
      .orElse(messagingActions)
      .orElse(enumerationActions)
      .orElse(channelsActions)
      .orElse(authorizationActions)
      .orElse(userDisconnectedActions)
  
}
