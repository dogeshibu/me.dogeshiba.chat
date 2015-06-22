package me.dogeshiba.chat.behaviour

trait ChatBehaviour[Message, Error, Connection, Sender] {

  def receive(message: Either[Message, Error]): Message

  def register(connection: Connection, sender: Sender) : Unit

}
