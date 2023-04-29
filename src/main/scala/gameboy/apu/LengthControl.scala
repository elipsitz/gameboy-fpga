package gameboy.apu

import chisel3._


class LengthControlConfig(bits: Int) extends Bundle {
  /** Raw length input (inverted + 1 when loaded) */
  val length = UInt(bits.W)
  /** Whether length is being loaded this cycle */
  val lengthLoad = Bool()
  /** Whether the length control is enabled */
  val enabled = Bool()
}

class LengthControl(bits: Int) extends Module {
  val io = IO(new Bundle {
    /** Pulsed on when triggered. */
    val trigger = Input(Bool())
    /** Configuration registers */
    val config = Input(new LengthControlConfig(bits))
    /** Length control tick pulse from frame sequencer (256 Hz) */
    val tick = Input(Bool())
    /** Pulsed when the channel output should get disabled */
    val channelDisable = Output(Bool())
  })

  val timer = RegInit(0.U(bits.W))
  io.channelDisable := false.B

  when (io.trigger) {
    // On trigger, if the timer is 0, it plays for the maximum length.
    // This is probably because channelEnable is only turned off on a tick where the timer goes down to 0.
    // So if it's at 0, it'll take 64 ticks before it gets set to 0.
  } .elsewhen (io.tick && io.config.enabled) {
    val newTimer = timer - 1.U
    timer := newTimer
    when (newTimer === 0.U) {
      // Channel only gets disabled if we *tick down* to 0.
      io.channelDisable := true.B
    }
  }

  when (io.config.lengthLoad) {
    // Timer = 64 - load value
    timer := (~io.config.length) + 1.U
  }
}
