package me.dogeshiba.chat.streams

import java.net.InetSocketAddress

import akka.actor.{ActorRef, Props}
import akka.stream.{OverflowStrategy, ActorFlowMaterializer, io}
import akka.stream.scaladsl.{Sink, Source, Flow, Tcp}
import akka.stream.scaladsl.Tcp.OutgoingConnection
import akka.util.ByteString
import me.dogeshiba.chat.Client
import me.dogeshiba.chat.protocols.leet.Messages.LeetProtocolMessage
import me.dogeshiba.chat.system.ActorSystemOwner
import me.dogeshiba.chat.protocols.VariableLengthBinaryProtocol
import me.dogeshiba.chat.streams.stages.VariableLengthBinaryProtocolStage
import org.reactivestreams.{Subscriber, Publisher}

class StreamClient[Message,Error](variableLengthBinaryProtocol: VariableLengthBinaryProtocol[Message,Error], textToMsg : String => Message, onRecieve : Message => Unit) extends Client with ActorSystemOwner {

  var actor : ActorRef = null

  override def start(address: String, port : Int): Unit = {

    implicit val materializer = ActorFlowMaterializer()

    val connection = Tcp().outgoingConnection(address,port)

    actor = Source
      .actorRef[String](1, OverflowStrategy.dropBuffer)
      //TODO parsing error handler
      .map(x => ByteString(variableLengthBinaryProtocol.encode(textToMsg(x)).left.get))
      .via(connection)
      .transform(() =>
        new VariableLengthBinaryProtocolStage(variableLengthBinaryProtocol))
      .to(Sink.foreach(onRecieve))
      .run()

  }

  override def send(msg: String): Unit = {
    actor ! msg
  }
}
