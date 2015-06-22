package me.dogeshiba.chat.streams

import java.net.InetSocketAddress

import akka.stream.scaladsl._
import akka.stream.{ActorFlowMaterializer, OverflowStrategy}
import akka.util.ByteString
import me.dogeshiba.chat.Server
import me.dogeshiba.chat.behaviour.ChatBehaviour
import me.dogeshiba.chat.protocols.VariableLengthBinaryProtocol
import me.dogeshiba.chat.streams.stages.VariableLengthBinaryProtocolStage
import me.dogeshiba.chat.system.ActorSystemOwner
import org.reactivestreams.{Subscriber, Subscription}


class StreamServer[Message, Error](parallelism : Int,
                                   protocol : VariableLengthBinaryProtocol[Message,Error],
                                   behaviour: ChatBehaviour[Message, Error, InetSocketAddress, Subscriber[Message]])
  extends Server with ActorSystemOwner {

  override def start(address : String, port : Int): Unit = {
    val binding = Tcp().bind(address, port)

    implicit val materializer = ActorFlowMaterializer()

    binding runForeach { connection =>
      println(s"New connection from: ${connection.remoteAddress}")

      val fromOthers = Source.actorRef[Message](20, OverflowStrategy.dropTail)

      val function = behaviour.receive(connection.remoteAddress)(_)

      val incoming = Flow[ByteString]
        .transform(() => new VariableLengthBinaryProtocolStage(protocol))
        .map(function)


      val serializingFlow = Flow[Message]
        .map(x => protocol.encode(x))
        .map {
          case Left(msg) => ByteString(msg)
          case Right(error) => ByteString(protocol.encodeError(error))
        }

      val flow = Flow(incoming, fromOthers, serializingFlow) { case (_,sub,_) => sub } { implicit builder =>
        (fromSocket, fromSubscriber, serializer) =>
        import FlowGraph.Implicits._

        val merge = builder.add(Merge[Message](2))

        fromSocket     ~> merge ~> serializer
        fromSubscriber ~> merge

        fromSocket.inlet -> serializer.outlet
      }

      val subscriber = connection.handleWith(flow)

      behaviour.register(connection.remoteAddress, new Subscriber[Message] {

        override def onError(t: Throwable): Unit = {}

        override def onSubscribe(s: Subscription): Unit = {}

        override def onComplete(): Unit = {}

        override def onNext(t: Message): Unit = subscriber ! t

      })

      println(s"Connected ${connection.remoteAddress}")
    }

  }

}
