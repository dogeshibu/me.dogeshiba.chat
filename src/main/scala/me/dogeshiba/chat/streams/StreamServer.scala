package me.dogeshiba.chat.streams

import java.net.InetSocketAddress

import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.{Flow, Tcp}
import akka.stream.stage._
import akka.util.ByteString
import me.dogeshiba.chat.Server
import me.dogeshiba.chat.system.ActorSystemOwner
import me.dogeshiba.chat.protocols.VariableLengthBinaryProtocol
import me.dogeshiba.chat.protocols.leet.LeetBinaryProtocol
import me.dogeshiba.chat.protocols.leet.Messages.LeetProtocolMessage
import me.dogeshiba.chat.streams.stages.VariableLengthBinaryProtocolStage

class StreamServer[Message, Error](protocol : VariableLengthBinaryProtocol[Message,Error], echoFunction : Message => Message) extends Server with ActorSystemOwner {

  override def start(address : String, port : Int): Unit = {
    val binding = Tcp().bind(address, port)

    implicit val materializer = ActorFlowMaterializer()

    binding runForeach { connection =>
      println(s"New connection from: ${connection.remoteAddress}")

      val echo = Flow[ByteString]
        .transform(() => new VariableLengthBinaryProtocolStage(protocol))
        .map(echoFunction)
        //TODO: parsing error handler
        .map(x => ByteString(protocol.encode(x).left.get))

      connection.handleWith(echo)
    }

  }

}