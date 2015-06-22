package me.dogeshiba.chat.behaviour

trait ChatBehaviour[Message, Error, Connection, Sender] {

  def receive(connection: Connection)(message: Either[Message,Error]) : Message

  def register(connection: Connection, sender: Sender) : Unit

}
