package me.dogeshiba.chat.behaviour.topkek

import java.net.InetSocketAddress

import me.dogeshiba.chat.behaviour.ChatBehaviour
import me.dogeshiba.chat.persistance.topkek.Persistance.{Admin, Anonymous, Authorized, User}
import me.dogeshiba.chat.protocols.topkek.Messages._
import org.reactivestreams.Subscriber

class MessageBroadcaster(initialChannels : Vector[String])
  extends ChatBehaviour[TopKekMessage,ProtocolErrorMessage,InetSocketAddress, Subscriber[TopKekMessage]] {

  private[this] var connections = scala.collection.concurrent.TrieMap[InetSocketAddress,User]()
  private[this] var channels = scala.collection.concurrent.TrieMap[String,Set[InetSocketAddress]]()

  for(channel <- initialChannels)
    channels += channel -> Set.empty[InetSocketAddress]

  type Receive = PartialFunction[TopKekMessage,TopKekMessage]

  private[this] def users(nick : String) =
    connections.values.find(_.nick == nick).get

  private[this] def update(user: User) = {
    connections += user.address -> user
  }

  private[this] def adminActions(implicit currentUser : () => User, sender : () => InetSocketAddress): Receive = {

    case msg : TopKekMessage with AdminMessage if currentUser().claim != Admin =>
      Unauthorized(msg.id)

    case CreateChannel(id, channel) if channels.contains(channel) =>
      ChannelNotUnique(id)

    case CreateChannel(id, channel) =>
      channels += channel -> Set.empty
      ServerOk(id)

    case DeleteChannel(id, channel) if channels.contains(channel) =>
      channels(channel).map(connections).foreach(_.subscriber.foreach(_.onNext(Dropped(id, channel))))
      channels -= channel
      ServerOk(id)

    case DeleteChannel(id, channel) =>
      ChannelNotFound(id)

    case ElevatePrivileges(id, nick) if connections.values.exists(_.nick == nick) =>
      update(users(nick).copy(claim = Admin))
      ServerOk(id)

    case DropPrivileges(id, nick) if connections.values.exists(_.nick == nick) =>
      update(users(nick).copy(claim = Authorized))
      ServerOk(id)

    case msg : TopKekMessage with AdminMessage =>
      UserNotFound(msg.id)
  }

  private[this] def messagingActions(implicit currentUser : () => User, sender : () => InetSocketAddress): Receive = {
    case SendPrivateMessage(id, nick, text) if connections.values.exists(_.nick == nick) && users(nick).subscriber.isDefined =>
      val senderNick = connections(sender())
      users(nick).subscriber.foreach(_.onNext(PrivateMessage(id, senderNick.nick.get, text)))
      ServerOk(id)

    case SendMessage(id, channel, text) if channels.contains(channel) =>
      val nick = currentUser().nick.getOrElse("anonymous")
      channels(channel).map(connections).foreach(_.subscriber.foreach(_.onNext(Message(id, channel, nick, text))))
      ServerOk(id)

    case SendPrivateMessage(id, nick, _) =>
      UserNotFound(id)

    case SendMessage(id, channel, _) =>
      ChannelNotFound(id)
  }

  private[this] def enumerationActions(implicit currentUser : () => User, sender : () => InetSocketAddress): Receive = {
    case EnumerateUsers(id, channel) if channels.contains(channel) =>
      UserList(id, channels(channel).map(connections).filter(_.nick.isDefined).map(_.nick.get).toVector)

    case EnumerateUsers(id, _) =>
      ChannelNotFound(id)

    case EnumerateChannels(id) =>
      ChannelList(id, channels.keys.toVector)
  }

  private[this] def channelsActions(implicit currentUser : () => User, sender : () => InetSocketAddress): Receive = {
    case Join(id, channel) if channels.contains(channel) =>
      channels += channel -> (channels(channel) + currentUser().address)
      ServerOk(id)
    case Unjoin(id, channel) if channels.contains(channel) =>
      channels += channel -> (channels(channel) - currentUser().address)
      ServerOk(id)

    case Join(id, _) =>
      ChannelNotFound(id)
    case Unjoin(id, _) =>
      ChannelNotFound(id)
  }

  private[this] def authorizationActions(implicit currentUser:() => User, sender : () => InetSocketAddress): Receive = {

    case Authorization(id, nick, password)
      if connections.values.exists(_.nick == nick) && users(nick).passwordHash.contains(password) && users(nick).subscriber.isEmpty =>
      update(currentUser().copy(nick = Some(nick), passwordHash = Some(password), claim = Authorized))
      ServerOk(id)

    case Authorization(id, nick, password)
      if connections.values.exists(_.nick == nick) && !users(nick).passwordHash.contains(password) =>
      InvalidPassword(id)

    case Authorization(id, _, _) =>
      Unauthorized(id)

    case SetNickname(id,nick) if currentUser().nick.contains(nick)  =>
      ServerOk(id)

    case SetNickname(id, nick) if connections.values.exists(_.nick == nick) =>
      NickNotUnique(id)

    case SetNickname(id, nick) =>
      update(currentUser().copy(nick = Some(nick), claim = Authorized))
      ServerOk(id)

    case LockAccount(id, password) =>
      update(currentUser().copy(passwordHash = Some(password)))
      ServerOk(id)

    case UnlockAccount(id) =>
      update(currentUser().copy(passwordHash = None))
      ServerOk(id)
  }

  private[this] def blockUnathorized(implicit currentUser: () => User, sender : () => InetSocketAddress) : Receive = {
    case msg : TopKekMessage with RequireAuthorization if currentUser().claim == Anonymous =>
      Unauthorized(msg.id)
  }

  private[this] def handshakeActions(implicit currentUser: () => User, sender : () => InetSocketAddress) : Receive = {
    case ClientHello(id) =>
      ServerHello(id)
  }

  override def receive(connection: InetSocketAddress)(message : Either[TopKekMessage, ProtocolErrorMessage]) : TopKekMessage = {
    if(!connections.contains(connection))
      connections += connection -> User(None,None,Anonymous,None,connection)
    implicit val currentUser = () => connections(connection)
    implicit val sender = () => connection
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

  override def register(connection: InetSocketAddress, sender: Subscriber[TopKekMessage]): Unit = {
    connections += connection -> connections.getOrElse(connection,User(None,None,Anonymous,None,connection)).copy(subscriber = Some(sender))
  }
}
