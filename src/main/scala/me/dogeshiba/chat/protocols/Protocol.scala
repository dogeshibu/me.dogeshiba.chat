package me.dogeshiba.chat.protocols


trait Protocol[Input, Message, Error] {
  def decode(bytes : Input) : Either[Message, Error]
  def encode(message : Message) : Either[Input, Error]

  def encodeError(error: Error) : Input

}
