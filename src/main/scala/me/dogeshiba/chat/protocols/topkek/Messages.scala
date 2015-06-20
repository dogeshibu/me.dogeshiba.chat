package me.dogeshiba.chat.protocols.topkek

object Messages {
  sealed trait TopKekMessage {
    val id : Int
  }

  sealed trait Request
  sealed trait Response
  sealed trait ProtocolErrorMessage extends Response
  sealed trait AdminMessage extends Request

  sealed case class ClientHello(id : Int) extends TopKekMessage with Request
  sealed case class SetNickname(id : Int, nick : String) extends TopKekMessage with Request
  sealed case class Authorization(id : Int, nick : String, password : String) extends TopKekMessage with Request
  sealed case class LockAccount(id : Int, password : String) extends TopKekMessage with Request
  sealed case class EnumerateChannels(id : Int) extends TopKekMessage with Request
  sealed case class Join(id : Int, channel : String) extends TopKekMessage with Request
  sealed case class EnumerateUsers(id : Int, channel : String) extends TopKekMessage with Request
  sealed case class SendMessage(id : Int, channel : String, text : String) extends TopKekMessage with Request
  sealed case class SendPrivateMessage(id : Int, nick : String, text : String) extends TopKekMessage with Request
  sealed case class Unjoin(id : Int, channel : String) extends TopKekMessage with Request
  sealed case class UnlockAccount(id : Int) extends TopKekMessage with Request

  sealed case class CreateChannel(id : Int, channel : String) extends TopKekMessage with AdminMessage
  sealed case class DeleteChannel(id : Int, channel : String) extends TopKekMessage with AdminMessage
  sealed case class ElevatePrivileges(id : Int, nick : String) extends TopKekMessage with AdminMessage
  sealed case class DropPrivileges(id : Int, nick : String) extends TopKekMessage with AdminMessage

  sealed case class ServerHello(id : Int) extends TopKekMessage with Response
  sealed case class ServerOk(id : Int) extends TopKekMessage with Response
  sealed case class Unauthorized(id : Int) extends TopKekMessage with Response
  sealed case class InvalidPassword(id : Int) extends TopKekMessage with Response
  sealed case class UserNotFound(id : Int) extends TopKekMessage with Response
  sealed case class ChannelNotFound(id : Int) extends TopKekMessage with Response
  sealed case class JoinRequired(id : Int) extends TopKekMessage with Response
  sealed case class ChannelList(id : Int, channels : Vector[String]) extends TopKekMessage with Response
  sealed case class UserList(id : Int, users : Vector[String]) extends TopKekMessage with Response
  sealed case class Dropped(id : Int, channel : String) extends TopKekMessage with Response
  sealed case class Message(id : Int, channel : String, nick : String, text : String) extends TopKekMessage with Response
  sealed case class NickNotUnique(id : Int) extends TopKekMessage with Response
  sealed case class ChannelNotUnique(id : Int) extends TopKekMessage with Response
  sealed case class PrivateMessage(id : Int, nick : String, text : String) extends TopKekMessage with Response

  sealed case class BadRequest(id : Int) extends TopKekMessage with ProtocolErrorMessage
  sealed case class UnsupportedOperation(id : Int) extends TopKekMessage with ProtocolErrorMessage
  sealed case class ParameterRequired(id : Int) extends TopKekMessage with ProtocolErrorMessage
}
