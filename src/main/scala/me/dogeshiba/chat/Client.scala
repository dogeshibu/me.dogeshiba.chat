package me.dogeshiba.chat

import java.net.InetSocketAddress

trait Client extends AutoCloseable{
  def start(address : String, port : Int)
  def send(msg : String)
}
