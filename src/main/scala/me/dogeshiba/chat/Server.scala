package me.dogeshiba.chat

import java.net.InetSocketAddress

trait Server extends AutoCloseable {
  def start(address : String, port : Int)
}
