package me.dogeshiba.chat.protocols.leet

import me.dogeshiba.chat.protocols.{VariableLengthBinaryProtocol, Protocol}
import me.dogeshiba.chat.protocols.leet.Errors.{InvalidMessage, LeetProtocolError}
import me.dogeshiba.chat.protocols.leet.Messages.LeetProtocolMessage
import scodec.Attempt.Successful
import scodec.{DecodeResult, Attempt, Codec}
import scodec.bits.BitVector
import scodec.codecs._
import scodec.codecs.implicits._

object LeetBinaryProtocol extends VariableLengthBinaryProtocol[LeetProtocolMessage, LeetProtocolError] {

  private val protocol = variableSizeBytes(uint16, uint16 ~ vectorOfN(uint16, variableSizeBytes(uint16, utf8)))

  override def decode(bytes: Array[Byte]): Either[LeetProtocolMessage, LeetProtocolError] =
    protocol.decode(BitVector.view(bytes)) match {
      case Successful(DecodeResult(code ~ strings,_)) => Left(LeetProtocolMessage(code, strings))
      case _ => Right(InvalidMessage)
    }

  override def encode(message: LeetProtocolMessage): Either[Array[Byte], LeetProtocolError] =
    protocol.encode(message.code -> message.arguments) match {
      case Successful(result) => Left(result.toByteArray)
      case _ => Right(InvalidMessage)
    }

  override def lengthInBytes(bytes: Array[Byte]): Option[Int] =
    uint16.decode(BitVector.view(bytes)).toOption.map(_.value + 2)

}
