package me.dogeshiba.chat.protocols.leet

object Messages {
  case class LeetProtocolMessage(code : Int, arguments : Vector[String])
}
