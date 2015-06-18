package me.dogeshiba.chat

import java.net.InetSocketAddress

import akka.actor.{Props, ActorSystem}
import me.dogeshiba.chat.protocols.leet.Errors.LeetProtocolError
import me.dogeshiba.chat.protocols.leet.LeetBinaryProtocol
import me.dogeshiba.chat.protocols.leet.Messages.LeetProtocolMessage
import me.dogeshiba.chat.streams.{StreamServer, StreamClient}
import me.dogeshiba.chat.common._

import scala.io.StdIn

object Chat extends App {

  def userInputToMessage(string: String) = LeetProtocolMessage(100, Vector("wat",string))

  def serverResponseToOutput(leetProtocolMessage: LeetProtocolMessage) = {
    println(s"Server echo: $leetProtocolMessage")
    print("> ")
  }

  args match {
    case Array("--client") =>
      using(new StreamClient(LeetBinaryProtocol,userInputToMessage,serverResponseToOutput)) { client =>
        client.start("localhost", 4568)
        print("> ")
        var text = StdIn.readLine()
        while (text != ":exit") {
          client.send(text)
          text = StdIn.readLine()
        }
      }
    case Array("--server") =>
      using(new StreamServer[LeetProtocolMessage, LeetProtocolError](LeetBinaryProtocol, m =>{ println(m); m.copy(code = 200) })) { server =>
        server.start("localhost",4568)
        StdIn.readLine()
      }

  }
}
