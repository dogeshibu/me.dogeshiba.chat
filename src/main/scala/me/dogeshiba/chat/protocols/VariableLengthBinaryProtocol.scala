package me.dogeshiba.chat.protocols

trait VariableLengthBinaryProtocol[Message,Error] extends Protocol[Array[Byte],Message,Error]{ self =>
  def lengthInBytes(bytes : Array[Byte]) : Option[Int]

  def compose[OtherMessage, OtherError <: OtherMessage](f : Error => OtherError)(protocol: Protocol[Message, OtherMessage, OtherError]) =
    new VariableLengthBinaryProtocol[OtherMessage, OtherError] {
      override def lengthInBytes(bytes: Array[Byte]): Option[Int] =
        self.lengthInBytes(bytes)

      override def encode(message: OtherMessage): Either[Array[Byte], OtherError] =
        protocol.encode(message).left.flatMap(self.encode(_).right.map(f))

      override def decode(bytes: Array[Byte]): Either[OtherMessage, OtherError] =
        self.decode(bytes).right.map(f).left.flatMap(protocol.decode)
    }
}
