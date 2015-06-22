package me.dogeshiba.chat

trait Client[Message] extends AutoCloseable{
  def start(address : String, port : Int)
  def send(msg : Message)
}
