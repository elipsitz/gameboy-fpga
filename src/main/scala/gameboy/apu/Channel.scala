package gameboy.apu

import chisel3._

class ChannelIO extends Bundle {
  val out = Output(UInt(4.W))
}

/** Silent no-op channel for testing */
class SilentChannel extends Module {
  val io = IO(new ChannelIO())
  io.out := 0.U
}