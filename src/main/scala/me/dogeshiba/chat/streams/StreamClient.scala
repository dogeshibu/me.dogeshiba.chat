package me.dogeshiba.chat.streams

import akka.actor.ActorRef
import akka.stream.scaladsl.{Sink, Source, Tcp}
import akka.stream.{ActorFlowMaterializer, OverflowStrategy}
import akka.util.ByteString
import me.dogeshiba.chat.Client
import me.dogeshiba.chat.protocols.VariableLengthBinaryProtocol
import me.dogeshiba.chat.streams.stages.VariableLengthBinaryProtocolStage
import me.dogeshiba.chat.system.ActorSystemOwner

class StreamClient[Message,Error](variableLengthBinaryProtocol: VariableLengthBinaryProtocol[Message,Error],
                                  onRecieve : Either[Message,Error] => Unit) extends Client[Message] with ActorSystemOwner {

  var subscriber : Option[ActorRef] = None

  override def start(address: String, port : Int): Unit = {

    implicit val materializer = ActorFlowMaterializer()

    val connection = Tcp().outgoingConnection(address,port)

    subscriber = Some(Source
      .actorRef[Message](20, OverflowStrategy.dropTail)
      .map(x => variableLengthBinaryProtocol.encode(x))
      .map {
        case Left(msg) => ByteString(msg)
        case Right(error) => ByteString(variableLengthBinaryProtocol.encodeError(error))
      }
      .via(connection)
      .transform(() =>
        new VariableLengthBinaryProtocolStage(variableLengthBinaryProtocol))
      .to(Sink.foreach(onRecieve))
      .run())

  }

  override def send(msg: Message): Unit = {
    subscriber.foreach { actor =>
      actor ! msg
    }
  }
}
