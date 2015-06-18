package me.dogeshiba.chat.protocols

trait VariableLengthBinaryProtocol[Message,Error] extends Protocol[Array[Byte],Message,Error]{
  def lengthInBytes(bytes : Array[Byte]) : Option[Int]
}
