package me.dogeshiba.chat.protocols.topkek

import me.dogeshiba.chat.protocols.Protocol
import me.dogeshiba.chat.protocols.leet.Messages.LeetProtocolMessage
import me.dogeshiba.chat.protocols.topkek.Messages._

object TopKekProtocol extends Protocol[LeetProtocolMessage,TopKekMessage, ProtocolErrorMessage] {

  private[this] val codes = ((100 to 109) ++ (401 to 405) ++ (200 to 213) ++ (300 to 302)).toSet

  override def decode(msg: LeetProtocolMessage): Either[TopKekMessage, ProtocolErrorMessage] = msg match {
    case LeetProtocolMessage(100,id,Vector()) => Left(ClientHello(id))
    case LeetProtocolMessage(101,id,Vector(nick)) => Left(SetNickname(id,nick))
    case LeetProtocolMessage(102,id,Vector(nick,password)) => Left(Authorization(id, nick, password))
    case LeetProtocolMessage(103,id,Vector(password)) => Left(LockAccount(id, password))
    case LeetProtocolMessage(104,id,Vector()) => Left(EnumerateChannels(id))
    case LeetProtocolMessage(105,id,Vector(channel)) => Left(Join(id, channel))
    case LeetProtocolMessage(106,id,Vector(channel)) => Left(EnumerateUsers(id, channel))
    case LeetProtocolMessage(107,id,Vector(channel, text)) => Left(SendMessage(id, channel, text))
    case LeetProtocolMessage(108,id,Vector(nick, text)) => Left(SendPrivateMessage(id, nick, text))
    case LeetProtocolMessage(109,id,Vector(channel)) => Left(Unjoin(id, channel))
    case LeetProtocolMessage(401,id,Vector()) => Left(UnlockAccount(id))
    case LeetProtocolMessage(402,id,Vector(channel)) => Left(CreateChannel(id, channel))
    case LeetProtocolMessage(403,id,Vector(channel)) => Left(DeleteChannel(id, channel))
    case LeetProtocolMessage(404,id,Vector(nick)) => Left(ElevatePrivileges(id, nick))
    case LeetProtocolMessage(405,id,Vector(nick)) => Left(DropPrivileges(id, nick))
    case LeetProtocolMessage(200,id,Vector()) => Left(ServerHello(id))
    case LeetProtocolMessage(201,id,Vector()) => Left(ServerOk(id))
    case LeetProtocolMessage(202,id,Vector()) => Left(Unauthorized(id))
    case LeetProtocolMessage(203,id,Vector()) => Left(InvalidPassword(id))
    case LeetProtocolMessage(204,id,Vector()) => Left(UserNotFound(id))
    case LeetProtocolMessage(205,id,Vector()) => Left(ChannelNotFound(id))
    case LeetProtocolMessage(206,id,Vector()) => Left(JoinRequired(id))
    case LeetProtocolMessage(207,id,channels) => Left(ChannelList(id, channels))
    case LeetProtocolMessage(208,id,users) => Left(UserList(id, users))
    case LeetProtocolMessage(209,id,Vector(channel)) => Left(Dropped(id, channel))
    case LeetProtocolMessage(210,id,Vector(channel, nick, text)) => Left(Message(id, channel, nick, text))
    case LeetProtocolMessage(211,id,Vector()) => Left(NickNotUnique(id))
    case LeetProtocolMessage(212,id,Vector()) => Left(ChannelNotUnique(id))
    case LeetProtocolMessage(213,id,Vector(nick, text)) => Left(PrivateMessage(id,nick,text))
    case LeetProtocolMessage(300,id,Vector()) => Left(BadRequest(id))
    case LeetProtocolMessage(301,id,Vector()) => Left(UnsupportedOperation(id))
    case LeetProtocolMessage(302,id,Vector()) => Left(ParameterRequired(id))
    case _ if codes.contains(msg.code) => Right(ParameterRequired(msg.id))
    case _ => Right(UnsupportedOperation(msg.id))
  }

  override def encode(message: TopKekMessage): Either[LeetProtocolMessage, ProtocolErrorMessage] = message match {
    case ClientHello(id) => Left(LeetProtocolMessage(100,id,Vector()))
    case SetNickname(id,nick) => Left(LeetProtocolMessage(101,id,Vector(nick)))
    case Authorization(id, nick, password) => Left(LeetProtocolMessage(102,id,Vector(nick,password)))
    case LockAccount(id, password) => Left(LeetProtocolMessage(103,id,Vector(password)))
    case EnumerateChannels(id) => Left(LeetProtocolMessage(104,id,Vector()))
    case Join(id, channel) => Left(LeetProtocolMessage(105,id,Vector(channel)))
    case EnumerateUsers(id, channel) => Left(LeetProtocolMessage(106,id,Vector(channel)))
    case SendMessage(id, channel, text) => Left(LeetProtocolMessage(107,id,Vector(channel, text)))
    case SendPrivateMessage(id, nick, text) => Left(LeetProtocolMessage(108,id,Vector(nick, text)))
    case Unjoin(id, channel) => Left(LeetProtocolMessage(109,id,Vector(channel)))
    case UnlockAccount(id) => Left(LeetProtocolMessage(401,id,Vector()))
    case CreateChannel(id, channel) => Left(LeetProtocolMessage(402,id,Vector(channel)))
    case DeleteChannel(id, channel) => Left(LeetProtocolMessage(403,id,Vector(channel)))
    case ElevatePrivileges(id, nick) => Left(LeetProtocolMessage(404,id,Vector(nick)))
    case DropPrivileges(id, nick) => Left(LeetProtocolMessage(405,id,Vector(nick)))
    case ServerHello(id) => Left(LeetProtocolMessage(200,id,Vector()))
    case ServerOk(id) => Left(LeetProtocolMessage(201,id,Vector()))
    case Unauthorized(id) => Left(LeetProtocolMessage(202,id,Vector()))
    case InvalidPassword(id) => Left(LeetProtocolMessage(203,id,Vector()))
    case UserNotFound(id) => Left(LeetProtocolMessage(204,id,Vector()))
    case ChannelNotFound(id) => Left(LeetProtocolMessage(205,id,Vector()))
    case JoinRequired(id) => Left(LeetProtocolMessage(206,id,Vector()))
    case ChannelList(id, channels) => Left(LeetProtocolMessage(207,id,channels))
    case UserList(id, users) => Left(LeetProtocolMessage(208,id,users))
    case Dropped(id, channel) => Left(LeetProtocolMessage(209,id,Vector(channel)))
    case Message(id, channel, nick, text) => Left(LeetProtocolMessage(210,id,Vector(channel, nick, text)))
    case NickNotUnique(id) => Left(LeetProtocolMessage(211,id,Vector()))
    case ChannelNotUnique(id) => Left(LeetProtocolMessage(212,id,Vector()))
    case PrivateMessage(id,nick,text) => Left(LeetProtocolMessage(213,id,Vector(nick, text)))
    case BadRequest(id) => Left(LeetProtocolMessage(300,id,Vector()))
    case UnsupportedOperation(id) => Left(LeetProtocolMessage(301,id,Vector()))
    case ParameterRequired(id) => Left(LeetProtocolMessage(302,id,Vector()))
  }
}
