package me.dogeshiba.chat.behaviour.topkek

import java.net.InetSocketAddress

import me.dogeshiba.chat.behaviour.ChatBehaviour
import me.dogeshiba.chat.persistance.topkek.UserRepository
import me.dogeshiba.chat.persistance.topkek.Users._
import me.dogeshiba.chat.protocols.topkek.Messages._
import org.reactivestreams.Subscriber

class MessageBroadcaster(repository: UserRepository)(private[this] var address: InetSocketAddress)
  extends ChatBehaviour[TopKekMessage,ProtocolErrorMessage,InetSocketAddress, Subscriber[TopKekMessage]] {

  private[this] def currentUser = repository(address).get

  repository += Anonymous(address, None)

  private[this] type Receive = PartialFunction[TopKekMessage, TopKekMessage]

  private[this] def currentNick = currentUser match {
    case user: Nicknamed => Some(user.nick)
    case _ => None
  }

  private[this] def perform(operation: Operation)(id: Int)(user: Option[User]): TopKekMessage = user.flatMap(process(operation)) match {
    case Some(processed) =>
      repository += processed
      ServerOk(id)
    case None =>
      Unauthorized(id)
  }

  private[this] def adminActions: Receive = {

    case msg: TopKekMessage with AdminMessage if !currentUser.isInstanceOf[Admin] =>
      Unauthorized(msg.id)

    case CreateChannel(id, channel) if repository.containsChannel(channel) =>
      ChannelNotUnique(id)

    case CreateChannel(id, channel) =>
      repository += channel
      ServerOk(id)

    case DeleteChannel(id, channel) if repository.containsChannel(channel) =>
      repository.users(channel).foreach(_.send(Dropped(id, channel)))
      repository -= channel
      ServerOk(id)

    case DeleteChannel(id, channel) =>
      ChannelNotFound(id)

    case ElevatePrivileges(id, nick) if repository.contains(nick) =>
      perform(Elevate)(id)(repository(nick))

    case DropPrivileges(id, nick) if repository.contains(nick) =>
      perform(Drop)(id)(repository(nick))
      ServerOk(id)

    case msg : TopKekMessage with AdminMessage =>
      UserNotFound(msg.id)
  }

  private[this] def messagingActions: Receive = {
    case SendPrivateMessage(id, nick, text) if repository.contains(nick) =>
      repository(nick).foreach(_.send(PrivateMessage(id, currentNick.get, text)))
      ServerOk(id)

    case SendMessage(id, channel, text) if repository.containsChannel(channel) =>
      repository.users(channel).foreach(_.send(Message(id, channel, currentNick.get, text)))
      ServerOk(id)

    case SendPrivateMessage(id, nick, _) =>
      UserNotFound(id)

    case SendMessage(id, channel, _) =>
      ChannelNotFound(id)
  }

  private[this] def enumerationActions: Receive = {
    case EnumerateUsers(id, channel) if repository.containsChannel(channel) =>
      UserList(id, repository.users(channel).collect { case nicknamed: Nicknamed => nicknamed.nick }.toVector)

    case EnumerateUsers(id, _) =>
      ChannelNotFound(id)

    case EnumerateChannels(id) =>
      ChannelList(id, repository.channels().toVector)
  }

  private[this] def channelsActions: Receive = {
    case Join(id, channel) if repository.containsChannel(channel) =>
      repository += channel -> currentUser
      ServerOk(id)
    case Unjoin(id, channel) if repository.containsChannel(channel) =>
      repository -= channel -> currentUser
      ServerOk(id)

    case Join(id, _) =>
      ChannelNotFound(id)
    case Unjoin(id, _) =>
      ChannelNotFound(id)
  }

  private[this] def authorizationActions: Receive = {

    case Authorization(id, nick, password) =>
      process(Authorize(nick, password, repository(nick)))(currentUser) match {
        case Some(user) =>
          repository += user
          ServerOk(id)
        case _ =>
          InvalidPassword(id)
      }

    case SetNickname(id, nick) if currentNick.contains(nick) =>
      ServerOk(id)

    case SetNickname(id, nick) if repository(nick).isDefined =>
      NickNotUnique(id)

    case SetNickname(id, nick) =>
      perform(SetNick(nick))(id)(Some(currentUser))

    case LockAccount(id, password) =>
      perform(Lock(password))(id)(Some(currentUser))

    case UnlockAccount(id) =>
      perform(Unlock)(id)(Some(currentUser))
  }

  private[this] def blockUnathorized: Receive = {

    case msg: TopKekMessage if currentUser.subscriber.isEmpty =>
      Unauthorized(msg.id)

    case msg: TopKekMessage with RequireAuthorization if currentNick.isEmpty =>
      Unauthorized(msg.id)
  }

  private[this] def handshakeActions: Receive = {
    case ClientHello(id) =>
      ServerHello(id)
  }

  override def receive(message: Either[TopKekMessage, ProtocolErrorMessage]): TopKekMessage = {
    message match {
      case Left(msg) =>
        handshakeActions
        .orElse(blockUnathorized)
        .orElse(adminActions)
        .orElse(messagingActions)
        .orElse(enumerationActions)
        .orElse(channelsActions)
        .orElse(authorizationActions)(msg)
      case Right(error) =>
        error
    }

  }

  override def register(connection: InetSocketAddress, sender: Subscriber[TopKekMessage]): Unit =
    repository += Anonymous(connection, Some(sender))

}
