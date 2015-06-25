package me.dogeshiba.chat.behaviour.topkek

import java.net.InetSocketAddress

import me.dogeshiba.chat.behaviour.ChatBehaviour
import me.dogeshiba.chat.persistance.topkek.UserRepository
import me.dogeshiba.chat.persistance.topkek.Users._
import me.dogeshiba.chat.protocols.topkek.Messages._
import org.reactivestreams.Subscriber

class TopKekChatBehaviour(repository: UserRepository)(private[this] var address: InetSocketAddress)
  extends ChatBehaviour[TopKekMessage,ProtocolErrorMessage,InetSocketAddress, Subscriber[TopKekMessage]] {

  private[this] def currentUser = repository(address).get

  repository += Anonymous(address)

  private[this] type Response = (TopKekMessage, Map[InetSocketAddress, Seq[TopKekMessage]])

  private[this] type Receive = PartialFunction[TopKekMessage, Response]

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

  private[this] def process(operation: Operation)(user: User): Option[User] = user -> operation match {
    case (Anonymous(addr), SetNick(nick)) =>
      Some(Authorized(nick, addr))
    case (Anonymous(addr), Authorize(nick, password, Some(Locked(n, pwd, _)))) if n == nick && password == pwd =>
      Some(Locked(nick, password, addr))
    case (Anonymous(addr), Authorize(nick, password, Some(Admin(n, pwd, _)))) if n == nick && password == pwd =>
      Some(Admin(nick, password, addr))
    case (Authorized(nick, addr), Lock(password)) =>
      Some(Locked(nick, password, addr))
    case (Locked(nick, password, addr), Elevate) =>
      Some(Admin(nick, password, addr))
    case (Locked(nick, password, addr), Unlock) =>
      Some(Authorized(nick, addr))
    case (Admin(nick, password, addr), Drop) =>
      Some(Locked(nick, password, addr))
    case _ => None
  }

  private[this] implicit def messageToResponse(message: TopKekMessage): Response =
    message -> Map.empty

  private[this] def adminActions: Receive = {

    case msg: TopKekMessage with AdminMessage if !currentUser.isInstanceOf[Admin] =>
      Unauthorized(msg.id)

    case CreateChannel(id, channel) if repository.containsChannel(channel) =>
      ChannelNotUnique(id)

    case CreateChannel(id, channel) =>
      repository += channel
      ServerOk(id)

    case DeleteChannel(id, channel) if repository.containsChannel(channel) =>
      repository -= channel
      ServerOk(id) -> repository
        .users(channel)
        .map(_.address -> Seq(Dropped(id, channel)))
        .toMap

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
      ServerOk(id) -> repository(nick)
        .toSeq
        .map(_.address -> Seq(PrivateMessage(id, currentNick.get, text)))
        .toMap

    case SendMessage(id, channel, text) if repository.containsChannel(channel) =>
      ServerOk(id) -> repository
        .users(channel)
        .map(_.address -> Seq(Message(id, channel, currentNick.get, text)))
        .toMap

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
    case msg: TopKekMessage with RequireAuthorization if currentNick.isEmpty =>
      Unauthorized(msg.id)
  }

  private[this] def handshakeActions: Receive = {
    case ClientHello(id) =>
      ServerHello(id)
  }

  override def receive(message: Either[TopKekMessage, ProtocolErrorMessage]): Response = {
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


  override def disconnect(connection: InetSocketAddress): Unit =
    repository(connection).foreach(repository -= _)

  private[this] sealed trait Operation

  private[this] sealed case class SetNick(nick: String) extends Operation

  private[this] sealed case class Authorize(nick: String, password: String, against: Option[User]) extends Operation

  private[this] sealed case class Lock(password: String) extends Operation

  private[this] object Unlock extends Operation

  private[this] object Elevate extends Operation

  private[this] object Drop extends Operation

}
