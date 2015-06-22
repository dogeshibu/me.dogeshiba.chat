package me.dogeshiba.chat

import me.dogeshiba.chat.behaviour.topkek.MessageBroadcaster
import me.dogeshiba.chat.common._
import me.dogeshiba.chat.persistance.topkek.InMemoryUserRepository
import me.dogeshiba.chat.protocols.leet.LeetBinaryProtocol
import me.dogeshiba.chat.protocols.topkek.Messages._
import me.dogeshiba.chat.protocols.topkek.TopKekProtocol
import me.dogeshiba.chat.streams.{StreamClient, StreamServer}

import scala.io.StdIn
import scala.util.Try

object Chat extends App {

  val repository = new InMemoryUserRepository()

  repository += "all"

  val protocol = LeetBinaryProtocol.compose(msg => BadRequest(-1).asInstanceOf[ProtocolErrorMessage])(TopKekProtocol)

  args match {
    case Array("--client") =>
      using(new StreamClient(protocol,serverResponseToOutput)) { client =>
        client.start("localhost", 4568)
        var id = 1
        client.send(ClientHello(id))
        id += 1
        client.send(SetNickname(id, "admin"))
        var text = StdIn.readLine()
        while (text != ":exit") {
          id += 1
          if(text == ":join") {
            client.send(Join(id,"all"))
          } else {
            client.send(SendMessage(id,"all",text))
          }
          text = StdIn.readLine()
        }
      }
    case Array("--server") =>
      using(new StreamServer(1, protocol, new MessageBroadcaster(repository)(_))) { server =>
        server.start("localhost",4568)
        StdIn.readLine()
      }

  }


  def serverResponseToOutput(leetProtocolMessage: Either[TopKekMessage,ProtocolErrorMessage]) = {
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
