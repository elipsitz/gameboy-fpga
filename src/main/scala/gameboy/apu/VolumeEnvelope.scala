package gameboy.apu

import chisel3._


class VolumeEnvelopeConfig extends Bundle {
  /** Initial volume level */
  val initialVolume = UInt(4.W)
  /** Whether sweep increases the volume (true) or decreases (false) */
  val modeIncrease = Bool()
  /** Period of the volume sweep */
  val sweepPace = UInt(3.W)
}

class VolumeEnvelope extends Module {
  val io = IO(new Bundle {
    /** Pulsed on when triggered. */
    val trigger = Input(Bool())
    /** Configuration registers */
    val config = Input(new VolumeEnvelopeConfig)
    /** Volume envelope tick pulse from frame sequencer (64 Hz) */
    val tick = Input(Bool())
    /** Output volume level */
    val out = Output(UInt(4.W))
  })

  val volume = RegInit(0.U(4.W))
  val timer = RegInit(0.U(3.W))
  io.out := volume
  // Config latched on trigger event.
  val modeIncrease = RegInit(false.B)
  val sweepPace = RegInit(0.U(3.W))

  when (sweepPace =/= 0.U && io.tick) {
    timer := timer - 1.U
    when (timer === 0.U) {
      timer := sweepPace

      when(modeIncrease && volume < 15.U) {
        volume := volume + 1.U
      }.elsewhen(!modeIncrease && volume > 0.U) {
        volume := volume - 1.U
      }
    }
  }

  when (io.trigger) {
    modeIncrease := io.config.modeIncrease
    sweepPace := io.config.sweepPace
    volume := io.config.initialVolume
    timer := io.config.sweepPace
  }
}
