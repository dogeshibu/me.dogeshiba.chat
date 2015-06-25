package me.dogeshiba.chat.behaviour

trait ChatBehaviour[Message, Error, Connection, Sender] {

  def receive(message: Either[Message, Error]): (Message, Map[Connection, Seq[Message]])

  def disconnect(connection: Connection): Unit

}
