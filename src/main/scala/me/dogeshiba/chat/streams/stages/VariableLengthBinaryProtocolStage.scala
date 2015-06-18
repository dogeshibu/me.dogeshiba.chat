package me.dogeshiba.chat.streams.stages

import akka.stream.stage.{SyncDirective, Directive, Context, PushPullStage}
import akka.util.ByteString
import me.dogeshiba.chat.protocols.VariableLengthBinaryProtocol
import me.dogeshiba.chat.protocols.leet.LeetBinaryProtocol
import me.dogeshiba.chat.protocols.leet.Messages.LeetProtocolMessage

class VariableLengthBinaryProtocolStage[Message,Error](protocol : VariableLengthBinaryProtocol[Message,Error]) extends PushPullStage[ByteString, Message] {
  private var buffer = ByteString.empty
  private var length = 0
  private var newMessage = true

  def reset(bytesLength : Int) = {
    newMessage = true
    length = 0
    buffer = buffer.takeRight(bytesLength)
  }


  override def onPush(elem: ByteString, ctx: Context[Message]): SyncDirective = {
    buffer ++= elem
    if(newMessage) {
      length = protocol.lengthInBytes(buffer.toArray).get
      newMessage = false
    }
    if(buffer.length >= length) {
      val bufferToDecode = buffer.take(length).toArray
      reset(buffer.length - length)
      //TODO: parsing error handler
      ctx.push(protocol.decode(bufferToDecode).left.get)
    } else {
      ctx.pull()
    }
  }

  override def onPull(ctx: Context[Message]): SyncDirective =
    ctx.pull()
}
