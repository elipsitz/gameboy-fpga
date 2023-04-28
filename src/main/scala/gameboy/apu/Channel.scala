package gameboy.apu

import chisel3._
import chisel3.util.Counter

class ChannelIO extends Bundle {
  /** Pulsed when channel is triggered by a write */
  val trigger = Input(Bool())
  /** Frame sequencer ticks */
  val ticks = Input(new FrameSequencerTicks)

  /** Output value of the channel */
  val out = Output(UInt(4.W))
  /** Whether the channel is active: (not disabled due to length (or frequency sweep)) **/
  val active = Output(Bool())
  /** Whether the DAC is enabled **/
  val dacEnabled = Output(Bool())
}

/** Silent no-op channel for testing */
class SilentChannel extends Module {
  val io = IO(new ChannelIO())
  io.out := 0.U
  io.active := false.B
  io.dacEnabled := false.B
}