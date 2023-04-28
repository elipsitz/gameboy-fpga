package gameboy.apu

import chisel3._

class FrameSequencerTicks extends Bundle {
  val volume = Bool()
  val length = Bool()
  val frequency = Bool()
}

/**
 * The frame sequencer generates low frequency pulses to advance frequency sweep,
 * volume sweep, and the length timer.
 */
class FrameSequencer extends Module {
  val io = IO(new Bundle {
    /** DIV-APU bit from the timer */
    val divApu = Input(Bool())
    /** Output event pulses */
    val ticks = Output(new FrameSequencerTicks())
  })

  val divPulse = RegNext(io.divApu, false.B) && !io.divApu
  val divCounter = RegInit(0.U(3.W))
  when (divPulse) { divCounter := divCounter + 1.U }
  val divPrevCounter = RegNext(divCounter, 0.U)

  // XXX: Maybe there's a way to just use the carry out of the counter bits?
  io.ticks.length := divCounter(0) && !divPrevCounter(0)
  io.ticks.frequency := divCounter(1) && !divPrevCounter(1)
  io.ticks.volume := divCounter(2) && !divPrevCounter(2)
}