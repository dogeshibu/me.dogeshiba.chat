package me.dogeshiba.chat.actors.topkek

object Messages {
  sealed trait AuxiliaryMessages

  object UserDisconnected extends AuxiliaryMessages
}
