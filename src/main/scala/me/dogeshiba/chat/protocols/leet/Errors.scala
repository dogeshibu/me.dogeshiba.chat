package me.dogeshiba.chat.protocols.leet

object Errors {
  sealed trait LeetProtocolError

  object InvalidMessage extends LeetProtocolError
}
