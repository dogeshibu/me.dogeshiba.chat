package me.dogeshiba.chat

trait Client extends AutoCloseable{
  def start(address : String, port : Int)
  def send(msg : String)
}
