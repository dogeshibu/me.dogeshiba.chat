package me.dogeshiba.chat.streams.stages

import akka.stream.stage.{Context, PushPullStage, SyncDirective}
import akka.util.ByteString
import me.dogeshiba.chat.protocols.VariableLengthBinaryProtocol

class VariableLengthBinaryProtocolStage[Message,Error](protocol : VariableLengthBinaryProtocol[Message,Error]) extends PushPullStage[ByteString, Either[Message,Error]] {
  private var buffer = ByteString.empty
  private var length = 0
  private var newMessage = true

  def reset(bytesLength : Int) = {
    newMessage = true
    length = 0
    buffer = buffer.takeRight(bytesLength)
  }


  override def onPush(elem: ByteString, ctx: Context[Either[Message,Error]]): SyncDirective = {
    buffer ++= elem
    if(newMessage) {
      length = protocol.lengthInBytes(buffer.toArray).get
      newMessage = false
    }
    if(buffer.length >= length) {
      val bufferToDecode = buffer.take(length).toArray
      reset(buffer.length - length)
      ctx.push(protocol.decode(bufferToDecode))
    } else {
      ctx.pull()
    }
  }

  override def onPull(ctx: Context[Either[Message,Error]]): SyncDirective =
    ctx.pull()

}
