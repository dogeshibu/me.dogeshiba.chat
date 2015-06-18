package me.dogeshiba.chat

package object common {
  def using[T <: AutoCloseable](closable : T)(action : T => Unit) = {
    action(closable)
    closable.close()
  }
}
