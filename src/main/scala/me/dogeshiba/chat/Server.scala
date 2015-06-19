package me.dogeshiba.chat

trait Server extends AutoCloseable {
  def start(address : String, port : Int)
}
