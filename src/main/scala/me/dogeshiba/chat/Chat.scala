package me.dogeshiba.chat

import me.dogeshiba.chat.common._
import me.dogeshiba.chat.protocols.leet.Errors.LeetProtocolError
import me.dogeshiba.chat.protocols.leet.LeetBinaryProtocol
import me.dogeshiba.chat.protocols.leet.Messages.LeetProtocolMessage
import me.dogeshiba.chat.streams.{StreamClient, StreamServer}

import scala.io.StdIn
import scala.util.Try

object Chat extends App {

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

  def userInputToMessage(string: String) = LeetProtocolMessage(100,1, Vector("wat",string))

  def serverResponseToOutput(leetProtocolMessage: LeetProtocolMessage) = {
    println(s"Server echo: $leetProtocolMessage")
    print("> ")
  }

  object IsClient {
    def unapply(args : Array[String]) : Option[Option[(String,Int)]] = args match {
      case Array("--client") => Some(None)
      case Array("--client",address,port) => Try(port.toInt).toOption.map(port => Some(address -> port))
    }
  }
  
}
