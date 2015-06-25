package me.dogeshiba.chat.streams

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, Cancellable}
import akka.stream.scaladsl.Tcp.IncomingConnection
import akka.stream.scaladsl._
import akka.stream.{ActorFlowMaterializer, OverflowStrategy}
import akka.util.ByteString
import me.dogeshiba.chat.Server
import me.dogeshiba.chat.behaviour.ChatBehaviour
import me.dogeshiba.chat.protocols.VariableLengthBinaryProtocol
import me.dogeshiba.chat.streams.stages.VariableLengthBinaryProtocolStage
import me.dogeshiba.chat.system.ActorSystemOwner
import org.reactivestreams.Subscriber

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.FiniteDuration


class StreamServer[Message, Error](parallelism : Int,
                                   protocol : VariableLengthBinaryProtocol[Message,Error],
                                   factory: InetSocketAddress => ChatBehaviour[Message, Error, InetSocketAddress, Subscriber[Message]])
  extends Server with ActorSystemOwner {

  private[this] val users = scala.collection.concurrent.TrieMap[InetSocketAddress, (ActorRef, Behaviour)]()

  private[this] type Behaviour = ChatBehaviour[Message, Error, InetSocketAddress, Subscriber[Message]]

  private[this] type BehaviourResponse = (Message, Map[InetSocketAddress, Seq[Message]])

  private[this] def tryDisconnect(user: InetSocketAddress, actorRef: ActorRef, behaviour: Behaviour) = {
    if (actorRef.isTerminated) {
      behaviour.disconnect(user)
      users -= user
    }
  }

  private[this] var cancellable: Option[Cancellable] = None

  private[this] def processConnection(connection: IncomingConnection)(implicit materializer: ActorFlowMaterializer, ec: ExecutionContextExecutor) = {
    println(s"New connection from: ${connection.remoteAddress}")

    val fromOthers = Source.actorRef[Message](20, OverflowStrategy.dropTail)

    val behaviour = factory(connection.remoteAddress)

    val otherUsersSink = Sink.foreach[Map[InetSocketAddress, Seq[Message]]] { map =>
      for ((user, messages) <- map;
           message <- messages;
           (actor, behaviour) <- users.get(user).toSeq) {
        if (actor.isTerminated) {
          tryDisconnect(user, actor, behaviour)
        } else {
          actor ! message
        }
      }
    }

    val incoming = Flow[ByteString]
      .transform(() => new VariableLengthBinaryProtocolStage(protocol))
      .map(behaviour.receive)

    val serializingFlow = Flow[Message]
      .map(x => protocol.encode(x))
      .map {
      case Left(msg) => ByteString(msg)
      case Right(error) => ByteString(protocol.encodeError(error))
    }

    val flow = Flow(incoming, otherUsersSink, fromOthers, serializingFlow) { case (_, _, sub, _) => sub } {
      implicit builder =>
        (fromSocket, sink, fromSubscriber, serializer) =>
          import FlowGraph.Implicits._

          val merge = builder.add(Merge[Message](2))
          val broadcast = builder.add(Broadcast[BehaviourResponse](2))

          fromSocket ~> broadcast
          broadcast.map(_._2) ~> sink
          broadcast.map(_._1) ~> merge ~> serializer
          fromSubscriber ~> merge

          fromSocket.inlet -> serializer.outlet
    }

    val subscriber = connection.handleWith(flow)

    users += connection.remoteAddress -> (subscriber -> behaviour)

    println(s"Connected ${connection.remoteAddress}")
  }

  override def start(address : String, port : Int): Unit = {
    val binding = Tcp().bind(address, port)

    implicit val materializer = ActorFlowMaterializer()
    implicit val ec = materializer.executionContext

    cancellable = Some(system.scheduler.schedule(FiniteDuration(60, TimeUnit.SECONDS), FiniteDuration(60, TimeUnit.SECONDS)) {
      for ((user, (actor, behaviour)) <- users) {
        tryDisconnect(user, actor, behaviour)
      }
    })

    binding.runForeach(processConnection)

  }

  override def close() = {
    cancellable.foreach(_.cancel())
    super.close()
  }

}
